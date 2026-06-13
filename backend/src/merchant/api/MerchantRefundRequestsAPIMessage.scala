package delivery.merchant.api

import delivery.merchant.services.MerchantBusinessService
import cats.effect.IO
import cats.syntax.all.*
import delivery.merchant.objects.apiTypes.MerchantRefundRequestsResponse
import delivery.order.api.RefundWorkflowSupport
import delivery.order.objects.Order
import delivery.order.tables.order.OrderTable
import delivery.platform.api.{APIWithRoleMessage, HttpApiError}
import delivery.domain.RefundStatus

import java.sql.Connection
import java.time.{Duration, Instant}

final case class MerchantRefundRequestsAPIMessage() extends APIWithRoleMessage[MerchantRefundRequestsResponse]:
  override def plan(connection: Connection, username: String): IO[MerchantRefundRequestsResponse] =
    for
      stores <- MerchantBusinessService.listOwnedStores(connection, username)
      orders <- OrderTable.listByMerchantIds(connection, stores.map(_.id))
      now <- IO.realTimeInstant
      _ <- autoAcceptOverdue(connection, orders, now)
      refreshed <- OrderTable.listByMerchantIds(connection, stores.map(_.id))
      refundRequests = refreshed.filter(_.refundStatus.nonEmpty)
    yield MerchantRefundRequestsResponse(refundRequests)

  private def autoAcceptOverdue(connection: Connection, orders: List[Order], now: Instant): IO[Unit] =
    orders
      .filter(order => RefundWorkflowSupport.isMerchantPending(order.refundStatus) && isOverdue(order.refundRequestedAt, now))
      .traverse_(order =>
        RefundWorkflowSupport.acceptRefund(
          connection,
          order,
          merchantReason = Some("商家超过30分钟未处理，系统自动通过退款"),
          adminReason = None,
          markMerchantReviewed = true
        )
      )

  private def isOverdue(requestedAt: Option[String], now: Instant): Boolean =
    requestedAt
      .flatMap(value => scala.util.Try(Instant.parse(value)).toOption)
      .exists(value => Duration.between(value, now).toMinutes >= 30)
