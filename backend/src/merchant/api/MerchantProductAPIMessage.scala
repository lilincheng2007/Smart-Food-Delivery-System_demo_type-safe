package delivery.merchant.api

import cats.effect.IO
import delivery.merchant.objects.Product
import delivery.merchant.tables.catalogproduct.CatalogProductTable
import delivery.shared.api.{APIWithRoleMessage, HttpApiError}
import delivery.shared.objects.{ListingStatus, ProductId}

import java.sql.Connection

final case class MerchantProductAPIMessage(
    productId: ProductId,
    name: String,
    description: String,
    imageUrl: Option[String],
    categoryName: Option[String],
    price: Double,
    remainingStock: Int,
    listingStatus: ListingStatus
) extends APIWithRoleMessage[Product]:
  override def plan(connection: Connection, username: String): IO[Product] =
    if name.trim.isEmpty || description.trim.isEmpty then IO.raiseError(HttpApiError.BadRequest("菜品名称和描述不能为空"))
    else if price < 0 || remainingStock < 0 then IO.raiseError(HttpApiError.BadRequest("价格和库存不能为负数"))
    else
      for
        existing <- CatalogProductTable.findById(connection, productId).flatMap {
          case Some(value) => IO.pure(value)
          case None        => IO.raiseError(HttpApiError.BadRequest("未找到菜品"))
        }
        _ <- MerchantAPIMessageSupport.requireOwnedStore(connection, username, existing.merchantId)
        productImageUrl <- MerchantAPIMessageSupport.validateProductImageUrl(imageUrl.getOrElse(existing.imageUrl))
        productCategoryName = MerchantAPIMessageSupport.normalizeProductCategoryName(categoryName.orElse(Some(existing.categoryName)))
        updated = existing.copy(
          name = name.trim,
          description = description.trim,
          imageUrl = productImageUrl,
          categoryName = productCategoryName,
          price = price,
          remainingStock = remainingStock,
          listingStatus = listingStatus,
          inventoryStatus = MerchantAPIMessageSupport.inventoryStatus(remainingStock, listingStatus)
        )
        _ <- CatalogProductTable.upsert(connection, updated)
      yield updated
