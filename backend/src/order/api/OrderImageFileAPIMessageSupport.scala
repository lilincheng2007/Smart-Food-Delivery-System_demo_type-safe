package delivery.order.api

import cats.effect.IO
import delivery.review.api.ReviewAPIMessageSupport
import delivery.shared.api.HttpApiError
import delivery.shared.tables.storedimage.{StoredImage, StoredImageMigration, StoredImageTable}

import java.sql.Connection
import java.util.{Base64, UUID}

object OrderImageFileAPIMessageSupport:
  def upload(connection: Connection, bytesBase64: String, contentTypeLower: String, filenameHint: Option[String]): IO[String] =
    for
      bytes <- IO.blocking(Base64.getDecoder.decode(bytesBase64)).handleErrorWith(_ => IO.raiseError(HttpApiError.BadRequest("图片内容格式错误")))
      _ <- if bytes.length > 2 * 1024 * 1024 then IO.raiseError(HttpApiError.BadRequest("图片不能超过 2MB")) else IO.unit
      _ <- if bytes.isEmpty then IO.raiseError(HttpApiError.BadRequest("未收到文件内容")) else IO.unit
      ext <- IO.fromEither(ReviewAPIMessageSupport.imageExtension(contentTypeLower, filenameHint).left.map(HttpApiError.BadRequest.apply))
      storedName = s"${UUID.randomUUID()}$ext"
      publicPath = s"/api/orders/refund-images/$storedName"
      _ <- StoredImageTable.upsert(connection, StoredImage(storedName, "order", StoredImageMigration.contentTypeFor(storedName), bytes))
    yield publicPath

end OrderImageFileAPIMessageSupport
