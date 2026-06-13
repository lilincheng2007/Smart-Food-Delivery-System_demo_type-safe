package delivery.user.api

import delivery.user.services.UserAccountService
import cats.effect.IO
import delivery.platform.api.{APIWithRoleMessage, HttpApiError}
import delivery.domain.apiTypes.OkResponse
import delivery.user.objects.CustomerProfilePatch
import delivery.user.tables.customerprofile.CustomerProfileTable
import delivery.user.utils.UserApiSupport

import java.sql.Connection

final case class CustomerProfilePatchAPIMessage(patch: CustomerProfilePatch) extends APIWithRoleMessage[OkResponse]:
  override def plan(connection: Connection, username: String): IO[OkResponse] =
    for
      account <- CustomerProfileTable.findByUsername(connection, username).flatMap {
        case Some(value) => IO.pure(value)
        case None        => IO.raiseError(HttpApiError.NotFound(UserApiSupport.customerNotFound.error))
      }
      nextAccount <- UserAccountService.patchCustomerAccount(account, patch) match
        case Left(msg) => IO.raiseError(HttpApiError.BadRequest(msg))
        case Right(value) => IO.pure(value)
      _ <- CustomerProfileTable.upsert(connection, nextAccount)
    yield OkResponse(ok = true)
