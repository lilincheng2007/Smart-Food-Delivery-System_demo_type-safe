package delivery.review.api

import delivery.review.validators.ReviewImageValidator
import cats.effect.IO
import delivery.order.tables.order.OrderTable
import delivery.review.objects.{MerchantReview, RiderReview}
import delivery.review.tables.{MerchantReviewTable, RiderReviewTable}
import delivery.rider.tables.rideraccount.RiderAccountTable
import delivery.platform.api.{APIWithRoleMessage, HttpApiError}
import delivery.domain.{OrderId, OrderStatus}
import delivery.domain.apiTypes.OkResponse
import delivery.user.tables.customerprofile.CustomerProfileTable

import java.sql.Connection
import java.util.UUID

final case class CustomerSubmitOrderReviewAPIMessage(
    orderId: OrderId,
    merchantRating: Int,
    merchantDescription: String,
    merchantImageUrl: Option[String],
    riderRating: Option[Int]
) extends APIWithRoleMessage[OkResponse]:
  override def plan(connection: Connection, username: String): IO[OkResponse] =
    val description = merchantDescription.trim
    for
      customer <- CustomerProfileTable.findByUsername(connection, username).flatMap {
        case Some(value) => IO.pure(value)
        case None        => IO.raiseError(HttpApiError.BadRequest("未找到顾客账号"))
      }
      order <- OrderTable.findById(connection, orderId).flatMap {
        case Some(value) => IO.pure(value)
        case None        => IO.raiseError(HttpApiError.BadRequest("未找到订单"))
      }
      _ <-
        if order.customerId != customer.profile.id then IO.raiseError(HttpApiError.BadRequest("无权评价该订单"))
        else if order.status != OrderStatus.已完成 then IO.raiseError(HttpApiError.BadRequest("订单完成后才能评价"))
        else if merchantRating < 1 || merchantRating > 5 then IO.raiseError(HttpApiError.BadRequest("商家评分必须为 1 到 5 星"))
        else if description.isEmpty then IO.raiseError(HttpApiError.BadRequest("商家评价文字不能为空"))
        else if merchantImageUrl.exists(url => !ReviewImageValidator.isAllowedImageUrl(url)) then IO.raiseError(HttpApiError.BadRequest("评价图片链接必须为 http(s) 或平台上传图片"))
        else IO.unit
      _ <- MerchantReviewTable.findByOrder(connection, order.id).flatMap {
        case Some(_) => IO.raiseError(HttpApiError.Conflict("该订单已评价商家"))
        case None    => IO.unit
      }
      merchantReview = MerchantReview(
        id = s"mr-${UUID.randomUUID()}",
        orderId = order.id,
        merchantId = order.merchantId,
        customerId = customer.profile.id,
        customerName = customer.profile.name,
        rating = merchantRating,
        description = description,
        imageUrl = merchantImageUrl.map(_.trim).filter(_.nonEmpty),
        upvotes = 0,
        downvotes = 0,
        createdAt = ""
      )
      _ <- MerchantReviewTable.create(connection, merchantReview)
      _ <- order.riderId match
        case Some(riderId) =>
          val rating = riderRating.getOrElse(merchantRating)
          if rating < 1 || rating > 5 then IO.raiseError(HttpApiError.BadRequest("骑手评分必须为 1 到 5 星"))
          else
            for
              existing <- RiderReviewTable.findByOrder(connection, order.id)
              _ <- existing match
                case Some(_) => IO.unit
                case None =>
                  val riderReview = RiderReview(
                    id = s"rr-${UUID.randomUUID()}",
                    orderId = order.id,
                    riderId = riderId,
                    customerId = customer.profile.id,
                    customerName = customer.profile.name,
                    rating = rating,
                    createdAt = ""
                  )
                  for
                    _ <- RiderReviewTable.create(connection, riderReview)
                    summary <- RiderReviewTable.summaryByRider(connection, riderId)
                    account <- RiderAccountTable.findByRiderId(connection, riderId)
                    _ <- account match
                      case Some(value) =>
                        val roundedAverage = BigDecimal(summary.averageRating).setScale(1, BigDecimal.RoundingMode.HALF_UP).toDouble
                        val updatedRider = value.profile.rider.copy(
                          rating = roundedAverage,
                          salary = value.profile.rider.salary + roundedAverage
                        )
                        RiderAccountTable.upsert(connection, value.copy(profile = value.profile.copy(rider = updatedRider)))
                      case None => IO.unit
                  yield ()
            yield ()
        case None => IO.unit
    yield OkResponse(ok = true)
