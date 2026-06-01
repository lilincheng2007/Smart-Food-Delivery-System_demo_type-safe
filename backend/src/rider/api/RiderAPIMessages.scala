package delivery.rider.api

import cats.effect.IO
import delivery.rider.objects.{RiderAvailableOrdersResponse, RiderDeliverySettlement, RiderDeliveryStatus, RiderMeResponse, RiderTimeoutCardRedeemResponse, RiderUseTimeoutCardResponse}
import delivery.rider.tables.rideraccount.RiderAccountTable
import delivery.rider.tables.riderassignment.RiderAssignmentTable
import delivery.rider.utils.{RiderApiSupport, RiderTimeoutPolicy}
import delivery.order.tables.order.OrderTable
import delivery.shared.api.{APIWithRoleMessage, HttpApiError}
import delivery.shared.objects.{OkResponse, OrderId, OrderStatus, RiderStatus}

import java.sql.Connection
import java.time.Instant

private def isHistoryOrderStatus(status: OrderStatus): Boolean =
  OrderStatus.history.contains(status)

private def isAvailableOrder(orderStatus: OrderStatus): Boolean =
  orderStatus == OrderStatus.待接单

private def statusView(record: delivery.rider.tables.RiderAssignmentRecord, canUseTimeoutCard: Boolean): RiderDeliveryStatus =
  RiderDeliveryStatus(
    orderId = record.orderId,
    assignedAt = record.assignedAt.toString,
    completedAt = record.completedAt.map(_.toString),
    deadlineAt = record.deadlineAt.getOrElse(RiderTimeoutPolicy.deadlineAt(record.assignedAt)).toString,
    wasTimeout = record.wasTimeout,
    timeoutExempted = record.timeoutExempted,
    timeoutCardUsed = record.timeoutCardUsed,
    overtimeSeconds = record.overtimeSeconds,
    canUseTimeoutCard = canUseTimeoutCard && record.wasTimeout && !record.timeoutExempted
  )

final case class RiderMeAPIMessage() extends APIWithRoleMessage[RiderMeResponse]:
  override def plan(connection: Connection, username: String): IO[RiderMeResponse] =
    for
      account <- RiderAccountTable.findByUsername(connection, username)
      response <- account match
        case None => IO.pure(None)
        case Some(value) =>
          for
            assignedOrders <- OrderTable.listByRiderId(connection, value.profile.rider.id)
            availableOrders <- OrderTable.listAvailableUnassigned(connection)
            records <- RiderAssignmentTable.listByRider(connection, value.profile.rider.id)
          yield
            val nextAccount = value.copy(profile =
              value.profile.copy(
                pendingOrders = assignedOrders.filterNot(order => isHistoryOrderStatus(order.status)),
                historyOrders = assignedOrders.filter(order => isHistoryOrderStatus(order.status))
              )
            )
            val deliveryStatuses = records.map(record => statusView(record, nextAccount.profile.rider.timeoutCardCount > 0))
            Some(RiderApiSupport.riderMeResponse(username, nextAccount, availableOrders, deliveryStatuses))
      output <- response match
        case None => IO.raiseError(HttpApiError.NotFound(RiderApiSupport.riderNotFound.error))
        case Some(value) => IO.pure(value)
    yield output

final case class RiderAvailableOrdersAPIMessage() extends APIWithRoleMessage[RiderAvailableOrdersResponse]:
  override def plan(connection: Connection, username: String): IO[RiderAvailableOrdersResponse] =
    RiderAccountTable.findByUsername(connection, username).flatMap {
      case None => IO.raiseError(HttpApiError.NotFound(RiderApiSupport.riderNotFound.error))
      case Some(_) =>
        OrderTable.listAvailableUnassigned(connection).map(RiderAvailableOrdersResponse(_))
    }

