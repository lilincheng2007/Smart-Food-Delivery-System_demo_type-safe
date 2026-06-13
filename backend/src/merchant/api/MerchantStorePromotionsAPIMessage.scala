package delivery.merchant.api

import cats.effect.IO
import delivery.merchant.tables.merchantstore.MerchantStoreTable
import delivery.merchant.utils.MerchantApiSupport
import delivery.platform.api.{APIWithRoleMessage, HttpApiError}
import delivery.domain.{MerchantId, Promotion}
import delivery.domain.apiTypes.OkResponse
import delivery.promotion.services.PromotionValidation

import java.sql.Connection

final case class MerchantStorePromotionsAPIMessage(merchantId: MerchantId, promotions: List[Promotion]) extends APIWithRoleMessage[OkResponse]:
  override def plan(connection: Connection, username: String): IO[OkResponse] =
    PromotionValidation.validate(promotions) match
      case Some(message) => IO.raiseError(HttpApiError.BadRequest(message))
      case None =>
        for
          merchant <- MerchantApiSupport.requireOwnedStore(connection, username, merchantId)
          products <- MerchantApiSupport.listOwnedProducts(connection, username, merchantId)
          productPromotionError = promotions.collectFirst {
            case promotion if promotion.discountType == "productAmount" =>
              val matchedProducts = products.filter(product => promotion.productIds.contains(product.id))
              if matchedProducts.isEmpty then Some("菜品优惠必须关联本店菜品")
              else matchedProducts.collectFirst {
                case product if roundMoney(product.price - promotion.discountValue) <= 0 =>
                  s"${product.name} 的优惠后价格必须大于 0 元"
              }
          }.flatten
          _ <- productPromotionError match
            case Some(message) => IO.raiseError(HttpApiError.BadRequest(message))
            case None          => IO.unit
          _ <- MerchantStoreTable.upsert(connection, username, merchant.copy(promotions = promotions.take(20)))
        yield OkResponse(ok = true)

  private def roundMoney(value: Double): Double =
    BigDecimal(value).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble
