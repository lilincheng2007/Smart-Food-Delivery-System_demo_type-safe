package delivery.rider.api

import cats.effect.IO
import delivery.rider.objects.RiderMeResponse
import delivery.rider.tables.rideraccount.RiderAccountTable
import delivery.rider.tables.riderassignment.RiderAssignmentTable
import delivery.rider.utils.RiderApiSupport
import delivery.order.tables.order.OrderTable
import delivery.shared.api.{APIWithRoleMessage, HttpApiError}
import delivery.shared.objects.OkResponse

import java.sql.Connection

private def isHistoryOrderStatus(status: String): Boolean =
  status == "已送达" || status == "已完成" || status == "已取消"

final case class RiderMeAPIMessage() extends APIWithRoleMessage[RiderMeResponse]:
  override def plan(connection: Connection, username: String): IO[RiderMeResponse] =
    for
      account <- RiderAccountTable.findByUsername(connection, username)
      response <- account match
        case None => IO.pure(None)
        case Some(value) =>
          OrderTable.list(connection).map { orders =>
            val riderId = value.profile.rider.id
            val assignedOrders = orders.filter(_.riderId.contains(riderId))
            val nextAccount = value.copy(profile =
              value.profile.copy(
                pendingOrders = assignedOrders.filterNot(order => isHistoryOrderStatus(order.status)),
                historyOrders = assignedOrders.filter(order => isHistoryOrderStatus(order.status))
              )
            )
            val availableOrders = orders.filter(order => order.status == "待接单" && order.riderId.isEmpty)
            Some(RiderApiSupport.riderMeResponse(username, nextAccount, availableOrders))
          }
      output <- response match
        case None => IO.raiseError(HttpApiError.NotFound(RiderApiSupport.riderNotFound.error))
        case Some(value) => IO.pure(value)
    yield output

final case class RiderGrabOrderAPIMessage(orderId: String) extends APIWithRoleMessage[OkResponse]:
  override def plan(connection: Connection, username: String): IO[OkResponse] =
    for
      account <- RiderAccountTable.findByUsername(connection, username).flatMap {
        case Some(value) => IO.pure(value)
        case None        => IO.raiseError(HttpApiError.BadRequest("未找到骑手账号"))
      }
      orders <- OrderTable.list(connection)
      _ <-
        if orders.count(order => order.riderId.contains(account.profile.rider.id) && !isHistoryOrderStatus(order.status)) >= 5 then
          IO.raiseError(HttpApiError.BadRequest("当前骑手最多同时配送 5 单"))
        else IO.unit
      order <- OrderTable.findById(connection, orderId).flatMap {
        case Some(value) => IO.pure(value)
        case None        => IO.raiseError(HttpApiError.BadRequest("未找到订单"))
      }
      _ <-
        if order.status != "待接单" || order.riderId.nonEmpty then IO.raiseError(HttpApiError.BadRequest("订单已被其他骑手抢走"))
        else IO.unit
      updatedOrder = order.copy(riderId = Some(account.profile.rider.id), status = "配送中")
      updatedRider = account.profile.rider.copy(status = "配送中", totalOrders = account.profile.rider.totalOrders + 1)
      _ <- OrderTable.upsert(connection, updatedOrder)
      _ <- RiderAccountTable.upsert(connection, account.copy(profile = account.profile.copy(rider = updatedRider)))
      _ <- RiderAssignmentTable.upsert(connection, updatedRider.id, updatedOrder.id, updatedOrder.status)
    yield OkResponse(ok = true)

final case class RiderUpdateOrderStatusAPIMessage(orderId: String) extends APIWithRoleMessage[OkResponse]:
  override def plan(connection: Connection, username: String): IO[OkResponse] =
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
        else if order.status != "配送中" then IO.raiseError(HttpApiError.BadRequest(s"当前状态不可执行更新状态：${order.status}"))
        else IO.unit
      updatedOrder = order.copy(status = "已送达")
      remainingPending <- OrderTable.list(connection).map(_.count(existing =>
        existing.id != orderId && existing.riderId.contains(account.profile.rider.id) && !isHistoryOrderStatus(existing.status)
      ))
      nextStatus = if remainingPending > 0 then "配送中" else "空闲"
      updatedRider = account.profile.rider.copy(status = nextStatus, salary = account.profile.rider.salary + 5)
      _ <- OrderTable.upsert(connection, updatedOrder)
      _ <- RiderAccountTable.upsert(connection, account.copy(profile = account.profile.copy(rider = updatedRider)))
      _ <- RiderAssignmentTable.upsert(connection, updatedRider.id, updatedOrder.id, updatedOrder.status)
    yield OkResponse(ok = true)
