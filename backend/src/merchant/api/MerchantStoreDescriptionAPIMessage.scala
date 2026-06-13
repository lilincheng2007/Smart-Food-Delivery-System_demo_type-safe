package delivery.merchant.api

import cats.effect.IO
import delivery.merchant.tables.merchantstore.MerchantStoreTable
import delivery.merchant.utils.MerchantApiSupport
import delivery.platform.api.{APIWithRoleMessage, HttpApiError}
import delivery.domain.{MerchantId}
import delivery.domain.apiTypes.{OkResponse}

import java.sql.Connection

final case class MerchantStoreDescriptionAPIMessage(merchantId: MerchantId, description: String) extends APIWithRoleMessage[OkResponse]:
  override def plan(connection: Connection, username: String): IO[OkResponse] =
    val trimmed = description.trim
    if trimmed.isEmpty then IO.raiseError(HttpApiError.BadRequest("店铺描述不能为空"))
    else if trimmed.length > 240 then IO.raiseError(HttpApiError.BadRequest("店铺描述不能超过 240 个字符"))
    else
      for
        merchant <- MerchantApiSupport.requireOwnedStore(connection, username, merchantId)
        _ <- MerchantStoreTable.upsert(connection, username, merchant.copy(description = trimmed))
      yield OkResponse(ok = true)
