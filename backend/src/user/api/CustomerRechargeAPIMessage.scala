package delivery.user.api

import cats.effect.IO
import delivery.platform.api.{APIWithRoleMessage, HttpApiError}
import delivery.user.objects.apiTypes.CustomerWalletTopUpResponse
import delivery.user.tables.customerprofile.CustomerProfileTable
import delivery.user.utils.UserApiSupport

import java.sql.Connection

final case class CustomerRechargeAPIMessage(amount: Double) extends APIWithRoleMessage[CustomerWalletTopUpResponse]:
  override def plan(connection: Connection, username: String): IO[CustomerWalletTopUpResponse] =
    if amount <= 0 || amount.isNaN || amount.isInfinity then IO.raiseError(HttpApiError.BadRequest("充值金额必须为正数"))
    else
      for
        account <- CustomerProfileTable.findByUsername(connection, username).flatMap {
          case Some(value) => IO.pure(value)
          case None        => IO.raiseError(HttpApiError.NotFound(UserApiSupport.customerNotFound.error))
        }
        nextWalletBalance = BigDecimal(account.profile.walletBalance + amount).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble
        nextAccount = account.copy(profile = account.profile.copy(walletBalance = nextWalletBalance))
        _ <- CustomerProfileTable.upsert(connection, nextAccount)
      yield CustomerWalletTopUpResponse(nextWalletBalance)
