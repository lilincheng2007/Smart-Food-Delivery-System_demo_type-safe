package delivery

import cats.effect.{IO, IOApp, Resource}
import com.comcast.ip4s.*
import delivery.shared.db.{DatabasePool, DeliveryStateStore}
import delivery.shared.json.ApiJsonCodecs.given
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.middleware.CORS
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import javax.sql.DataSource

object Main extends IOApp.Simple:

  private given Logger[IO] = Slf4jLogger.getLogger[IO]

  def run: IO[Unit] =
    dbBackedServer.use(_ => IO.never)

  private def dbBackedServer: Resource[IO, Unit] =
    for
      ds <- DatabasePool.resource
      _ <- Resource.eval(DeliveryStateStore.migrate(ds))
      _ <- buildServer(ds)
    yield ()

  private def buildServer(ds: DataSource): Resource[IO, Unit] =
    val httpApp = CORS.policy.withAllowOriginAll.httpApp(DeliveryRoutes(ds).orNotFound)
    EmberServerBuilder
      .default[IO]
      .withHost(host"0.0.0.0")
      .withPort(port"8787")
      .withHttpApp(httpApp)
      .build
      .evalTap(_ => Logger[IO].info("delivery-backend listening on 0.0.0.0:8787"))
      .map(_ => ())

end Main
