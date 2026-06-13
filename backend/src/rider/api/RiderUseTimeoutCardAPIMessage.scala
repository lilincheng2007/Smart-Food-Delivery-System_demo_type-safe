package delivery.rider.api

import cats.effect.IO
import delivery.rider.objects.apiTypes.RiderUseTimeoutCardResponse
import delivery.rider.tables.rideraccount.RiderAccountTable
import delivery.rider.tables.riderassignment.RiderAssignmentTable
import delivery.platform.api.{APIWithRoleMessage, HttpApiError}
import delivery.domain.OrderId

import java.sql.Connection

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
