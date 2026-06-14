package delivery.merchant.objects.apiTypes

import delivery.merchant.objects.{ProductBundleGroup, ProductInventoryMode}
import delivery.domain.{ListingStatus, MerchantId}

final case class CreateProductRequest(
    merchantId: MerchantId,
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
