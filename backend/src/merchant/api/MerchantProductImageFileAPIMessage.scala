package delivery.merchant.api

import delivery.merchant.services.MerchantBusinessService
import cats.effect.IO
import delivery.merchant.objects.Product
import delivery.merchant.tables.catalogproduct.CatalogProductTable
import delivery.platform.api.{APIWithRoleMessage, HttpApiError}
import delivery.domain.ProductId
import delivery.media.services.StoredImageService

import java.sql.Connection

final case class MerchantProductImageFileAPIMessage(
    productId: ProductId,
    bytesBase64: String,
    contentTypeLower: String,
    filenameHint: Option[String]
) extends APIWithRoleMessage[Product]:
  override def plan(connection: Connection, username: String): IO[Product] =
    for
      existing <- CatalogProductTable.findById(connection, productId).flatMap {
        case Some(value) => IO.pure(value)
        case None        => IO.raiseError(HttpApiError.BadRequest("未找到菜品"))
      }
      _ <- MerchantBusinessService.requireOwnedStore(connection, username, existing.merchantId)
      publicPath <- StoredImageService.saveBase64Image(connection, "merchant-product", "/api/merchant/product-images", bytesBase64, contentTypeLower, filenameHint)
      updated = existing.copy(imageUrl = publicPath)
      _ <- CatalogProductTable.upsert(connection, updated)
    yield updated
