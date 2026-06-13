package delivery.user.api

import delivery.user.services.UserAccountService
import cats.effect.IO
import delivery.platform.api.{APIMessage, HttpApiError}
import delivery.domain.{UserRole}
import delivery.domain.apiTypes.{OkResponse}
import delivery.user.tables.AuthCredentialRecord
import delivery.user.tables.authcredential.AuthCredentialTable

import java.sql.Connection

final case class RegisterAPIMessage(role: UserRole, username: String, password: String) extends APIMessage[OkResponse]:
  override def plan(connection: Connection): IO[OkResponse] =
    val roleValue = role.toString
    for
      existing <- AuthCredentialTable.find(connection, roleValue, username)
      _ <- existing match
        case Some(_) => IO.raiseError(HttpApiError.BadRequest("该角色下账号已存在。"))
        case None    => IO.unit
      _ <- AuthCredentialTable.upsert(connection, AuthCredentialRecord(roleValue, username, password))
      _ <- role match
        case UserRole.customer => UserAccountService.registerCustomer(connection, username, password)
        case UserRole.merchant => UserAccountService.registerMerchant(connection, username, password)
        case UserRole.rider    => UserAccountService.registerRider(connection, username, password)
        case UserRole.admin    => IO.unit
    yield OkResponse(ok = true)
