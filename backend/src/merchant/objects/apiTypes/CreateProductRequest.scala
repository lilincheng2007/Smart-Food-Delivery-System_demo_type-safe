package delivery.merchant.objects.apiTypes

import delivery.shared.objects.{ListingStatus, MerchantId}

final case class CreateProductRequest(
    merchantId: MerchantId,
    name: String,
    description: String,
    imageUrl: String,
    categoryName: String,
    price: Double,
    remainingStock: Int,
    listingStatus: ListingStatus
)
