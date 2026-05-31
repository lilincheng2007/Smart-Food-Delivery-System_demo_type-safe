package delivery.ai.objects

final case class AIRecommendedProduct(
    productId: String,
    productName: String,
    price: Double,
    reason: String
)

final case class AIRecommendedMerchant(
    merchantId: String,
    storeName: String,
    category: String,
    reason: String,
    products: List[AIRecommendedProduct]
)

final case class AISearchResponse(
    query: String,
    merchants: List[AIRecommendedMerchant],
    summary: String
)
