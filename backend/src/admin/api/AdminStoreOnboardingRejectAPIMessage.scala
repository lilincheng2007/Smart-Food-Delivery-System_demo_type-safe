package delivery.admin.api

import cats.effect.IO
import delivery.admin.tables.storeonboarding.StoreOnboardingRequestTable
import delivery.platform.api.{APIWithRoleMessage, HttpApiError}
import delivery.domain.apiTypes.OkResponse

import java.sql.Connection

final case class AdminStoreOnboardingRejectAPIMessage(requestId: String, rejectionReason: String) extends APIWithRoleMessage[OkResponse]:
  override def plan(connection: Connection, username: String): IO[OkResponse] =
    val id = requestId.trim
    val reason = rejectionReason.trim
    if id.isEmpty then IO.raiseError(HttpApiError.BadRequest("申请 ID 不能为空"))
    else if reason.isEmpty then IO.raiseError(HttpApiError.BadRequest("驳回原因不能为空"))
    else
      for
        request <- StoreOnboardingRequestTable.findPending(connection, id)
        _ <- IO.fromOption(request)(HttpApiError.NotFound("未找到待审核的店铺申请"))
        _ <- StoreOnboardingRequestTable.reject(connection, id, username, reason)
      yield OkResponse(ok = true)
