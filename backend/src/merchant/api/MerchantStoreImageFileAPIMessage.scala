package delivery.merchant.api

import delivery.merchant.services.MerchantBusinessService
import cats.effect.IO
import delivery.merchant.tables.merchantstore.MerchantStoreTable
import delivery.platform.api.APIWithRoleMessage
import delivery.domain.MerchantId
import delivery.media.services.StoredImageService

import java.sql.Connection

final case class MerchantStoreImageFileAPIMessage(
    merchantId: MerchantId,
    bytesBase64: String,
    contentTypeLower: String,
    filenameHint: Option[String]
) extends APIWithRoleMessage[String]:
  override def plan(connection: Connection, username: String): IO[String] =
    for
      merchant <- MerchantBusinessService.requireOwnedStore(connection, username, merchantId)
      publicPath <- StoredImageService.saveBase64Image(connection, "merchant-store", "/api/merchant/store-images", bytesBase64, contentTypeLower, filenameHint)
      _ <- MerchantStoreTable.upsert(connection, username, merchant.copy(imageUrl = Some(publicPath)))
    yield publicPath
