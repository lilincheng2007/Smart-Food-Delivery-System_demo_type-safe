package delivery.merchant.objects

import delivery.shared.objects.ProductId

final case class ProductDescriptionPatch(productId: ProductId, description: String)
