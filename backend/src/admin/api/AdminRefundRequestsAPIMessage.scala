package delivery.admin.api

import cats.effect.IO
import delivery.admin.objects.apiTypes.AdminRefundRequestsResponse
import delivery.order.tables.order.OrderTable
import delivery.platform.api.APIWithRoleMessage
import delivery.domain.RefundStatus

import java.sql.Connection

final case class AdminRefundRequestsAPIMessage() extends APIWithRoleMessage[AdminRefundRequestsResponse]:
  override def plan(connection: Connection, username: String): IO[AdminRefundRequestsResponse] =
    val adminVisibleStatuses = Set(RefundStatus.待管理员仲裁, RefundStatus.已通过, RefundStatus.已驳回)
    OrderTable.listRefundRequests(connection).map { requests =>
      AdminRefundRequestsResponse(requests.filter(order => order.refundStatus.exists(adminVisibleStatuses.contains)))
    }
