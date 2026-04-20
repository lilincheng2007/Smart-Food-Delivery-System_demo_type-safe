package delivery.shared.auth

import cats.effect.IO
import io.circe.Json
import io.circe.parser.parse
import pdi.jwt.{JwtAlgorithm, JwtClaim}
import pdi.jwt.JwtCirce
import scala.util.{Failure, Success}

object JwtSupport:

  private val DefaultSecret = "dev-delivery-jwt-secret-change-me"
  private val algo = JwtAlgorithm.HS256

  def secret: IO[String] =
    IO(Option(System.getenv("JWT_SECRET")).filter(_.nonEmpty).getOrElse(DefaultSecret))

  def signToken(username: String, role: String): IO[String] =
    for {
      nowMillis <- IO.realTime.map(_.toMillis)
      jwtSecret <- secret
      now = nowMillis / 1000
      exp = now + 7 * 24 * 3600
      content = Json
        .obj(
          "sub" -> Json.fromString(username),
          "role" -> Json.fromString(role)
        )
        .noSpaces
      claim = JwtClaim(content = content, expiration = Some(exp), issuedAt = Some(now))
    } yield JwtCirce.encode(claim, jwtSecret, algo)

  def verifyToken(token: String): IO[Either[String, (String, String)]] =
    secret.flatMap(secretValue => IO(JwtCirce.decode(token, secretValue, Seq(algo)))).flatMap {
      case Failure(e) => IO.pure(Left(e.getMessage))
      case Success(claim) =>
        val sub = claim.subject.getOrElse("")
        val role = parse(claim.content) match
          case Right(json) => json.hcursor.get[String]("role").toOption.getOrElse("")
          case Left(_)     => ""
        if sub.nonEmpty && role.nonEmpty then IO.pure(Right((sub, role)))
        else IO.pure(Left("Invalid token payload"))
    }

end JwtSupport
