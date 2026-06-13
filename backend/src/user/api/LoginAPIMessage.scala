package delivery.user.api

import cats.effect.IO
import delivery.platform.api.{APIMessage, HttpApiError}
import delivery.auth.JwtSupport
import delivery.domain.UserRole
import delivery.user.objects.apiTypes.LoginResponse
import delivery.user.tables.authcredential.AuthCredentialTable

import java.sql.Connection

final case class LoginAPIMessage(role: UserRole, username: String, password: String) extends APIMessage[LoginResponse]:
  override def plan(connection: Connection): IO[LoginResponse] =
    val roleValue = role.toString
    for
      credential <- AuthCredentialTable.find(connection, roleValue, username)
      _ <- credential.map(_.password) match
        case None                     => IO.raiseError(HttpApiError.Unauthorized(s"未找到该角色下的账号：$username"))
        case Some(value) if value != password => IO.raiseError(HttpApiError.Unauthorized("密码错误，请重新输入。"))
        case Some(_)                  => IO.unit
      token <- JwtSupport.signToken(username, roleValue)
    yield LoginResponse(token, username, role)
