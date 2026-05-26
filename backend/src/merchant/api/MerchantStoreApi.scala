package delivery.merchant.api

import cats.effect.IO
import delivery.shared.api.ApiPlan
import delivery.merchant.objects.CreateStoreRequest
import delivery.merchant.tables.MerchantDomainOps
import delivery.shared.objects.DeliveryState
import delivery.shared.db.DeliveryStateOps
import org.typelevel.log4cats.slf4j.Slf4jLogger

object MerchantStoreApi extends ApiPlan[MerchantStoreApi.MerchantStoreCommand, Either[String, MerchantStoreApi.MerchantStoreSuccess]]:

  final case class MerchantStoreCommand(
      state: DeliveryState,
      username: String,
      body: CreateStoreRequest
  )

  private val logger = Slf4jLogger.getLogger[IO]

  override val name: String = "MerchantStoreApi"

  override def plan(input: MerchantStoreApi.MerchantStoreCommand): IO[Either[String, MerchantStoreApi.MerchantStoreSuccess]] =
    for
      _ <- logger.info(s"$name started, username=${input.username}")
      response <- MerchantDomainOps.createMerchantStore(input.state.merchant, input.username, input.body.storeName, input.body.address).flatMap {
        case Left(msg) => IO.pure(Left(msg))
        case Right((nextMerchant, merchantId)) =>
          IO.pure(
            Right(MerchantStoreSuccess(DeliveryStateOps.withMerchantState(input.state, nextMerchant), merchantId))
          )
      }
      _ <- logger.info(s"$name finished, success=${response.isRight}")
    yield response

  final case class MerchantStoreSuccess(nextState: DeliveryState, response: String)

end MerchantStoreApi
