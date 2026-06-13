package delivery.merchant.api

import delivery.merchant.services.MerchantBusinessService
import cats.effect.IO
import delivery.merchant.objects.{Product, ProductBundleGroup}
import delivery.merchant.tables.catalogproduct.CatalogProductTable
import delivery.platform.api.{APIWithRoleMessage, HttpApiError}
import delivery.domain.{ListingStatus, ProductId}

import java.sql.Connection

final case class MerchantProductAPIMessage(
    productId: ProductId,
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
        existing <- CatalogProductTable.findById(connection, productId).flatMap {
          case Some(value) => IO.pure(value)
          case None        => IO.raiseError(HttpApiError.BadRequest("未找到菜品"))
        }
        _ <- MerchantBusinessService.requireOwnedStore(connection, username, existing.merchantId)
        existingProducts <- CatalogProductTable.list(connection)
        productImageUrl <- MerchantBusinessService.validateProductImageUrl(imageUrl.getOrElse(existing.imageUrl))
        productCategoryName = MerchantBusinessService.normalizeProductCategoryName(categoryName.orElse(Some(existing.categoryName)))
        normalizedInventoryMode = MerchantBusinessService.normalizeInventoryMode(inventoryMode.orElse(Some(existing.inventoryMode)))
        normalizedMaxPerOrder = MerchantBusinessService.normalizeMaxPerOrder(maxPerOrder.orElse(existing.maxPerOrder))
        normalizedBundleGroups = bundleGroups.getOrElse(existing.bundleGroups)
        _ <- MerchantBusinessService.validateBundleGroups(normalizedBundleGroups, existingProducts, existing.merchantId, Some(existing.id)) match
          case Left(message) => IO.raiseError(HttpApiError.BadRequest(message))
          case Right(()) => IO.unit
        updated = existing.copy(
          name = name.trim,
          description = description.trim,
          imageUrl = productImageUrl,
          categoryName = productCategoryName,
          price = price,
          remainingStock = if normalizedInventoryMode == "unlimited" then 999999 else remainingStock,
          listingStatus = listingStatus,
          inventoryStatus = MerchantBusinessService.inventoryStatus(remainingStock, listingStatus, normalizedInventoryMode),
          inventoryMode = normalizedInventoryMode,
          maxPerOrder = normalizedMaxPerOrder,
          bundleGroups = normalizedBundleGroups
        )
        _ <- CatalogProductTable.upsert(connection, updated)
      yield updated
