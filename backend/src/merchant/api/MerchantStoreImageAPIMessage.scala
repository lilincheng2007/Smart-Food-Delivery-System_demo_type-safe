package delivery.merchant.api

import delivery.merchant.services.MerchantBusinessService
import cats.effect.IO
import delivery.merchant.tables.merchantstore.MerchantStoreTable
import delivery.platform.api.APIWithRoleMessage
import delivery.domain.{MerchantId}
import delivery.domain.apiTypes.{OkResponse}

import java.sql.Connection

final case class MerchantStoreImageAPIMessage(merchantId: MerchantId, imageUrl: String) extends APIWithRoleMessage[OkResponse]:
  override def plan(connection: Connection, username: String): IO[OkResponse] =
    for
      merchant <- MerchantBusinessService.requireOwnedStore(connection, username, merchantId)
      imageOpt <- MerchantBusinessService.validateImageUrl(imageUrl)
      _ <- MerchantStoreTable.upsert(connection, username, merchant.copy(imageUrl = imageOpt))
    yield OkResponse(ok = true)
