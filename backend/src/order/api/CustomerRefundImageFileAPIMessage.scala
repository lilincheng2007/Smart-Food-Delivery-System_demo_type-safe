package delivery.order.api

import cats.effect.IO
import delivery.order.services.OrderImageFileService
import delivery.platform.api.APIWithRoleMessage

import java.sql.Connection

final case class CustomerRefundImageFileAPIMessage(bytesBase64: String, contentTypeLower: String, filenameHint: Option[String]) extends APIWithRoleMessage[String]:
  override def plan(connection: Connection, username: String): IO[String] =
    OrderImageFileService.upload(connection, bytesBase64, contentTypeLower, filenameHint)
