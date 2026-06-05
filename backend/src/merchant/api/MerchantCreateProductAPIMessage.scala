package delivery.merchant.api

import cats.effect.IO
import delivery.merchant.objects.Product
import delivery.merchant.tables.catalogproduct.CatalogProductTable
import delivery.merchant.tables.merchantstore.MerchantStoreTable
import delivery.shared.api.{APIWithRoleMessage, HttpApiError}
import delivery.shared.objects.{ListingStatus, MerchantId}

import java.sql.Connection

final case class MerchantCreateProductAPIMessage(
    merchantId: MerchantId,
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
        merchant <- MerchantAPIMessageSupport.requireOwnedStore(connection, username, merchantId)
        productImageUrl <- MerchantAPIMessageSupport.validateProductImageUrl(imageUrl.getOrElse(""))
        productCategoryName = MerchantAPIMessageSupport.normalizeProductCategoryName(categoryName)
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
          remainingStock = remainingStock,
          listingStatus = listingStatus,
          inventoryStatus = MerchantAPIMessageSupport.inventoryStatus(remainingStock, listingStatus),
          discountText = None
        )
        _ <- CatalogProductTable.upsert(connection, product)
        _ <- MerchantStoreTable.upsert(connection, username, merchant.copy(featuredProductIds = merchant.featuredProductIds :+ product.id))
      yield product
