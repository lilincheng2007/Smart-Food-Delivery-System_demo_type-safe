package delivery.merchant.objects

import delivery.shared.objects.{MerchantCategory, MerchantId, ProductId, Promotion}

final case class Merchant(
    id: MerchantId,
    storeName: String,
    category: MerchantCategory,
    address: String,
    phone: String,
    rating: Double,
    tags: List[String],
    featuredProductIds: List[ProductId],
    imageUrl: Option[String],
    description: String = "",
    announcement: String = "",
    promotions: List[Promotion] = Nil
)
