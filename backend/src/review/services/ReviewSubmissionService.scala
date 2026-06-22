package delivery.review.services

import cats.effect.IO
import delivery.order.tables.order.OrderTable
import delivery.review.objects.{MerchantReview, RiderReview}
import delivery.review.tables.{MerchantReviewTable, RiderReviewTable}
import delivery.review.validators.ReviewImageValidator
import delivery.rider.tables.rideraccount.RiderAccountTable
import delivery.platform.api.HttpApiError
import delivery.domain.{OrderId, OrderStatus, RiderId}
import delivery.user.tables.customerprofile.CustomerProfileTable

import java.sql.Connection
import java.util.UUID

object ReviewSubmissionService:

  final case class SubmitOrderReviewInput(
      orderId: OrderId,
      merchantRating: Int,
      merchantDescription: String,
      merchantImageUrl: Option[String],
      riderRating: Option[Int]
  )

  def submitOrderReview(connection: Connection, username: String, input: SubmitOrderReviewInput): IO[Unit] =
    for
      reviewInput <- ReviewImageValidator.validateMerchantReviewInput(input.merchantRating, input.merchantDescription, input.merchantImageUrl) match
        case Left(message) => IO.raiseError(HttpApiError.BadRequest(message))
        case Right(value)  => IO.pure(value)
      customer <- CustomerProfileTable.findByUsername(connection, username).flatMap {
        case Some(value) => IO.pure(value)
        case None        => IO.raiseError(HttpApiError.BadRequest("未找到顾客账号"))
      }
      order <- OrderTable.findById(connection, input.orderId).flatMap {
        case Some(value) => IO.pure(value)
        case None        => IO.raiseError(HttpApiError.BadRequest("未找到订单"))
      }
      _ <- validateCustomerOrderOwnership(customer.profile.id, order.customerId, order.status)
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
        rating = reviewInput.rating,
        description = reviewInput.description,
        imageUrl = reviewInput.imageUrl,
        upvotes = 0,
        downvotes = 0,
        createdAt = ""
      )
      _ <- MerchantReviewTable.create(connection, merchantReview)
      _ <- maybeSubmitRiderReview(connection, order.id, order.riderId, customer.profile.id, customer.profile.name, input.riderRating, reviewInput.rating)
    yield ()

  private def validateCustomerOrderOwnership(customerId: String, orderCustomerId: String, status: OrderStatus): IO[Unit] =
    if orderCustomerId != customerId then IO.raiseError(HttpApiError.BadRequest("无权评价该订单"))
    else if status != OrderStatus.已完成 then IO.raiseError(HttpApiError.BadRequest("订单完成后才能评价"))
    else IO.unit

  private def maybeSubmitRiderReview(
      connection: Connection,
      orderId: OrderId,
      riderId: Option[RiderId],
      customerId: String,
      customerName: String,
      riderRating: Option[Int],
      fallbackRating: Int
  ): IO[Unit] =
    riderId match
      case Some(value) =>
        val rating = riderRating.getOrElse(fallbackRating)
        for
          validRating <- ReviewImageValidator.validateRiderRating(rating) match
            case Left(message) => IO.raiseError(HttpApiError.BadRequest(message))
            case Right(value)  => IO.pure(value)
          existing <- RiderReviewTable.findByOrder(connection, orderId)
          _ <- existing match
            case Some(_) => IO.unit
            case None =>
              val riderReview = RiderReview(
                id = s"rr-${UUID.randomUUID()}",
                orderId = orderId,
                riderId = value,
                customerId = customerId,
                customerName = customerName,
                rating = validRating,
                createdAt = ""
              )
              for
                _ <- RiderReviewTable.create(connection, riderReview)
                _ <- refreshRiderProfileScore(connection, value)
              yield ()
        yield ()
      case None => IO.unit

  private def refreshRiderProfileScore(connection: Connection, riderId: RiderId): IO[Unit] =
    for
      summary <- RiderReviewTable.summaryByRider(connection, riderId)
      account <- RiderAccountTable.findByRiderId(connection, riderId)
      _ <- account match
        case Some(value) =>
          val roundedAverage = BigDecimal(summary.averageRating).setScale(1, BigDecimal.RoundingMode.HALF_UP).toDouble
          val updatedRider = value.profile.rider.copy(
            rating = roundedAverage,
            salary = value.profile.rider.salary + roundedAverage
          )
          RiderAccountTable.upsert(connection, value.copy(profile = value.profile.copy(rider = updatedRider))).void
        case None => IO.unit
    yield ()

end ReviewSubmissionService
