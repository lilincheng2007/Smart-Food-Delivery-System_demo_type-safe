package delivery.order.routes

import cats.effect.IO
import delivery.shared.http.AuthHttp
import delivery.order.api.CheckoutApi
import delivery.order.objects.CheckoutRequest
import delivery.order.utils.OrderApiSupport
import delivery.shared.db.DeliveryStateStore
import delivery.shared.json.ApiJsonCodecs.given
import delivery.shared.objects.ErrorBody
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.given
import org.http4s.dsl.io.*

import javax.sql.DataSource

object OrderRoutes:

  def routes(ds: DataSource): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case req @ POST -> Root / "checkout" =>
        AuthHttp.requireRole(req, "customer") { username =>
          for
            body <- req.as[CheckoutRequest]
            current <- DeliveryStateStore.load(ds)
            response <- CheckoutApi.plan(CheckoutApi.CheckoutCommand(current, username, body)).flatMap {
              case Left(CheckoutApi.CheckoutFailure.CustomerMissing) => NotFound(OrderApiSupport.customerNotFound)
              case Left(CheckoutApi.CheckoutFailure.Invalid(msg)) => BadRequest(ErrorBody(msg))
              case Right(output) => DeliveryStateStore.save(ds)(output.nextState) *> Ok(output.response)
            }
          yield response
        }
    }

end OrderRoutes
