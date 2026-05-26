package delivery.order.api

import cats.effect.IO
import cats.syntax.all.*
import delivery.merchant.objects.Product
import delivery.merchant.tables.catalogproduct.CatalogProductTable
import delivery.order.objects.{CheckoutLine, CheckoutRequest, CheckoutResponse, CustomerOrdersResponse, Order, OrderCancelResponse}
import delivery.order.tables.checkoutrequest.CheckoutRequestTable
import delivery.order.tables.order.OrderTable
import delivery.order.utils.OrderApiSupport
import delivery.shared.api.{APIWithRoleMessage, HttpApiError}
import delivery.user.objects.CustomerProfile
import delivery.user.tables.customerprofile.CustomerProfileTable

import java.sql.Connection

private def isHistoryOrderStatus(status: String): Boolean =
  status == "已送达" || status == "已完成" || status == "已取消"

private def buildOrdersForCheckout(products: List[Product], customerProfile: CustomerProfile, lines: List[CheckoutLine]): IO[Either[String, (List[Order], Double)]] =
  if lines.isEmpty then IO.pure(Left("购物车为空"))
  else
    for
      nowMillis <- IO.realTime.map(_.toMillis)
      zoneId <- IO.delay(java.time.ZoneId.systemDefault())
    yield
      val grouped = lines.groupBy(_.merchantId)
      val now = java.time.Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDateTime
      val orderTimeText = f"${now.getYear}%04d-${now.getMonthValue}%02d-${now.getDayOfMonth}%02d ${now.getHour}%02d:${now.getMinute}%02d"

      val createdOrders = grouped.toList.zipWithIndex.flatMap { case ((merchantId, groupLines), idx) =>
        val items = groupLines.flatMap { line =>
          products.find(p => p.id == line.productId && p.merchantId == merchantId).map(p => delivery.order.objects.OrderItem(p.id, p.name, p.price, line.quantity))
        }
        if items.isEmpty then None
        else
          Some(
            Order(
              s"o-$nowMillis-${idx + 1}",
              customerProfile.id,
              customerProfile.name,
              customerProfile.phone,
              merchantId,
              None,
              items,
              items.map(i => i.unitPrice * i.quantity).sum,
              customerProfile.defaultAddress,
              "制作中",
              orderTimeText
            )
          )
      }

      if createdOrders.isEmpty then Left("无法解析购物车商品")
      else
        val total = createdOrders.map(_.totalAmount).sum
        if customerProfile.walletBalance < total then Left("余额不足")
        else Right((createdOrders.reverse, total))

final case class CustomerOrdersAPIMessage() extends APIWithRoleMessage[CustomerOrdersResponse]:
  override def plan(connection: Connection, username: String): IO[CustomerOrdersResponse] =
    for
      account <- CustomerProfileTable.findByUsername(connection, username)
      output <- account match
        case None => IO.raiseError(HttpApiError.NotFound(OrderApiSupport.customerNotFound.error))
        case Some(value) =>
          OrderTable.list(connection).map { orders =>
            val customerOrders = orders.filter(_.customerId == value.profile.id)
            CustomerOrdersResponse(
              pendingOrders = customerOrders.filterNot(order => isHistoryOrderStatus(order.status)),
              historyOrders = customerOrders.filter(order => isHistoryOrderStatus(order.status))
            )
          }
    yield output

final case class OrderDetailAPIMessage(orderId: String) extends APIWithRoleMessage[Order]:
  override def plan(connection: Connection, username: String): IO[Order] =
    for
      account <- CustomerProfileTable.findByUsername(connection, username)
      order <- OrderTable.findById(connection, orderId)
      output <- (account, order) match
        case (None, _) => IO.raiseError(HttpApiError.NotFound(OrderApiSupport.customerNotFound.error))
        case (Some(value), Some(found)) if found.customerId == value.profile.id => IO.pure(found)
        case (Some(value), None) =>
          value.profile.pendingOrders
            .find(_.id == orderId)
            .orElse(value.profile.historyOrders.find(_.id == orderId)) match
            case Some(found) => IO.pure(found)
            case None        => IO.raiseError(HttpApiError.NotFound("未找到订单"))
        case (Some(_), Some(_)) => IO.raiseError(HttpApiError.NotFound("未找到订单"))
    yield output

