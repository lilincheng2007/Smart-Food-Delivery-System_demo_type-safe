package delivery.merchant.objects.apiTypes

import delivery.merchant.objects.{Merchant, Product}
import delivery.shared.objects.Promotion

final case class CatalogResponse(merchants: List[Merchant], products: List[Product], platformPromotions: List[Promotion] = Nil)
