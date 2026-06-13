package delivery.order.services

import cats.effect.IO
import delivery.media.services.StoredImageService

import java.sql.Connection

object OrderImageFileService:
  def upload(connection: Connection, bytesBase64: String, contentTypeLower: String, filenameHint: Option[String]): IO[String] =
    for
      publicPath <- StoredImageService.saveBase64Image(connection, "order", "/api/orders/refund-images", bytesBase64, contentTypeLower, filenameHint)
    yield publicPath

end OrderImageFileService