final case class RiderGrabOrderAPIMessage(orderId: OrderId) extends APIWithRoleMessage[OkResponse]:
  override def plan(connection: Connection, username: String): IO[OkResponse] =
    for
      account <- RiderAccountTable.findByUsername(connection, username).flatMap {
        case Some(value) => IO.pure(value)
        case None        => IO.raiseError(HttpApiError.BadRequest("未找到骑手账号"))
      }
      activeOrderCount <- OrderTable.countActiveByRider(connection, account.profile.rider.id)
      _ <-
        if activeOrderCount >= 5 then IO.raiseError(HttpApiError.BadRequest("当前骑手最多同时配送 5 单"))
        else IO.unit
      order <- OrderTable.findById(connection, orderId).flatMap {
        case Some(value) => IO.pure(value)
        case None        => IO.raiseError(HttpApiError.BadRequest("未找到订单"))
      }
      _ <-
        if !isAvailableOrder(order.status) || order.riderId.nonEmpty then IO.raiseError(HttpApiError.BadRequest("订单已被其他骑手抢走"))
        else IO.unit
      updatedOrder = order.copy(riderId = Some(account.profile.rider.id), status = OrderStatus.配送中)
      updatedRider = account.profile.rider.copy(status = RiderStatus.配送中, totalOrders = account.profile.rider.totalOrders + 1)
      _ <- OrderTable.upsert(connection, updatedOrder)
      _ <- RiderAccountTable.upsert(connection, account.copy(profile = account.profile.copy(rider = updatedRider)))
      _ <- RiderAssignmentTable.upsert(connection, updatedRider.id, updatedOrder.id, updatedOrder.status)
    yield OkResponse(ok = true)

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
        else if order.status != OrderStatus.配送中 then IO.raiseError(HttpApiError.BadRequest(s"当前状态不可执行更新状态：${order.status}"))
        else if targetStatus != OrderStatus.已送达 then IO.raiseError(HttpApiError.BadRequest("骑手只能将配送中订单更新为已送达"))
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
      updatedOrder = order.copy(status = targetStatus)
      remainingPending <- OrderTable.countActiveByRider(connection, account.profile.rider.id, excludingOrderId = Some(orderId))
      nextStatus = if remainingPending > 0 then RiderStatus.配送中 else RiderStatus.空闲
      currentRider = account.profile.rider
      updatedRider = currentRider.copy(
        status = nextStatus,
        salary = currentRider.salary + 5,
        energyPoints = currentRider.energyPoints + earnedEnergy,
        timeoutCardCount = if autoUseCard then currentRider.timeoutCardCount - 1 else currentRider.timeoutCardCount,
        timeoutCount = if wasTimeout then currentRider.timeoutCount + 1 else currentRider.timeoutCount,
        timeoutExemptedCount = if autoUseCard then currentRider.timeoutExemptedCount + 1 else currentRider.timeoutExemptedCount
      )
      _ <- OrderTable.upsert(connection, updatedOrder)
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

final case class RiderRedeemTimeoutCardAPIMessage() extends APIWithRoleMessage[RiderTimeoutCardRedeemResponse]:
  override def plan(connection: Connection, username: String): IO[RiderTimeoutCardRedeemResponse] =
    for
      account <- RiderAccountTable.findByUsername(connection, username).flatMap {
        case Some(value) => IO.pure(value)
        case None        => IO.raiseError(HttpApiError.BadRequest("未找到骑手账号"))
      }
      rider = account.profile.rider
      _ <-
        if rider.energyPoints < RiderTimeoutPolicy.TimeoutCardEnergyCost then IO.raiseError(HttpApiError.BadRequest("能量值不足，暂无法兑换超时免责卡"))
        else IO.unit
      updatedRider = rider.copy(
        energyPoints = rider.energyPoints - RiderTimeoutPolicy.TimeoutCardEnergyCost,
        timeoutCardCount = rider.timeoutCardCount + 1
      )
      _ <- RiderAccountTable.upsert(connection, account.copy(profile = account.profile.copy(rider = updatedRider)))
    yield RiderTimeoutCardRedeemResponse(true, updatedRider.energyPoints, updatedRider.timeoutCardCount)

final case class RiderUseTimeoutCardAPIMessage(orderId: OrderId) extends APIWithRoleMessage[RiderUseTimeoutCardResponse]:
  override def plan(connection: Connection, username: String): IO[RiderUseTimeoutCardResponse] =
    for
      account <- RiderAccountTable.findByUsername(connection, username).flatMap {
        case Some(value) => IO.pure(value)
        case None        => IO.raiseError(HttpApiError.BadRequest("未找到骑手账号"))
      }
      assignment <- RiderAssignmentTable.find(connection, account.profile.rider.id, orderId).flatMap {
        case Some(value) => IO.pure(value)
        case None        => IO.raiseError(HttpApiError.BadRequest("未找到骑手派单记录"))
      }
      _ <-
        if !assignment.wasTimeout then IO.raiseError(HttpApiError.BadRequest("该订单未超时，无需使用免责卡"))
        else if assignment.timeoutExempted then IO.raiseError(HttpApiError.BadRequest("该订单已免责"))
        else if account.profile.rider.timeoutCardCount <= 0 then IO.raiseError(HttpApiError.BadRequest("暂无可用超时免责卡"))
        else IO.unit
      updatedRider = account.profile.rider.copy(
        timeoutCardCount = account.profile.rider.timeoutCardCount - 1,
        timeoutExemptedCount = account.profile.rider.timeoutExemptedCount + 1
      )
      _ <- RiderAccountTable.upsert(connection, account.copy(profile = account.profile.copy(rider = updatedRider)))
      _ <- RiderAssignmentTable.markTimeoutExempted(connection, updatedRider.id, orderId)
    yield RiderUseTimeoutCardResponse(true, orderId, updatedRider.timeoutCardCount, timeoutExempted = true)
