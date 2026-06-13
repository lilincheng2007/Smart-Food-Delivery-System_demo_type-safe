package delivery.merchant.api

import delivery.merchant.services.MerchantBusinessService
import cats.effect.IO
import delivery.merchant.objects.{Product, ProductBundleGroup}
import delivery.merchant.tables.catalogproduct.CatalogProductTable
import delivery.merchant.tables.merchantstore.MerchantStoreTable
import delivery.platform.api.{APIWithRoleMessage, HttpApiError}
import delivery.domain.{ListingStatus, MerchantId}

import java.sql.Connection

final case class MerchantCreateProductAPIMessage(
    merchantId: MerchantId,
    name: String,
    description: String,
    imageUrl: Option[String],
    categoryName: Option[String],
    price: Double,
    remainingStock: Int,
    listingStatus: ListingStatus,
    inventoryMode: Option[String] = None,
    maxPerOrder: Option[Int] = None,
    bundleGroups: Option[List[ProductBundleGroup]] = None
) extends APIWithRoleMessage[Product]:
  override def plan(connection: Connection, username: String): IO[Product] =
    if name.trim.isEmpty then IO.raiseError(HttpApiError.BadRequest("菜品名称不能为空"))
    else if price < 0 || remainingStock < 0 then IO.raiseError(HttpApiError.BadRequest("价格和库存不能为负数"))
    else
      for
        merchant <- MerchantBusinessService.requireOwnedStore(connection, username, merchantId)
        existingProducts <- CatalogProductTable.list(connection)
        productImageUrl <- MerchantBusinessService.validateProductImageUrl(imageUrl.getOrElse(""))
        productCategoryName = MerchantBusinessService.normalizeProductCategoryName(categoryName)
        normalizedInventoryMode = MerchantBusinessService.normalizeInventoryMode(inventoryMode)
        normalizedMaxPerOrder = MerchantBusinessService.normalizeMaxPerOrder(maxPerOrder)
        normalizedBundleGroups = bundleGroups.getOrElse(Nil)
        _ <- MerchantBusinessService.validateBundleGroups(normalizedBundleGroups, existingProducts, merchantId, None) match
          case Left(message) => IO.raiseError(HttpApiError.BadRequest(message))
          case Right(()) => IO.unit
        nowMillis <- IO.realTime.map(_.toMillis)
        product = Product(
          id = s"p-local-$nowMillis",
          merchantId = merchantId,
          name = name.trim,
          price = price,
          description = description.trim,
          imageUrl = productImageUrl,
          categoryName = productCategoryName,
          monthlySales = 0,
          remainingStock = if normalizedInventoryMode == "unlimited" then 999999 else remainingStock,
          listingStatus = listingStatus,
          inventoryStatus = MerchantBusinessService.inventoryStatus(remainingStock, listingStatus, normalizedInventoryMode),
          inventoryMode = normalizedInventoryMode,
          maxPerOrder = normalizedMaxPerOrder,
          discountText = None,
          bundleGroups = normalizedBundleGroups
        )
        _ <- CatalogProductTable.upsert(connection, product)
        _ <- MerchantStoreTable.upsert(connection, username, merchant.copy(featuredProductIds = merchant.featuredProductIds :+ product.id))
      yield product
