package delivery.merchant.api

import cats.effect.IO
import delivery.merchant.objects.UpdateStoreImageRequest
import delivery.merchant.tables.MerchantDomainOps
import delivery.shared.api.ApiPlan
import delivery.shared.objects.{DeliveryState, OkResponse}
import delivery.shared.db.DeliveryStateOps
import org.typelevel.log4cats.slf4j.Slf4jLogger

object MerchantStoreImageApi extends ApiPlan[
  MerchantStoreImageApi.MerchantStoreImageCommand,
  Either[String, MerchantStoreImageApi.MerchantStoreImageSuccess]
]:

  final case class MerchantStoreImageCommand(
      state: DeliveryState,
      username: String,
      merchantId: String,
      body: UpdateStoreImageRequest
  )

  final case class MerchantStoreImageSuccess(nextState: DeliveryState, response: OkResponse)

  private val logger = Slf4jLogger.getLogger[IO]

  override val name: String = "MerchantStoreImageApi"

  override def plan(input: MerchantStoreImageCommand): IO[Either[String, MerchantStoreImageSuccess]] =
    for
      _ <- logger.info(s"$name started, username=${input.username}, merchantId=${input.merchantId}")
      response = MerchantDomainOps
        .updateStoreImage(input.state.merchant, input.username, input.merchantId, input.body.imageUrl)
        .map(nextMerchant =>
          MerchantStoreImageSuccess(
            DeliveryStateOps.withMerchantState(input.state, nextMerchant),
            OkResponse(ok = true)
          )
        )
      _ <- logger.info(s"$name finished, success=${response.isRight}")
    yield response

end MerchantStoreImageApi
