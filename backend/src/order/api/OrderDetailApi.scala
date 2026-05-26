package delivery.order.api

import cats.effect.IO
import delivery.order.objects.Order
import delivery.shared.api.ApiPlan
import delivery.shared.objects.DeliveryState
import org.typelevel.log4cats.slf4j.Slf4jLogger

object OrderDetailApi extends ApiPlan[OrderDetailApi.OrderDetailQuery, Option[Order]]:

  final case class OrderDetailQuery(state: DeliveryState, username: String, orderId: String)

  private val logger = Slf4jLogger.getLogger[IO]

  override val name: String = "OrderDetailApi"

  override def plan(input: OrderDetailApi.OrderDetailQuery): IO[Option[Order]] =
    for
      _ <- logger.info(s"$name started, username=${input.username}, orderId=${input.orderId}")
      response = input.state.user.customerAccounts.find(_.username == input.username).flatMap { account =>
        input.state.order.orders
          .find(order => order.id == input.orderId && order.customerId == account.profile.id)
          .orElse((account.profile.pendingOrders ++ account.profile.historyOrders).find(_.id == input.orderId))
      }
      _ <- logger.info(s"$name finished, found=${response.isDefined}")
    yield response

end OrderDetailApi
