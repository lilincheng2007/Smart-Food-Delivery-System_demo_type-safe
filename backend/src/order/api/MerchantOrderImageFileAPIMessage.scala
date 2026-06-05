package delivery.order.api

import cats.effect.IO
import delivery.shared.api.APIWithRoleMessage

import java.sql.Connection

final case class MerchantOrderImageFileAPIMessage(bytesBase64: String, contentTypeLower: String, filenameHint: Option[String]) extends APIWithRoleMessage[String]:
  override def plan(connection: Connection, username: String): IO[String] =
    OrderImageFileAPIMessageSupport.upload(connection, bytesBase64, contentTypeLower, filenameHint)
