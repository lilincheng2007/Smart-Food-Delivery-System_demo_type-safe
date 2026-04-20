package delivery

import cats.effect.IO
import cats.effect.kernel.Ref
import delivery.admin.routes.AdminRoutes
import delivery.merchant.routes.MerchantRoutes
import delivery.order.routes.OrderRoutes
import delivery.rider.routes.RiderRoutes
import delivery.shared.objects.DeliveryState
import delivery.user.routes.UserRoutes
import org.http4s.HttpRoutes
import org.http4s.server.Router

object DeliveryRoutes:

  def apply(ref: Ref[IO, DeliveryState], persist: DeliveryState => IO[Unit]): HttpRoutes[IO] =
    Router(
      "/api/admin" -> AdminRoutes.routes(ref),
      "/api/user" -> UserRoutes.routes(ref, persist),
      "/api/merchant/store-images" -> MerchantRoutes.storeImagePublicRoutes,
      "/api/merchant" -> MerchantRoutes.routes(ref, persist),
      "/api/rider" -> RiderRoutes.routes(ref, persist),
      "/api/order" -> OrderRoutes.routes(ref, persist)
    )

end DeliveryRoutes
