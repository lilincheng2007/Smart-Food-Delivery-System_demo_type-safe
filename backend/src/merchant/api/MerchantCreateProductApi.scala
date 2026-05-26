package delivery.merchant.api

import cats.effect.IO
import delivery.merchant.objects.{CreateProductRequest, Product}
import delivery.merchant.tables.MerchantDomainOps
import delivery.shared.api.ApiPlan
import delivery.shared.objects.DeliveryState
import delivery.shared.db.DeliveryStateOps
import org.typelevel.log4cats.slf4j.Slf4jLogger

object MerchantCreateProductApi extends ApiPlan[
  MerchantCreateProductApi.MerchantCreateProductCommand,
  Either[String, MerchantCreateProductApi.MerchantCreateProductSuccess]
]:

  final case class MerchantCreateProductCommand(
      state: DeliveryState,
      username: String,
      body: CreateProductRequest
  )

  final case class MerchantCreateProductSuccess(nextState: DeliveryState, response: Product)

  private val logger = Slf4jLogger.getLogger[IO]

  override val name: String = "MerchantCreateProductApi"

  override def plan(input: MerchantCreateProductCommand): IO[Either[String, MerchantCreateProductSuccess]] =
    for
      _ <- logger.info(s"$name started, username=${input.username}, merchantId=${input.body.merchantId}")
      response <- MerchantDomainOps.createProduct(
        input.state.merchant,
        input.username,
        input.body.merchantId,
        input.body.name,
        input.body.description,
        input.body.price,
        input.body.remainingStock,
        input.body.listingStatus
      ).map(_.map { case (nextMerchant, createdProduct) =>
        MerchantCreateProductSuccess(
          DeliveryStateOps.withMerchantState(input.state, nextMerchant),
          createdProduct
        )
      })
      _ <- logger.info(s"$name finished, success=${response.isRight}")
    yield response

end MerchantCreateProductApi
