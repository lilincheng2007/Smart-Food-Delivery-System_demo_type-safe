package delivery.order.services

import cats.effect.IO
import cats.syntax.all.*
import delivery.admin.tables.storeonboarding.StoreOnboardingRequestTable
import delivery.domain.{OrderStatus, RefundStatus, UserRole}
import delivery.merchant.services.MerchantBusinessService
import delivery.merchant.tables.merchantstore.MerchantStoreTable
import delivery.order.objects.OrderChatRole
import delivery.order.objects.apiTypes.{NotificationFeedItem, NotificationFeedResponse}
import delivery.order.tables.notificationreadstate.NotificationReadStateTable
import delivery.order.tables.order.OrderTable
import delivery.platform.api.{HttpApiError}
import delivery.rider.tables.rideraccount.RiderAccountTable
import delivery.user.tables.customerprofile.CustomerProfileTable

import java.sql.Connection
import java.time.Instant
import scala.util.Try

object NotificationFeedService:
  private final case class DraftEvent(id: String, message: String, target: String, createdAtMillis: Long)

  def feed(connection: Connection, username: String, role: UserRole, cursor: Option[String], limit: Option[Int]): IO[NotificationFeedResponse] =
    for
      events <- eventsForRole(connection, username, role)
      readIds <- NotificationReadStateTable.listReadIds(connection, username)
      readSet = readIds.toSet
      deduped = events
        .groupBy(_.id)
        .values
        .map(_.maxBy(_.createdAtMillis))
        .toList
        .sortBy(_.createdAtMillis)(Ordering.Long.reverse)
      unreadCount = deduped.count(event => !readSet.contains(event.id))
      offset = cursor.flatMap(parseCursor).getOrElse(0)
      safeLimit = normalizeLimit(limit)
      paged = deduped.slice(offset, offset + safeLimit)
      nextCursor = Option.when(offset + safeLimit < deduped.size)((offset + safeLimit).toString)
      items = paged.map(event =>
        NotificationFeedItem(
          id = event.id,
          message = event.message,
          target = event.target,
          createdAt = Instant.ofEpochMilli(event.createdAtMillis).toString,
          isRead = readSet.contains(event.id)
        )
      )
    yield NotificationFeedResponse(items = items, nextCursor = nextCursor, unreadCount = unreadCount)

  private def eventsForRole(connection: Connection, username: String, role: UserRole): IO[List[DraftEvent]] =
    role match
      case UserRole.customer => customerEvents(connection, username)
      case UserRole.merchant => merchantEvents(connection, username)
      case UserRole.rider    => riderEvents(connection, username)
      case UserRole.admin    => adminEvents(connection)

  private def customerEvents(connection: Connection, username: String): IO[List[DraftEvent]] =
    for
      account <- CustomerProfileTable.findByUsername(connection, username).flatMap {
        case Some(value) => IO.pure(value)
        case None        => IO.raiseError(HttpApiError.NotFound("未找到顾客"))
      }
      orders <- OrderTable.listByCustomerId(connection, account.profile.id)
      merchants <- MerchantStoreTable.listCatalog(connection)
      merchantNameById = merchants.map(item => item.id -> item.storeName).toMap
      chat <- chatEvents(connection, username, OrderChatRole.customer, orders)
      orderEvents = orders.flatMap { order =>
        val storeName = merchantNameById.getOrElse(order.merchantId, order.merchantId)
        val created = DraftEvent(
          id = s"customer-order-created:${order.id}",
          message = s"你已向店铺「${storeName}」下单，订单 ${order.id} 已创建",
          target = "/delivery/customer?tab=profile",
          createdAtMillis = parseAt(order.placedAt, fallback = nowMillis())
        )
        val delivered =
          Option.when(order.status == OrderStatus.已送达)(
            DraftEvent(
              id = s"customer-order-delivered:${order.id}",
              message = s"店铺「${storeName}」的订单 ${order.id} 已送达",
              target = "/delivery/customer?tab=profile",
              createdAtMillis = parseAt(orderTimelineAt(order, "delivered").orElse(order.estimatedReadyAt), fallback = parseAt(order.placedAt, nowMillis()))
            )
          )
        val refund = order.refundStatus.flatMap {
          case RefundStatus.已通过 =>
            Some(DraftEvent(
              id = s"customer-refund:${order.id}:accepted:${order.refundedAt.getOrElse("")}",
              message = s"店铺「${storeName}」的订单 ${order.id} 退款申请已通过",
              target = "/delivery/customer?tab=profile",
              createdAtMillis = parseAt(order.refundedAt.orElse(order.refundMerchantReviewedAt).orElse(order.refundRequestedAt), fallback = parseAt(order.placedAt, nowMillis()))
            ))
          case RefundStatus.已驳回 =>
            Some(DraftEvent(
              id = s"customer-refund:${order.id}:rejected:${order.refundAdminReason.getOrElse("")}",
              message = s"店铺「${storeName}」的订单 ${order.id} 退款申请已被平台驳回",
              target = "/delivery/customer?tab=profile",
              createdAtMillis = parseAt(order.refundedAt.orElse(order.refundMerchantReviewedAt).orElse(order.refundRequestedAt), fallback = parseAt(order.placedAt, nowMillis()))
            ))
          case RefundStatus.商家已驳回 =>
            Some(DraftEvent(
              id = s"customer-refund:${order.id}:merchantRejected:${order.refundMerchantReason.getOrElse("")}",
              message = s"店铺「${storeName}」驳回了订单 ${order.id} 的退款申请",
              target = "/delivery/customer?tab=profile",
              createdAtMillis = parseAt(order.refundMerchantReviewedAt.orElse(order.refundRequestedAt), fallback = parseAt(order.placedAt, nowMillis()))
            ))
          case RefundStatus.待管理员仲裁 =>
            Some(DraftEvent(
              id = s"customer-refund:${order.id}:adminPending:${order.refundRequestedAt.getOrElse("")}",
              message = s"你已将店铺「${storeName}」的订单 ${order.id} 退款申请提交平台仲裁",
              target = "/delivery/customer?tab=profile",
              createdAtMillis = parseAt(order.refundRequestedAt, fallback = parseAt(order.placedAt, nowMillis()))
            ))
          case _ => None
        }
        List(created) ++ delivered.toList ++ refund.toList
      }
    yield orderEvents ++ chat

  private def merchantEvents(connection: Connection, username: String): IO[List[DraftEvent]] =
    for
      stores <- MerchantBusinessService.listOwnedStores(connection, username)
      orders <- OrderTable.listByMerchantIds(connection, stores.map(_.id))
      onboardingRequests <- StoreOnboardingRequestTable.listByOwner(connection, username)
      chat <- chatEvents(connection, username, OrderChatRole.merchant, orders)
      merchantNameById = stores.map(item => item.id -> item.storeName).toMap
      onboardingEvents = onboardingRequests
        .filter(_.status != "pending")
        .map { request =>
          val message =
            if request.status == "accepted" then s"店铺「${request.storeName}」入驻申请已通过"
            else s"店铺「${request.storeName}」入驻申请已驳回"
          DraftEvent(
            id = s"merchant-onboarding:${request.id}:${request.status}:${request.reviewedAt.getOrElse("")}",
            message = message,
            target = "/delivery/merchant?tab=profile",
            createdAtMillis = parseAt(request.reviewedAt.getOrElse(request.createdAt), fallback = nowMillis())
          )
        }
      refundEvents = orders.flatMap { order =>
        val storeName = merchantNameById.getOrElse(order.merchantId, order.merchantId)
        order.refundStatus.flatMap {
          case RefundStatus.待商家审核 =>
            Some(DraftEvent(
              id = s"merchant-refund-request:${order.id}:${order.refundRequestedAt.getOrElse("")}",
              message = s"顾客「${order.customerName}」向店铺「${storeName}」提出订单 ${order.id} 的退款申请：${order.refundReason.getOrElse("未填写原因")}",
              target = "/delivery/merchant?tab=reviews",
              createdAtMillis = parseAt(order.refundRequestedAt, fallback = parseAt(order.placedAt, nowMillis()))
            ))
          case RefundStatus.已通过 if order.refundAdminReason.exists(_.trim.nonEmpty) =>
            Some(DraftEvent(
              id = s"merchant-refund-forced:${order.id}:${order.refundedAt.getOrElse(order.refundAdminReason.getOrElse(""))}",
              message = s"平台已强制通过顾客「${order.customerName}」对店铺「${storeName}」订单 ${order.id} 的退款申请",
              target = "/delivery/merchant?tab=reviews",
              createdAtMillis = parseAt(order.refundedAt.orElse(order.refundMerchantReviewedAt).orElse(order.refundRequestedAt), fallback = parseAt(order.placedAt, nowMillis()))
            ))
          case _ => None
        }
      }
    yield onboardingEvents ++ refundEvents ++ chat

  private def riderEvents(connection: Connection, username: String): IO[List[DraftEvent]] =
    for
      account <- RiderAccountTable.findByUsername(connection, username).flatMap {
        case Some(value) => IO.pure(value)
        case None        => IO.raiseError(HttpApiError.NotFound("未找到骑手"))
      }
      assigned <- OrderTable.listByRiderId(connection, account.profile.rider.id)
      available <- OrderTable.listAvailableUnassigned(connection)
      chat <- chatEvents(connection, username, OrderChatRole.rider, assigned ++ available)
    yield chat

  private def adminEvents(connection: Connection): IO[List[DraftEvent]] =
    for
      onboarding <- StoreOnboardingRequestTable.list(connection)
      refunds <- OrderTable.listRefundRequests(connection)
      onboardingEvents = onboarding
        .filter(_.status == "pending")
        .map(request =>
          DraftEvent(
            id = s"admin-onboarding:${request.id}:${request.createdAt}",
            message = s"商家账号「${request.ownerUsername}」提交了店铺「${request.storeName}」入驻申请",
            target = "/delivery/admin",
            createdAtMillis = parseAt(request.createdAt, fallback = nowMillis())
          )
        )
      refundEvents = refunds
        .filter(order => order.refundStatus.contains(RefundStatus.待管理员仲裁))
        .map(order =>
          DraftEvent(
            id = s"admin-refund:${order.id}:${order.refundRequestedAt.getOrElse("")}:${order.refundMerchantReviewedAt.getOrElse("")}",
            message = s"顾客「${order.customerName}」提交了订单 ${order.id} 的退款仲裁申请",
            target = "/delivery/admin",
            createdAtMillis = parseAt(order.refundRequestedAt.orElse(Some(order.placedAt)), fallback = nowMillis())
          )
        )
    yield onboardingEvents ++ refundEvents

  private def chatEvents(connection: Connection, username: String, role: OrderChatRole, orders: List[delivery.order.objects.Order]): IO[List[DraftEvent]] =
    OrderChatUnreadService.countsForRole(connection, username, role).map { response =>
      val orderById = orders.map(order => order.id -> order).toMap
      response.counts
        .filter(_.unreadCount > 0)
        .map { count =>
          val roleName = chatRoleLabel(count.peerRole)
          val order = orderById.get(count.orderId)
          val peerName =
            count.peerRole match
              case OrderChatRole.customer => order.map(_.customerName).filter(_.nonEmpty).getOrElse("顾客")
              case OrderChatRole.merchant => order.map(_.merchantId).getOrElse("商家")
              case OrderChatRole.rider    => order.flatMap(_.riderId).getOrElse("骑手")
          DraftEvent(
            id = s"chat:${count.orderId}:${count.peerRole}:${count.unreadCount}:${count.latestMessageType.map(_.toString).getOrElse("")}:${count.latestContent.getOrElse("")}",
            message = s"${roleName}「${peerName}」在订单 ${count.orderId} 发送了新消息（${count.unreadCount} 条未读）",
            target = s"/delivery/chat/${java.net.URLEncoder.encode(count.orderId, "UTF-8")}?peer=${java.net.URLEncoder.encode(count.peerRole.toString, "UTF-8")}",
            createdAtMillis = parseAt(count.latestCreatedAt, fallback = nowMillis())
          )
        }
    }

  private def parseCursor(value: String): Option[Int] =
    Try(value.trim.toInt).toOption.filter(_ >= 0)

  private def normalizeLimit(value: Option[Int]): Int =
    value.map(v => math.max(1, math.min(100, v))).getOrElse(40)

  private def parseAt(value: Option[String], fallback: => Long): Long =
    value.flatMap(raw => Try(Instant.parse(raw)).toOption).map(_.toEpochMilli).getOrElse(fallback)

  private def parseAt(value: String, fallback: => Long): Long =
    parseAt(Some(value), fallback)

  private def nowMillis(): Long = Instant.now().toEpochMilli

  private def orderTimelineAt(order: delivery.order.objects.Order, keyword: String): Option[String] =
    order.statusTimeline
      .find(event => event.key.toLowerCase.contains(keyword.toLowerCase) || event.label.contains("送达"))
      .map(_.occurredAt)

  private def chatRoleLabel(role: OrderChatRole): String =
    role match
      case OrderChatRole.customer => "顾客"
      case OrderChatRole.merchant => "商家"
      case OrderChatRole.rider    => "骑手"

end NotificationFeedService
