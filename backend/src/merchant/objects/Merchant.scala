package delivery.merchant.objects

final case class Merchant(
    id: String,
    storeName: String,
    category: String,
    address: String,
    phone: String,
    rating: Double,
    tags: List[String],
    featuredProductIds: List[String],
    imageUrl: Option[String]
)
