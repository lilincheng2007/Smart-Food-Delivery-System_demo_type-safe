package delivery.order.api

import cats.effect.IO
import delivery.order.objects.CustomerOrdersResponse
import delivery.shared.api.ApiPlan
import delivery.shared.objects.DeliveryState
import org.typelevel.log4cats.slf4j.Slf4jLogger

object CustomerOrdersApi extends ApiPlan[CustomerOrdersApi.CustomerOrdersQuery, Option[CustomerOrdersResponse]]:

  final case class CustomerOrdersQuery(state: DeliveryState, username: String)

  private val logger = Slf4jLogger.getLogger[IO]

  override val name: String = "CustomerOrdersApi"

  override def plan(input: CustomerOrdersApi.CustomerOrdersQuery): IO[Option[CustomerOrdersResponse]] =
    for
      _ <- logger.info(s"$name started, username=${input.username}")
      response = input.state.user.customerAccounts.find(_.username == input.username).map { account =>
        CustomerOrdersResponse(
          pendingOrders = account.profile.pendingOrders,
          historyOrders = account.profile.historyOrders
        )
      }
      _ <- logger.info(s"$name finished, found=${response.isDefined}")
    yield response

end CustomerOrdersApi
