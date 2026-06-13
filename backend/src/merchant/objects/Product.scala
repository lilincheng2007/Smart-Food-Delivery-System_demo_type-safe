package delivery.merchant.objects

import delivery.domain.{InventoryStatus, ListingStatus, MerchantId, ProductId}

final case class ProductBundleOption(productId: ProductId, recommended: Boolean = false, extraPrice: Double = 0, customExtraPrice: Boolean = false)

final case class ProductBundleGroup(
    id: String,
    name: String,
    quantity: Int,
    selectionType: String = "repeatable",
    includedPrice: Double = 0,
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
    bundleGroups: List[ProductBundleGroup] = Nil,
    inventoryMode: String = "finite",
    maxPerOrder: Option[Int] = None
)
