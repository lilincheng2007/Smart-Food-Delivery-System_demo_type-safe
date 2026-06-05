package delivery.merchant.objects.apiTypes

import delivery.shared.objects.ListingStatus

final case class UpdateProductRequest(
    name: String,
    description: String,
    imageUrl: String,
    categoryName: String,
    price: Double,
    remainingStock: Int,
    listingStatus: ListingStatus
)
