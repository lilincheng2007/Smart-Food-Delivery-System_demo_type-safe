package delivery.admin.routes

import cats.effect.IO
import delivery.admin.api.*
import delivery.admin.utils.AdminApiSupport
import delivery.shared.db.DeliveryStateStore
import delivery.shared.http.AuthHttp
import delivery.shared.json.ApiJsonCodecs.given
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.given
import org.http4s.dsl.io.*

import javax.sql.DataSource

object AdminRoutes:

  def routes(ds: DataSource): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case GET -> Root =>
        RootInfoApi.plan(RootInfoApi.RootInfoQuery).flatMap(Ok(_))

      case GET -> Root / "health" =>
        HealthApi.plan(HealthApi.HealthQuery).flatMap(Ok(_))

      case req @ GET -> Root / "me" =>
        AuthHttp.requireRole(req, "admin") { username =>
          DeliveryStateStore.load(ds).flatMap(state => AdminMeApi.plan(AdminMeApi.AdminMeQuery(state, username))).flatMap {
            case None => NotFound(AdminApiSupport.adminNotFound)
            case Some(output) => Ok(output)
          }
        }

      case req @ GET -> Root / "overview" =>
        AuthHttp.requireRole(req, "admin") { _ =>
          DeliveryStateStore.load(ds).flatMap(state => OverviewApi.plan(OverviewApi.OverviewQuery(state))).flatMap(Ok(_))
        }

      case req @ GET -> Root / "orders-panel" =>
        AuthHttp.requireRole(req, "admin") { _ =>
          DeliveryStateStore.load(ds).flatMap(state => OrdersPanelApi.plan(OrdersPanelApi.OrdersPanelQuery(state))).flatMap(Ok(_))
        }

      case req @ GET -> Root / "platform-meta" =>
        AuthHttp.requireRole(req, "admin") { _ =>
          DeliveryStateStore.load(ds).flatMap(state => PlatformMetaApi.plan(PlatformMetaApi.PlatformMetaQuery(state))).flatMap(Ok(_))
        }
    }

end AdminRoutes
