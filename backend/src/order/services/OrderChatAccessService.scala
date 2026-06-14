package delivery.order.services

import cats.effect.IO
import cats.syntax.all.*
import delivery.merchant.services.MerchantBusinessService
import delivery.order.objects.{Order, OrderChatMessageType, OrderChatRole}
import delivery.order.tables.order.OrderTable
import delivery.rider.tables.rideraccount.RiderAccountTable
import delivery.platform.api.HttpApiError
import delivery.domain.{OrderId, OrderStatus}
import delivery.user.tables.customerprofile.CustomerProfileTable

import java.sql.Connection

object OrderChatAccessService:
  private val allowedPeerRoles: Map[OrderChatRole, Set[OrderChatRole]] = Map(
    OrderChatRole.customer -> Set(OrderChatRole.merchant, OrderChatRole.rider),
    OrderChatRole.merchant -> Set(OrderChatRole.customer, OrderChatRole.rider),
    OrderChatRole.rider -> Set(OrderChatRole.customer, OrderChatRole.merchant)
  )

  def requireOrderForRole(connection: Connection, username: String, role: OrderChatRole, orderId: OrderId, peerRole: OrderChatRole): IO[Order] =
    for
      _ <- validatePeerRole(role, peerRole)
      order <- OrderTable.findById(connection, orderId).flatMap {
        case Some(value) => IO.pure(value)
        case None        => IO.raiseError(HttpApiError.BadRequest("未找到订单"))
      }
      _ <- role match
        case OrderChatRole.customer => requireCustomerOrder(connection, username, order)
        case OrderChatRole.merchant => MerchantBusinessService.requireOwnedStore(connection, username, order.merchantId).void
        case OrderChatRole.rider    => requireRiderOrder(connection, username, order)
    yield order

  def validateMessage(messageType: OrderChatMessageType, content: String): IO[Unit] =
    val trimmed = content.trim
    if trimmed.isEmpty then IO.raiseError(HttpApiError.BadRequest("消息内容不能为空"))
    else IO.unit

  private def validatePeerRole(role: OrderChatRole, peerRole: OrderChatRole): IO[Unit] =
    if allowedPeerRoles.get(role).exists(_.contains(peerRole)) then IO.unit
    else IO.raiseError(HttpApiError.BadRequest("不支持的聊天对象"))

  private def requireCustomerOrder(connection: Connection, username: String, order: Order): IO[Unit] =
    CustomerProfileTable.findByUsername(connection, username).flatMap {
      case Some(account) if account.profile.id == order.customerId => IO.unit
      case Some(_)                                                 => IO.raiseError(HttpApiError.BadRequest("无权查看该订单聊天"))
      case None                                                    => IO.raiseError(HttpApiError.BadRequest("未找到顾客账号"))
    }

  private def requireRiderOrder(connection: Connection, username: String, order: Order): IO[Unit] =
    RiderAccountTable.findByUsername(connection, username).flatMap {
      case Some(account) if order.riderId.contains(account.profile.rider.id) || order.status == OrderStatus.待骑手接单 => IO.unit
      case Some(_)                                                                                                      => IO.raiseError(HttpApiError.BadRequest("无权查看该订单聊天"))
      case None                                                                                                         => IO.raiseError(HttpApiError.BadRequest("未找到骑手账号"))
    }

end OrderChatAccessService
