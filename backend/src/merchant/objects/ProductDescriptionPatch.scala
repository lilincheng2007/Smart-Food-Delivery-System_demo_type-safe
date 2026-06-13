package delivery.merchant.objects

import delivery.domain.ProductId

final case class ProductDescriptionPatch(productId: ProductId, description: String)
