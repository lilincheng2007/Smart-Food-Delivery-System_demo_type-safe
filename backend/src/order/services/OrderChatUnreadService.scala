package delivery.order.services

import cats.effect.IO
import delivery.merchant.services.MerchantBusinessService
import delivery.order.objects.apiTypes.OrderChatUnreadCountsResponse
import delivery.order.tables.order.OrderTable
import delivery.order.tables.orderchat.OrderChatMessageTable
import delivery.rider.tables.rideraccount.RiderAccountTable
import delivery.platform.api.HttpApiError
import delivery.domain.OrderId
import delivery.user.tables.customerprofile.CustomerProfileTable

import java.sql.Connection

object OrderChatUnreadService:
  def countsForRole(connection: Connection, username: String, role: String): IO[OrderChatUnreadCountsResponse] =
    for
      orderIds <- accessibleOrderIds(connection, username, role)
      counts <- OrderChatMessageTable.unreadCountsForOrders(connection, role, orderIds)
    yield OrderChatUnreadCountsResponse(counts)

  private def accessibleOrderIds(connection: Connection, username: String, role: String): IO[List[OrderId]] =
    role match
      case "customer" =>
        for
          account <- CustomerProfileTable.findByUsername(connection, username).flatMap {
            case Some(value) => IO.pure(value)
            case None        => IO.raiseError(HttpApiError.BadRequest("未找到顾客账号"))
          }
          orders <- OrderTable.listByCustomerId(connection, account.profile.id)
        yield orders.map(_.id)
      case "merchant" =>
        for
          stores <- MerchantBusinessService.listOwnedStores(connection, username)
          orders <- OrderTable.listByMerchantIds(connection, stores.map(_.id))
        yield orders.map(_.id)
      case "rider" =>
        for
          account <- RiderAccountTable.findByUsername(connection, username).flatMap {
            case Some(value) => IO.pure(value)
            case None        => IO.raiseError(HttpApiError.BadRequest("未找到骑手账号"))
          }
          assigned <- OrderTable.listByRiderId(connection, account.profile.rider.id)
          available <- OrderTable.listAvailableUnassigned(connection)
        yield (assigned ++ available).map(_.id)
      case _ =>
        IO.raiseError(HttpApiError.BadRequest("当前身份不支持订单聊天"))

end OrderChatUnreadService
