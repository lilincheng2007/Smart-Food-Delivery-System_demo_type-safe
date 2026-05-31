package delivery.merchant.utils

import cats.effect.IO
import delivery.merchant.objects.{Merchant, MerchantAccountPublic, MerchantMeResponse, Product}
import delivery.merchant.tables.MerchantAccountRecord
import delivery.merchant.tables.catalogproduct.CatalogProductTable
import delivery.merchant.tables.merchantstore.MerchantStoreTable
import delivery.shared.api.HttpApiError
import delivery.shared.objects.{ErrorBody, MerchantId, UserRole}

import java.sql.Connection

object MerchantApiSupport:

  def merchantNotFound: ErrorBody = ErrorBody("未找到账号")

  def merchantMeResponse(username: String, account: MerchantAccountRecord): MerchantMeResponse =
    MerchantMeResponse(
      username = username,
      role = UserRole.merchant,
      merchantAccount = MerchantAccountPublic(UserRole.merchant, account.username, account.profile)
    )

  def requireOwnedStore(connection: Connection, username: String, merchantId: MerchantId): IO[Merchant] =
    MerchantStoreTable.listByOwner(connection, username).flatMap { stores =>
      IO.fromOption(stores.find(_.id == merchantId))(HttpApiError.BadRequest("无权操作该店铺"))
    }

  def listOwnedProducts(connection: Connection, username: String, merchantId: MerchantId): IO[List[Product]] =
    for
      _ <- requireOwnedStore(connection, username, merchantId)
      products <- CatalogProductTable.list(connection)
    yield products.filter(_.merchantId == merchantId)

end MerchantApiSupport
