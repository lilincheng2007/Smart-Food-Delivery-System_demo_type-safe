package delivery.merchant.api

import cats.effect.IO
import cats.syntax.all.*
import delivery.merchant.objects.MerchantProfile
import delivery.merchant.tables.merchantaccount.MerchantAccountTable
import delivery.merchant.tables.merchantstore.MerchantStoreTable
import delivery.merchant.utils.MerchantApiSupport
import delivery.platform.api.{APIWithRoleMessage, HttpApiError}
import delivery.domain.apiTypes.OkResponse

import java.sql.Connection

final case class MerchantProfileAPIMessage(profile: MerchantProfile) extends APIWithRoleMessage[OkResponse]:
  override def plan(connection: Connection, username: String): IO[OkResponse] =
    for
      existing <- MerchantAccountTable.findByUsername(connection, username)
      account <- existing match
        case Some(value) => IO.pure(value)
        case None        => IO.raiseError(HttpApiError.NotFound(MerchantApiSupport.merchantNotFound.error))
      nextAccount = account.copy(profile = profile)
      _ <- MerchantAccountTable.upsert(connection, nextAccount)
      _ <- profile.stores.traverse_(store => MerchantStoreTable.upsert(connection, username, store.merchant))
    yield OkResponse(ok = true)
