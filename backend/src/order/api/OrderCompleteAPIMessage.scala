package delivery.order.api

import cats.effect.IO
import delivery.order.objects.Order
import delivery.order.tables.order.OrderTable
import delivery.rider.tables.riderassignment.RiderAssignmentTable
import delivery.shared.api.{APIWithRoleMessage, HttpApiError}
import delivery.shared.objects.{OrderId, OrderStatus}
import delivery.shared.utils.VoucherSupport
import delivery.user.tables.customerprofile.CustomerProfileTable

import java.sql.Connection

final case class OrderCompleteAPIMessage(orderId: OrderId) extends APIWithRoleMessage[Order]:
  override def plan(connection: Connection, username: String): IO[Order] =
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
        else if order.status == OrderStatus.已完成 then IO.raiseError(HttpApiError.BadRequest("订单已完成"))
        else if order.status != OrderStatus.已送达 then IO.raiseError(HttpApiError.BadRequest(s"当前状态不可确认完成：${order.status}"))
        else IO.unit
      earnedPoints = math.floor(if order.payableAmount > 0 then order.payableAmount else order.totalAmount).toInt
      currentPoints = account.profile.foodiePoints
      currentLevel = math.max(account.profile.foodieLevel, OrderAPIMessageSupport.levelOf(currentPoints))
      nextPoints = currentPoints + earnedPoints
      nextLevel = OrderAPIMessageSupport.levelOf(nextPoints)
      rewardCount = math.max(0, nextLevel - currentLevel)
      nextVouchers = VoucherSupport.addStandardPlatformVouchers(account.profile.id, account.profile.vouchers, rewardCount)
      completedOrder = order.copy(status = OrderStatus.已完成, pointsAwarded = earnedPoints)
      nextAccount = account.copy(profile = account.profile.copy(
        pendingOrders = account.profile.pendingOrders.filterNot(_.id == orderId),
        historyOrders = completedOrder :: account.profile.historyOrders.filterNot(_.id == orderId),
        foodiePoints = nextPoints,
        foodieLevel = nextLevel,
        vouchers = nextVouchers
      ))
      _ <- OrderTable.upsert(connection, completedOrder)
      _ <- order.riderId match
        case Some(riderId) => RiderAssignmentTable.upsert(connection, riderId, completedOrder.id, completedOrder.status)
        case None          => IO.unit
      _ <- CustomerProfileTable.upsert(connection, nextAccount)
    yield completedOrder
