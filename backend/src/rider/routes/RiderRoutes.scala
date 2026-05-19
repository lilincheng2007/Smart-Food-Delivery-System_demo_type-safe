package delivery.rider.routes

import cats.effect.IO
import delivery.rider.api.{RiderGrabOrderApi, RiderMeApi, RiderUpdateOrderStatusApi}
import delivery.rider.utils.RiderApiSupport
import delivery.shared.db.DeliveryStateStore
import delivery.shared.http.AuthHttp
import delivery.shared.json.ApiJsonCodecs.given
import delivery.shared.objects.{ErrorBody, OkResponse}
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.given
import org.http4s.dsl.io.*

import javax.sql.DataSource

object RiderRoutes:

  def routes(ds: DataSource): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case req @ GET -> Root / "me" =>
        AuthHttp.requireRole(req, "rider") { username =>
          DeliveryStateStore.load(ds).flatMap(state => RiderMeApi.plan(RiderMeApi.RiderMeQuery(state, username))).flatMap {
            case None => NotFound(RiderApiSupport.riderNotFound)
            case Some(output) => Ok(output)
          }
        }

      case req @ POST -> Root / "me" / "orders" / orderId / "grab" =>
        AuthHttp.requireRole(req, "rider") { username =>
          DeliveryStateStore.load(ds).flatMap { current =>
            RiderGrabOrderApi.plan(RiderGrabOrderApi.RiderGrabOrderCommand(current, username, orderId)) match
              case Left(msg) => IO.pure(Left(msg))
              case Right(output) => DeliveryStateStore.save(ds)(output.nextState).as(Right(output.nextState))
          }.flatMap {
            case Left(msg) => BadRequest(ErrorBody(msg))
            case Right(_) => Ok(OkResponse(ok = true))
          }
        }

      case req @ POST -> Root / "me" / "orders" / orderId / "status" =>
        AuthHttp.requireRole(req, "rider") { username =>
          DeliveryStateStore.load(ds).flatMap { current =>
            RiderUpdateOrderStatusApi.plan(RiderUpdateOrderStatusApi.RiderUpdateOrderStatusCommand(current, username, orderId)) match
              case Left(msg) => IO.pure(Left(msg))
              case Right(output) => DeliveryStateStore.save(ds)(output.nextState).as(Right(output))
          }.flatMap {
            case Left(msg) => BadRequest(ErrorBody(msg))
            case Right(output) => Ok(output.response)
          }
        }
    }

end RiderRoutes
