package delivery.merchant.api

import cats.effect.IO
import delivery.admin.objects.apiTypes.StoreOnboardingRequestsResponse
import delivery.admin.tables.storeonboarding.StoreOnboardingRequestTable
import delivery.platform.api.APIWithRoleMessage

import java.sql.Connection

final case class MerchantStoreOnboardingRequestsAPIMessage() extends APIWithRoleMessage[StoreOnboardingRequestsResponse]:
  override def plan(connection: Connection, username: String): IO[StoreOnboardingRequestsResponse] =
    StoreOnboardingRequestTable.listByOwner(connection, username).map(StoreOnboardingRequestsResponse.apply)
