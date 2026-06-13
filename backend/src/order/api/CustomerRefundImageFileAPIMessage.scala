package delivery.order.api

import cats.effect.IO
import delivery.platform.api.APIWithRoleMessage
import delivery.media.services.StoredImageService

import java.sql.Connection

final case class CustomerRefundImageFileAPIMessage(bytesBase64: String, contentTypeLower: String, filenameHint: Option[String]) extends APIWithRoleMessage[String]:
  override def plan(connection: Connection, username: String): IO[String] =
    for
      publicPath <- StoredImageService.saveBase64Image(connection, "order", "/api/orders/refund-images", bytesBase64, contentTypeLower, filenameHint)
    yield publicPath