final case class OrderCancelAPIMessage(orderId: String) extends APIWithRoleMessage[OrderCancelResponse]:
  override def plan(connection: Connection, username: String): IO[OrderCancelResponse] =
    for
      account <- CustomerProfileTable.findByUsername(connection, username).flatMap {
        case Some(value) => IO.pure(value)
        case None        => IO.raiseError(HttpApiError.BadRequest("未找到顾客账号"))
      }
      order <- OrderTable.findById(connection, orderId).flatMap {
        case Some(value) => IO.pure(value)
        case None        => IO.raiseError(HttpApiError.BadRequest("未找到订单"))
      }
      _ <-
        if order.customerId != account.profile.id then IO.raiseError(HttpApiError.BadRequest("无权操作该订单"))
        else if order.status == "已取消" then IO.raiseError(HttpApiError.BadRequest("订单已取消"))
        else if order.status == "已送达" || order.status == "已完成" then IO.raiseError(HttpApiError.BadRequest("已完成订单不可取消"))
        else if order.riderId.nonEmpty || order.status == "配送中" then IO.raiseError(HttpApiError.BadRequest("配送中订单不可取消"))
        else IO.unit
      canceledOrder = order.copy(status = "已取消")
      nextAccount = account.copy(profile = account.profile.copy(walletBalance = account.profile.walletBalance + order.totalAmount))
      _ <- OrderTable.upsert(connection, canceledOrder)
      _ <- CustomerProfileTable.upsert(connection, nextAccount)
    yield OrderCancelResponse(canceledOrder, nextAccount.profile.walletBalance)

final case class CheckoutAPIMessage(
    lines: List[CheckoutLine],
    customerName: Option[String],
    customerPhone: Option[String],
    deliveryAddress: Option[String]
) extends APIWithRoleMessage[CheckoutResponse]:
  override def plan(connection: Connection, username: String): IO[CheckoutResponse] =
    val body = CheckoutRequest(lines, customerName, customerPhone, deliveryAddress)
    for
      account <- CustomerProfileTable.findByUsername(connection, username).flatMap {
        case Some(value) => IO.pure(value)
        case None        => IO.raiseError(HttpApiError.NotFound(OrderApiSupport.customerNotFound.error))
      }
      products <- CatalogProductTable.list(connection)
      profileForOrders =
        (body.customerName, body.customerPhone, body.deliveryAddress) match
          case (Some(n), Some(ph), Some(ad))
              if n.trim.nonEmpty && ph.trim.nonEmpty && ad.trim.nonEmpty =>
            account.profile.copy(name = n.trim, phone = ph.trim, defaultAddress = ad.trim)
          case _ => account.profile
      built <- buildOrdersForCheckout(products, profileForOrders, body.lines.map(OrderApiSupport.normalizeLine))
      result <- built match
        case Left(msg) => IO.raiseError(HttpApiError.BadRequest(msg))
        case Right((orders, totalDebit)) =>
          val nextAccount = account.copy(profile =
            account.profile.copy(
              walletBalance = account.profile.walletBalance - totalDebit,
              pendingOrders = orders.reverse ::: account.profile.pendingOrders
            )
          )
          for
            _ <- orders.traverse_(OrderTable.upsert(connection, _))
            _ <- CheckoutRequestTable.insert(connection, username, body, orders.map(_.id))
            _ <- CustomerProfileTable.upsert(connection, nextAccount)
          yield CheckoutResponse(orders = orders, walletBalance = nextAccount.profile.walletBalance)
    yield result
