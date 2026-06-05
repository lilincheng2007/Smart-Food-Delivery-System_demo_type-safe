package delivery.merchant.api

import cats.effect.IO
import delivery.merchant.tables.merchantstore.MerchantStoreTable
import delivery.merchant.utils.MerchantApiSupport
import delivery.shared.api.{APIWithRoleMessage, HttpApiError}
import delivery.shared.objects.MerchantId
import delivery.shared.objects.apiTypes.OkResponse

import java.sql.Connection

final case class MerchantStoreAnnouncementAPIMessage(merchantId: MerchantId, announcement: String) extends APIWithRoleMessage[OkResponse]:
  override def plan(connection: Connection, username: String): IO[OkResponse] =
    val trimmed = announcement.trim
    if trimmed.length > 180 then IO.raiseError(HttpApiError.BadRequest("店铺公告不能超过 180 个字符"))
    else
      for
        merchant <- MerchantApiSupport.requireOwnedStore(connection, username, merchantId)
        _ <- MerchantStoreTable.upsert(connection, username, merchant.copy(announcement = trimmed))
      yield OkResponse(ok = true)
