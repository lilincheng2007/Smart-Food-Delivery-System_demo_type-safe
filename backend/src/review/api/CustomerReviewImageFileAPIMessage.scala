package delivery.review.api

import cats.effect.IO
import delivery.platform.api.APIWithRoleMessage
import delivery.media.services.StoredImageService

import java.sql.Connection

final case class CustomerReviewImageFileAPIMessage(bytesBase64: String, contentTypeLower: String, filenameHint: Option[String]) extends APIWithRoleMessage[String]:
  override def plan(connection: Connection, username: String): IO[String] =
    for
      publicPath <- StoredImageService.saveBase64Image(connection, "review", "/api/reviews/images", bytesBase64, contentTypeLower, filenameHint)
    yield publicPath
