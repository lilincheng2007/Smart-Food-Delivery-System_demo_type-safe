package delivery.rider.api

import cats.effect.IO
import delivery.order.tables.order.OrderTable
import delivery.rider.tables.rideraccount.RiderAccountTable
import delivery.rider.tables.riderassignment.RiderAssignmentTable
import delivery.shared.api.{APIWithRoleMessage, HttpApiError}
import delivery.shared.objects.{OrderId, OrderStatus, RiderStatus}
import delivery.shared.objects.apiTypes.{OkResponse}

import java.sql.Connection

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
        if !RiderAPIMessageSupport.isAvailableOrder(order.status) || order.riderId.nonEmpty then IO.raiseError(HttpApiError.BadRequest("该订单暂不可抢或已被其他骑手抢走"))
        else IO.unit
      updatedOrder = order.copy(riderId = Some(account.profile.rider.id), status = OrderStatus.配送中)
      updatedRider = account.profile.rider.copy(status = RiderStatus.配送中, totalOrders = account.profile.rider.totalOrders + 1)
      _ <- OrderTable.upsert(connection, updatedOrder)
      _ <- RiderAccountTable.upsert(connection, account.copy(profile = account.profile.copy(rider = updatedRider)))
      _ <- RiderAssignmentTable.upsert(connection, updatedRider.id, updatedOrder.id, updatedOrder.status)
    yield OkResponse(ok = true)
