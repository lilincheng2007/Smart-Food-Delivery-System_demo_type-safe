package delivery.platform.interop

import cats.effect.IO
import org.http4s.Header
import org.http4s.Request
import org.typelevel.ci.CIStringSyntax

object InternalToken:

  val headerName: String = "X-Internal-Token"

  def value: IO[String] =
    IO.delay(sys.env.getOrElse("SERVICE_INTERNAL_TOKEN", "dev-internal-token"))

  def authorize(req: Request[?]): IO[Boolean] =
    value.map(token => req.headers.get(ci"X-Internal-Token").map(_.head.value).contains(token))

  def header: IO[Header.Raw] =
    value.map(token => Header.Raw(ci"X-Internal-Token", token))

end InternalToken
