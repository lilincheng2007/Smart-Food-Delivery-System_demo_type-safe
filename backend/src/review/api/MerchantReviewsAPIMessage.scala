package delivery.review.api

import cats.effect.IO
import delivery.review.objects.apiTypes.MerchantReviewsResponse
import delivery.review.tables.MerchantReviewTable
import delivery.platform.api.APIMessage
import delivery.domain.MerchantId

import java.sql.Connection

final case class MerchantReviewsAPIMessage(merchantId: MerchantId) extends APIMessage[MerchantReviewsResponse]:
  override def plan(connection: Connection): IO[MerchantReviewsResponse] =
    for
      summary <- MerchantReviewTable.summaryByMerchant(connection, merchantId)
      reviews <- MerchantReviewTable.listByMerchant(connection, merchantId)
    yield MerchantReviewsResponse(summary, reviews)
