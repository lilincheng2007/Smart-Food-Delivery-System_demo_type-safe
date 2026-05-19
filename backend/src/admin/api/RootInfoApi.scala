package delivery.admin.api

import cats.effect.IO
import delivery.admin.objects.{RootInfoModule, RootInfoResponse}
import delivery.admin.tables.AdminTables
import delivery.merchant.tables.MerchantTables
import delivery.order.tables.OrderTables
import delivery.shared.api.ApiPlan
import delivery.rider.tables.RiderTables
import delivery.user.tables.UserTables
import org.typelevel.log4cats.slf4j.Slf4jLogger

object RootInfoApi extends ApiPlan[RootInfoApi.RootInfoQuery.type, RootInfoResponse]:

  case object RootInfoQuery

  private val logger = Slf4jLogger.getLogger[IO]

  override val name: String = "RootInfoApi"

  override def plan(input: RootInfoApi.RootInfoQuery.type): IO[RootInfoResponse] =
    for
      _ <- logger.info(s"$name started")
      response <- IO.pure(
        RootInfoResponse(
          service = "delivery-backend",
          message = "单体服务，按 admin/order/user/merchant/rider 逻辑分层",
          modules = Map(
            "user" -> moduleInfo(UserTables.all),
            "order" -> moduleInfo(OrderTables.all),
            "merchant" -> moduleInfo(MerchantTables.all),
            "rider" -> moduleInfo(RiderTables.all),
            "admin" -> moduleInfo(AdminTables.all)
          )
        )
      )
      _ <- logger.info(s"$name finished")
    yield response

  private def moduleInfo(tableNames: List[String]): RootInfoModule =
    RootInfoModule(
      layers = List("api", "objects", "routes", "tables", "utils"),
      tables = tableNames
    )

end RootInfoApi
