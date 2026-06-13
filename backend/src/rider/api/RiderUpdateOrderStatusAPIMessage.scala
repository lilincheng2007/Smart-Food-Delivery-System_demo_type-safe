package delivery.rider.api

import cats.effect.IO
import delivery.order.api.OrderStatusTransitionService
import delivery.order.tables.order.OrderTable
import delivery.rider.objects.RiderDeliverySettlement
import delivery.rider.tables.rideraccount.RiderAccountTable
import delivery.rider.tables.riderassignment.RiderAssignmentTable
import delivery.rider.utils.RiderTimeoutPolicy
import delivery.platform.api.{APIWithRoleMessage, HttpApiError}
import delivery.domain.{OrderId, OrderStatus, RiderStatus}

import java.sql.Connection
import java.time.Instant
import java.util.UUID

final case class RiderUpdateOrderStatusAPIMessage(orderId: OrderId, targetStatus: OrderStatus) extends APIWithRoleMessage[RiderDeliverySettlement]:
  override def plan(connection: Connection, username: String): IO[RiderDeliverySettlement] =
    for
      account <- RiderAccountTable.findByUsername(connection, username).flatMap {
        case Some(value) => IO.pure(value)
        case None        => IO.raiseError(HttpApiError.BadRequest("未找到骑手账号"))
      }
      order <- OrderTable.findById(connection, orderId).flatMap {
        case Some(value) => IO.pure(value)
        case None        => IO.raiseError(HttpApiError.BadRequest("未找到订单"))
      }
      _ <-
        if order.riderId != Some(account.profile.rider.id) then IO.raiseError(HttpApiError.BadRequest("无权操作该订单"))
        else IO.unit
      assignment <- RiderAssignmentTable.find(connection, account.profile.rider.id, orderId).flatMap {
        case Some(value) => IO.pure(value)
        case None        => IO.raiseError(HttpApiError.BadRequest("未找到骑手派单记录"))
      }
      completedAt = Instant.now()
      deadlineAt = RiderTimeoutPolicy.deadlineAt(assignment.assignedAt)
      overtimeSeconds = RiderTimeoutPolicy.overtimeSeconds(assignment.assignedAt, completedAt)
      wasTimeout = overtimeSeconds > 0
      earnedEnergy = if wasTimeout then 0 else RiderTimeoutPolicy.EnergyPerDeliveredOrder
      autoUseCard = wasTimeout && account.profile.rider.timeoutCardCount > 0
      updatedOrder <- OrderStatusTransitionService.transition(connection, order, targetStatus, actorRole = "rider")
      remainingPending <- OrderTable.countActiveByRider(connection, account.profile.rider.id, excludingOrderId = Some(orderId))
      nextStatus = if remainingPending > 0 then RiderStatus.配送中 else RiderStatus.空闲
      currentRider = account.profile.rider
      updatedRider = currentRider.copy(
        status = nextStatus,
        energyPoints = currentRider.energyPoints + earnedEnergy,
        timeoutCardCount = if autoUseCard then currentRider.timeoutCardCount - 1 else currentRider.timeoutCardCount,
        timeoutCount = if wasTimeout then currentRider.timeoutCount + 1 else currentRider.timeoutCount,
        timeoutExemptedCount = if autoUseCard then currentRider.timeoutExemptedCount + 1 else currentRider.timeoutExemptedCount
      )
      _ <- RiderAccountTable.upsert(connection, account.copy(profile = account.profile.copy(rider = updatedRider)))
      _ <- RiderAssignmentTable.completeDelivery(
        connection,
        updatedRider.id,
        updatedOrder.id,
        updatedOrder.status,
        completedAt,
        deadlineAt,
        wasTimeout,
        autoUseCard,
        autoUseCard,
        overtimeSeconds
      )
    yield RiderDeliverySettlement(
      ok = true,
      orderId = orderId,
      earnedEnergy = earnedEnergy,
      currentEnergyPoints = updatedRider.energyPoints,
      currentTimeoutCardCount = updatedRider.timeoutCardCount,
      wasTimeout = wasTimeout,
      timeoutCardUsed = autoUseCard,
      timeoutExempted = autoUseCard,
      overtimeSeconds = overtimeSeconds
    )
