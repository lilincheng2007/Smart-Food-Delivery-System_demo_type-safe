package delivery.admin.api

import cats.effect.IO
import delivery.admin.objects.apiTypes.StoreOnboardingRequestsResponse
import delivery.admin.tables.storeonboarding.StoreOnboardingRequestTable
import delivery.platform.api.APIWithRoleMessage

import java.sql.Connection

final case class AdminStoreOnboardingRequestsAPIMessage() extends APIWithRoleMessage[StoreOnboardingRequestsResponse]:
  override def plan(connection: Connection, username: String): IO[StoreOnboardingRequestsResponse] =
    StoreOnboardingRequestTable.list(connection).map(StoreOnboardingRequestsResponse.apply)
