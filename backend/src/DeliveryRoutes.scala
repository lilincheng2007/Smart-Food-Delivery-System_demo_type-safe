package delivery

import cats.effect.IO
import delivery.admin.routes.AdminRoutes
import delivery.merchant.routes.MerchantRoutes
import delivery.order.routes.OrderRoutes
import delivery.rider.routes.RiderRoutes
import delivery.user.routes.UserRoutes
import org.http4s.HttpRoutes
import org.http4s.server.Router

import javax.sql.DataSource

object DeliveryRoutes:

  def apply(ds: DataSource): HttpRoutes[IO] =
    Router(
      "/api/admin" -> AdminRoutes.routes(ds),
      "/api/user" -> UserRoutes.routes(ds),
      "/api/merchant/store-images" -> MerchantRoutes.storeImagePublicRoutes,
      "/api/merchant" -> MerchantRoutes.routes(ds),
      "/api/rider" -> RiderRoutes.routes(ds),
      "/api/order" -> OrderRoutes.routes(ds)
    )

end DeliveryRoutes
