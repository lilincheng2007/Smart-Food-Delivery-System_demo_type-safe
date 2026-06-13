package delivery.order.api

import delivery.order.services.{OrderCheckoutService, OrderStatusTransitionService}
import cats.effect.IO
import delivery.order.objects.Order
import delivery.order.tables.order.OrderTable
import delivery.rider.tables.riderassignment.RiderAssignmentTable
import delivery.platform.api.{APIWithRoleMessage, HttpApiError}
import delivery.domain.{OrderId, OrderStatus}
import delivery.promotion.services.VoucherSupport
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
        else IO.unit
      earnedPoints = math.floor(if order.payableAmount > 0 then order.payableAmount else order.totalAmount).toInt
      currentPoints = account.profile.foodiePoints
      currentLevel = math.max(account.profile.foodieLevel, OrderCheckoutService.levelOf(currentPoints))
      nextPoints = currentPoints + earnedPoints
      nextLevel = OrderCheckoutService.levelOf(nextPoints)
      rewardCount = math.max(0, nextLevel - currentLevel)
      nextVouchers = VoucherSupport.addStandardPlatformVouchers(account.profile.id, account.profile.vouchers, rewardCount)
      completedOrder <- OrderStatusTransitionService.transition(
        connection,
        order,
        OrderStatus.已完成,
        actorRole = "customer",
        patch = _.copy(pointsAwarded = earnedPoints)
      )
      nextAccount = account.copy(profile = account.profile.copy(
        pendingOrders = account.profile.pendingOrders.filterNot(_.id == orderId),
        historyOrders = completedOrder :: account.profile.historyOrders.filterNot(_.id == orderId),
        foodiePoints = nextPoints,
        foodieLevel = nextLevel,
        vouchers = nextVouchers
      ))
      _ <- order.riderId match
        case Some(riderId) => RiderAssignmentTable.upsert(connection, riderId, completedOrder.id, completedOrder.status)
        case None          => IO.unit
      _ <- CustomerProfileTable.upsert(connection, nextAccount)
    yield completedOrder
