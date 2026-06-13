package delivery.user.api

import cats.effect.IO
import delivery.platform.api.{APIWithRoleMessage, HttpApiError}
import delivery.promotion.services.VoucherSupport
import delivery.user.objects.apiTypes.CustomerMeResponse
import delivery.user.tables.customerprofile.CustomerProfileTable
import delivery.user.utils.UserApiSupport

import java.sql.Connection

final case class CustomerMeAPIMessage() extends APIWithRoleMessage[CustomerMeResponse]:
  override def plan(connection: Connection, username: String): IO[CustomerMeResponse] =
    for
      response <- CustomerProfileTable.findByUsername(connection, username)
      output <- response.map { account =>
        val normalized = account.copy(profile = account.profile.copy(vouchers = VoucherSupport.mergeStandardPlatformVouchers(account.profile.id, account.profile.vouchers)))
        if normalized == account then IO.pure(UserApiSupport.customerMeResponse(username, normalized))
        else CustomerProfileTable.upsert(connection, normalized).map(_ => UserApiSupport.customerMeResponse(username, normalized))
      } match
        case None => IO.raiseError(HttpApiError.NotFound(UserApiSupport.customerNotFound.error))
        case Some(value) => value
    yield output
