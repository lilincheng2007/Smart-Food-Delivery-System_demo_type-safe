package delivery.merchant.objects

import delivery.shared.objects.{InventoryStatus, ListingStatus, MerchantId, ProductId}

final case class ProductBundleOption(productId: ProductId, recommended: Boolean = false)

final case class ProductBundleGroup(
    id: String,
    name: String,
    quantity: Int,
    options: List[ProductBundleOption] = Nil
)

final case class Product(
    id: ProductId,
    merchantId: MerchantId,
    name: String,
    price: Double,
    description: String,
    imageUrl: String,
    monthlySales: Int,
    remainingStock: Int,
    listingStatus: ListingStatus,
    inventoryStatus: InventoryStatus,
    discountText: Option[String] = None,
    categoryName: String = "默认分类",
    bundleGroups: List[ProductBundleGroup] = Nil
)
