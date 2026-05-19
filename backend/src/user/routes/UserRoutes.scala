package delivery.user.routes

import cats.effect.IO
import delivery.shared.db.DeliveryStateStore
import delivery.shared.http.AuthHttp
import delivery.shared.json.ApiJsonCodecs.given
import delivery.shared.objects.ErrorBody
import delivery.user.api.*
import delivery.user.objects.{CustomerProfilePatch, LoginRequest, RegisterRequest}
import delivery.user.utils.UserApiSupport
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.given
import org.http4s.dsl.io.*

import javax.sql.DataSource

object UserRoutes:

  def routes(ds: DataSource): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case req @ POST -> Root / "login" =>
        for
          body <- req.as[LoginRequest]
          current <- DeliveryStateStore.load(ds)
          response <- LoginApi.plan(LoginApi.LoginCommand(current, body)).flatMap {
            case Left(msg) => AuthHttp.unauthorizedJson(msg)
            case Right(output) => Ok(output)
          }
        yield response

      case req @ POST -> Root / "register" =>
        for
          body <- req.as[RegisterRequest]
          current <- DeliveryStateStore.load(ds)
          response <- RegisterApi.plan(RegisterApi.RegisterCommand(current, body)).flatMap {
            case Left(msg) if msg == UserApiSupport.invalidRole.error => BadRequest(UserApiSupport.invalidRole)
            case Left(msg) => BadRequest(ErrorBody(msg))
            case Right(output) => DeliveryStateStore.save(ds)(output.nextState) *> Ok(output.response)
          }
        yield response

      case req @ GET -> Root / "me" =>
        AuthHttp.requireRole(req, "customer") { username =>
          DeliveryStateStore.load(ds).flatMap(state => CustomerMeApi.plan(CustomerMeApi.CustomerMeQuery(state, username))).flatMap {
            case None => NotFound(UserApiSupport.customerNotFound)
            case Some(output) => Ok(output)
          }
        }

      case req @ PATCH -> Root / "me" / "profile" =>
        AuthHttp.requireRole(req, "customer") { username =>
          for
            body <- req.as[CustomerProfilePatch]
            current <- DeliveryStateStore.load(ds)
            response <- CustomerProfilePatchApi
              .plan(CustomerProfilePatchApi.CustomerProfilePatchCommand(current, username, body))
              .flatMap {
                case Left(msg) => BadRequest(ErrorBody(msg))
                case Right(output) => DeliveryStateStore.save(ds)(output.nextState) *> Ok(output.response)
              }
          yield response
        }
    }

end UserRoutes
