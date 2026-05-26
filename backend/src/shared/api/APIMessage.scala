package delivery.shared.api

import cats.effect.IO
import cats.syntax.all.*
import delivery.shared.auth.JwtSupport
import delivery.shared.db.DatabaseSession
import delivery.shared.json.ApiJsonCodecs.given
import delivery.shared.objects.ErrorBody
import io.circe.{Decoder, Encoder, Json}
import io.circe.syntax.*
import org.http4s.*
import org.http4s.circe.CirceEntityCodec.given
import org.http4s.dsl.io.*
import org.http4s.headers.Authorization

import java.sql.Connection
import javax.sql.DataSource
import scala.reflect.ClassTag

trait APIMessage[Response]:
  def plan(connection: Connection): IO[Response]

trait APIWithRoleMessage[Response] extends APIMessage[Response]:
  def plan(connection: Connection, username: String): IO[Response]

  final override def plan(connection: Connection): IO[Response] =
    IO.raiseError(HttpApiError.Unauthorized("缺少 Authorization Bearer token"))

final case class RegisteredAPIMessage(
    apiName: String,
    requiredRole: Option[String],
    planJson: (Json, Connection, Option[String]) => IO[Json]
)

object APIMessage:

  private[api] def apiNameFromClassName(className: String): String =
    val objectName = className.stripSuffix("$")
    val baseName = objectName.stripSuffix("APIMessage")
    s"${baseName}API".toLowerCase

object RegisteredAPIMessage:

  def api[Message <: APIMessage[Response], Response](using
      Decoder[Message],
      Encoder[Response],
      ClassTag[Message]
  ): RegisteredAPIMessage =
    RegisteredAPIMessage(
      apiName = nameOf[Message],
      requiredRole = None,
      planJson = (payload, connection, _) =>
        for
          message <- IO.fromEither(payload.as[Message].left.map(error => HttpApiError.BadRequest(s"请求体格式错误：${error.getMessage}")))
          response <- message.plan(connection)
        yield response.asJson
    )

  def apiWithRole[Message <: APIWithRoleMessage[Response], Response](role: String)(using
      Decoder[Message],
      Encoder[Response],
      ClassTag[Message]
  ): RegisteredAPIMessage =
    RegisteredAPIMessage(
      apiName = nameOf[Message],
      requiredRole = Some(role),
      planJson = (payload, connection, username) =>
        for
          message <- IO.fromEither(payload.as[Message].left.map(error => HttpApiError.BadRequest(s"请求体格式错误：${error.getMessage}")))
          user <- IO.fromOption(username)(HttpApiError.Unauthorized("缺少 Authorization Bearer token"))
          response <- message.plan(connection, user)
        yield response.asJson
    )

  private def nameOf[Message](using classTag: ClassTag[Message]): String =
    APIMessage.apiNameFromClassName(classTag.runtimeClass.getSimpleName)

sealed abstract class HttpApiError(message: String) extends RuntimeException(message)

object HttpApiError:
  final case class BadRequest(message: String) extends HttpApiError(message)
  final case class Unauthorized(message: String) extends HttpApiError(message)
  final case class Forbidden(message: String) extends HttpApiError(message)
  final case class NotFound(message: String) extends HttpApiError(message)
  final case class Conflict(message: String) extends HttpApiError(message)

object APIMessageRouter:

  def routes(apiMessages: List[RegisteredAPIMessage], ds: DataSource): HttpRoutes[IO] =
    val apiMessagesByName = apiMessages.map(apiMessage => normalize(apiMessage.apiName) -> apiMessage).toMap

    HttpRoutes.of[IO] { case req @ POST -> Root / apiName =>
      handleErrors {
        for
          apiMessage <- IO.fromOption(apiMessagesByName.get(normalize(apiName)))(HttpApiError.NotFound(s"不支持的 API：$apiName"))
          username <- resolveUsername(req, apiMessage.requiredRole)
          payload <- req.as[Json]
          response <- DatabaseSession.withTransactionConnection(ds)(connection => apiMessage.planJson(payload, connection, username))
          httpResponse <- Ok(response)
        yield httpResponse
      }
    }

  private def normalize(apiName: String): String =
    apiName.trim.toLowerCase

  private def resolveUsername(req: Request[IO], requiredRole: Option[String]): IO[Option[String]] =
    requiredRole match
      case None => IO.pure(None)
      case Some(expected) =>
        bearerToken(req) match
          case None => IO.raiseError(HttpApiError.Unauthorized("缺少 Authorization Bearer token"))
          case Some(token) =>
            JwtSupport.verifyToken(token).flatMap {
              case Left(msg) => IO.raiseError(HttpApiError.Unauthorized(msg))
              case Right((username, role)) =>
                if role == expected then IO.pure(Some(username))
                else IO.raiseError(HttpApiError.Forbidden("权限不足"))
            }

  private def bearerToken(req: Request[IO]): Option[String] =
    req.headers.get[Authorization].collect {
      case Authorization(Credentials.Token(AuthScheme.Bearer, token)) => token
    }

  private def handleErrors(action: IO[Response[IO]]): IO[Response[IO]] =
    action.handleErrorWith {
      case error: InvalidMessageBodyFailure =>
        BadRequest(ErrorBody(error.getMessage))
      case error: HttpApiError.BadRequest =>
        BadRequest(ErrorBody(error.getMessage))
      case error: HttpApiError.Unauthorized =>
        Response[IO](Status.Unauthorized).withEntity(ErrorBody(error.getMessage)).pure[IO]
      case error: HttpApiError.Forbidden =>
        Forbidden(ErrorBody(error.getMessage))
      case error: HttpApiError.NotFound =>
        NotFound(ErrorBody(error.getMessage))
      case error: HttpApiError.Conflict =>
        Conflict(ErrorBody(error.getMessage))
      case error =>
        InternalServerError(ErrorBody(error.getMessage))
    }

end APIMessageRouter
