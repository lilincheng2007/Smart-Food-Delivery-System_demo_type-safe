package delivery.merchant.objects.apiTypes

import delivery.merchant.objects.{ProductBundleGroup, ProductInventoryMode}
import delivery.domain.ListingStatus

final case class UpdateProductRequest(
    name: String,
    description: String,
    imageUrl: String,
    categoryName: String,
    price: Double,
    remainingStock: Int,
    listingStatus: ListingStatus,
    inventoryMode: Option[ProductInventoryMode] = None,
    maxPerOrder: Option[Int] = None,
    bundleGroups: Option[List[ProductBundleGroup]] = None
)
