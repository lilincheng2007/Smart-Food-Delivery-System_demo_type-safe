package delivery.merchant.api

import cats.effect.IO
import delivery.merchant.objects.Product
import delivery.merchant.tables.catalogproduct.CatalogProductTable
import delivery.shared.api.{APIWithRoleMessage, HttpApiError}
import delivery.shared.objects.ProductId
import delivery.shared.tables.storedimage.{StoredImage, StoredImageMigration, StoredImageTable}

import java.sql.Connection
import java.util.Base64
import java.util.UUID

final case class MerchantProductImageFileAPIMessage(
    productId: ProductId,
    bytesBase64: String,
    contentTypeLower: String,
    filenameHint: Option[String]
) extends APIWithRoleMessage[Product]:
  override def plan(connection: Connection, username: String): IO[Product] =
    for
      bytes <- IO.blocking(Base64.getDecoder.decode(bytesBase64)).handleErrorWith(_ => IO.raiseError(HttpApiError.BadRequest("图片内容格式错误")))
      _ <- if bytes.length > 2 * 1024 * 1024 then IO.raiseError(HttpApiError.BadRequest("图片不能超过 2MB")) else IO.unit
      _ <- if bytes.isEmpty then IO.raiseError(HttpApiError.BadRequest("未收到文件内容")) else IO.unit
      ext <- IO.fromEither(MerchantAPIMessageSupport.storeImageExtension(contentTypeLower, filenameHint).left.map(HttpApiError.BadRequest.apply))
      existing <- CatalogProductTable.findById(connection, productId).flatMap {
        case Some(value) => IO.pure(value)
        case None        => IO.raiseError(HttpApiError.BadRequest("未找到菜品"))
      }
      _ <- MerchantAPIMessageSupport.requireOwnedStore(connection, username, existing.merchantId)
      storedName = s"${UUID.randomUUID()}$ext"
      publicPath = s"/api/merchant/product-images/$storedName"
      _ <- StoredImageTable.upsert(connection, StoredImage(storedName, "merchant-product", StoredImageMigration.contentTypeFor(storedName), bytes))
      updated = existing.copy(imageUrl = publicPath)
      _ <- CatalogProductTable.upsert(connection, updated)
    yield updated
