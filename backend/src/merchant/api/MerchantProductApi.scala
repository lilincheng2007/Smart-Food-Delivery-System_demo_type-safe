package delivery.merchant.api

import cats.effect.IO
import delivery.merchant.objects.{Product, UpdateProductRequest}
import delivery.merchant.tables.MerchantDomainOps
import delivery.shared.api.ApiPlan
import delivery.shared.objects.DeliveryState
import delivery.shared.db.DeliveryStateOps
import org.typelevel.log4cats.slf4j.Slf4jLogger

object MerchantProductApi extends ApiPlan[
  MerchantProductApi.MerchantProductCommand,
  Either[String, MerchantProductApi.MerchantProductSuccess]
]:

  final case class MerchantProductCommand(
      state: DeliveryState,
      username: String,
      productId: String,
      body: UpdateProductRequest
  )

  final case class MerchantProductSuccess(nextState: DeliveryState, response: Product)

  private val logger = Slf4jLogger.getLogger[IO]

  override val name: String = "MerchantProductApi"

  override def plan(input: MerchantProductCommand): IO[Either[String, MerchantProductSuccess]] =
    for
      _ <- logger.info(s"$name started, username=${input.username}, productId=${input.productId}")
      response = MerchantDomainOps.updateProduct(
        input.state.merchant,
        input.username,
        input.productId,
        input.body.name,
        input.body.description,
        input.body.price,
        input.body.remainingStock,
        input.body.listingStatus
      ).map { case (nextMerchant, updatedProduct) =>
        MerchantProductSuccess(
          DeliveryStateOps.withMerchantState(input.state, nextMerchant),
          updatedProduct
        )
      }
      _ <- logger.info(s"$name finished, success=${response.isRight}")
    yield response

end MerchantProductApi
