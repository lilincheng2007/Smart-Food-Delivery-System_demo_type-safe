package delivery.media.services

import cats.effect.IO
import delivery.media.objects.StoredImage
import delivery.media.tables.storedimage.StoredImageTable
import delivery.media.validators.ImageUploadValidator
import delivery.platform.api.HttpApiError

import java.sql.Connection
import java.util.UUID

object StoredImageService:
  def saveBase64Image(
      connection: Connection,
      scope: String,
      publicPathPrefix: String,
      bytesBase64: String,
      contentTypeLower: String,
      filenameHint: Option[String]
  ): IO[String] =
    for
      bytes <- ImageUploadValidator.decodeBase64(bytesBase64)
      _ <- ImageUploadValidator.validateBytes(bytes)
      ext <- IO.fromEither(ImageUploadValidator.extension(contentTypeLower, filenameHint).left.map(HttpApiError.BadRequest.apply))
      storedName = s"${UUID.randomUUID()}$ext"
      publicPath = s"$publicPathPrefix/$storedName"
      _ <- StoredImageTable.upsert(connection, StoredImage(storedName, scope, ImageUploadValidator.contentTypeForName(storedName), bytes))
    yield publicPath

end StoredImageService
