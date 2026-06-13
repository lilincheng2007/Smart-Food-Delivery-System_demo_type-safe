package delivery.rider.api

import cats.effect.IO
import delivery.rider.objects.apiTypes.RiderTimeoutCardRedeemResponse
import delivery.rider.tables.rideraccount.RiderAccountTable
import delivery.rider.utils.RiderTimeoutPolicy
import delivery.platform.api.{APIWithRoleMessage, HttpApiError}

import java.sql.Connection

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
