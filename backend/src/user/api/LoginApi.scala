package delivery.user.api

import cats.effect.IO
import delivery.shared.api.ApiPlan
import delivery.shared.auth.JwtSupport
import delivery.shared.objects.DeliveryState
import delivery.user.objects.{LoginRequest, LoginResponse}
import delivery.user.tables.UserDomainOps
import org.typelevel.log4cats.slf4j.Slf4jLogger

object LoginApi extends ApiPlan[LoginApi.LoginCommand, Either[String, LoginResponse]]:

  final case class LoginCommand(state: DeliveryState, body: LoginRequest)

  private val logger = Slf4jLogger.getLogger[IO]

  override val name: String = "LoginApi"

  override def plan(input: LoginApi.LoginCommand): IO[Either[String, LoginResponse]] =
    for
      _ <- logger.info(s"$name started, username=${input.body.username}")
      response <- UserDomainOps.verifyLogin(input.state.user, input.body.role, input.body.username, input.body.password) match
        case Left(msg) => IO.pure(Left(msg))
        case Right(_) =>
          JwtSupport.signToken(input.body.username, input.body.role).map { token =>
            Right(LoginResponse(token, input.body.username, input.body.role))
          }
      _ <- logger.info(s"$name finished, success=${response.isRight}")
    yield response

end LoginApi
