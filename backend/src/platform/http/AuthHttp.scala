package delivery.platform.http

import cats.effect.IO
import cats.syntax.all.*
import delivery.auth.JwtSupport
import delivery.platform.json.ApiJsonCodecs.given
import delivery.domain.ErrorBody
import org.http4s.*
import org.http4s.circe.CirceEntityCodec.given
import org.http4s.dsl.io.*
import org.http4s.headers.Authorization

object AuthHttp:

  def unauthorizedJson(msg: String): IO[Response[IO]] =
    Response[IO](Status.Unauthorized).withEntity(ErrorBody(msg)).pure[IO]

  private def bearerToken(req: Request[IO]): Option[String] =
    req.headers.get[Authorization].collect {
      case Authorization(Credentials.Token(AuthScheme.Bearer, t)) => t
    }

  def withBearer(req: Request[IO])(f: (String, String) => IO[Response[IO]]): IO[Response[IO]] =
    bearerToken(req) match
      case None => unauthorizedJson("缺少 Authorization Bearer token")
      case Some(token) =>
        JwtSupport.verifyToken(token).flatMap {
          case Left(msg)   => unauthorizedJson(msg)
          case Right(pair) => f(pair._1, pair._2)
        }

  def requireRole(req: Request[IO], expected: String)(use: String => IO[Response[IO]]): IO[Response[IO]] =
    withBearer(req) { (user, role) =>
      if role != expected then Forbidden(ErrorBody("权限不足"))
      else use(user)
    }

end AuthHttp
