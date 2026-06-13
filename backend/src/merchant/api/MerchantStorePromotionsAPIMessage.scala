package delivery.merchant.api

import cats.effect.IO
import delivery.merchant.tables.merchantstore.MerchantStoreTable
import delivery.merchant.services.MerchantOwnedProductService
import delivery.merchant.validators.MerchantStoreOwnershipValidator
import delivery.platform.api.{APIWithRoleMessage, HttpApiError}
import delivery.domain.{MerchantId, Promotion}
import delivery.domain.apiTypes.OkResponse
import delivery.promotion.validators.PromotionValidator

import java.sql.Connection

final case class MerchantStorePromotionsAPIMessage(merchantId: MerchantId, promotions: List[Promotion]) extends APIWithRoleMessage[OkResponse]:
  override def plan(connection: Connection, username: String): IO[OkResponse] =
    PromotionValidator.validate(promotions) match
      case Some(message) => IO.raiseError(HttpApiError.BadRequest(message))
      case None =>
        for
          merchant <- MerchantStoreOwnershipValidator.requireOwnedStore(connection, username, merchantId)
          products <- MerchantOwnedProductService.listOwnedProducts(connection, username, merchantId)
          productPromotionError = PromotionValidator.validateMerchantProductPromotions(promotions, products)
          _ <- productPromotionError match
            case Some(message) => IO.raiseError(HttpApiError.BadRequest(message))
            case None          => IO.unit
          _ <- MerchantStoreTable.upsert(connection, username, merchant.copy(promotions = promotions.take(20)))
        yield OkResponse(ok = true)
