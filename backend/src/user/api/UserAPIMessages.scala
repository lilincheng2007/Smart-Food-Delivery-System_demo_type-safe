package delivery.user.api

import cats.effect.IO
import delivery.shared.api.{APIMessage, APIWithRoleMessage, HttpApiError}
import delivery.shared.db.DeliveryStateStore
import delivery.shared.objects.OkResponse
import delivery.user.objects.{CustomerMeResponse, CustomerProfilePatch, LoginRequest, LoginResponse, RegisterRequest}
import delivery.user.utils.UserApiSupport

import javax.sql.DataSource

final case class LoginAPIMessage(role: String, username: String, password: String) extends APIMessage[LoginResponse]:
  override def plan(ds: DataSource): IO[LoginResponse] =
    for
      state <- DeliveryStateStore.load(ds)
      response <- LoginApi.plan(LoginApi.LoginCommand(state, LoginRequest(role, username, password)))
      output <- response match
        case Left(msg) => IO.raiseError(HttpApiError.Unauthorized(msg))
        case Right(value) => IO.pure(value)
    yield output

final case class RegisterAPIMessage(role: String, username: String, password: String) extends APIMessage[OkResponse]:
  override def plan(ds: DataSource): IO[OkResponse] =
    for
      state <- DeliveryStateStore.load(ds)
      response <- RegisterApi.plan(RegisterApi.RegisterCommand(state, RegisterRequest(role, username, password)))
      output <- response match
        case Left(msg) if msg == UserApiSupport.invalidRole.error => IO.raiseError(HttpApiError.BadRequest(msg))
        case Left(msg) => IO.raiseError(HttpApiError.BadRequest(msg))
        case Right(value) => DeliveryStateStore.save(ds)(value.nextState).as(value.response)
    yield output

final case class CustomerMeAPIMessage() extends APIWithRoleMessage[CustomerMeResponse]:
  override def plan(ds: DataSource, username: String): IO[CustomerMeResponse] =
    for
      state <- DeliveryStateStore.load(ds)
      response <- CustomerMeApi.plan(CustomerMeApi.CustomerMeQuery(state, username))
      output <- response match
        case None => IO.raiseError(HttpApiError.NotFound(UserApiSupport.customerNotFound.error))
        case Some(value) => IO.pure(value)
    yield output

final case class CustomerProfilePatchAPIMessage(patch: CustomerProfilePatch) extends APIWithRoleMessage[OkResponse]:
  override def plan(ds: DataSource, username: String): IO[OkResponse] =
    for
      state <- DeliveryStateStore.load(ds)
      response <- CustomerProfilePatchApi.plan(CustomerProfilePatchApi.CustomerProfilePatchCommand(state, username, patch))
      output <- response match
        case Left(msg) => IO.raiseError(HttpApiError.BadRequest(msg))
        case Right(value) => DeliveryStateStore.save(ds)(value.nextState).as(value.response)
    yield output
