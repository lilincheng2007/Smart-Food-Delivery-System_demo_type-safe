package delivery.merchant.objects.apiTypes

import delivery.merchant.objects.{Merchant, Product}
import delivery.domain.Promotion

final case class CatalogResponse(merchants: List[Merchant], products: List[Product], platformPromotions: List[Promotion] = Nil)
