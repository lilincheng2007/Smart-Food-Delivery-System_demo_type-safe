package delivery.rider.api

import delivery.rider.services.RiderDeliveryService
import cats.effect.IO
import delivery.order.tables.order.OrderTable
import delivery.review.tables.RiderReviewTable
import delivery.rider.objects.apiTypes.RiderMeResponse
import delivery.rider.tables.rideraccount.RiderAccountTable
import delivery.rider.tables.riderassignment.RiderAssignmentTable
import delivery.rider.utils.RiderApiSupport
import delivery.platform.api.{APIWithRoleMessage, HttpApiError}

import java.sql.Connection

final case class RiderMeAPIMessage() extends APIWithRoleMessage[RiderMeResponse]:
  override def plan(connection: Connection, username: String): IO[RiderMeResponse] =
    for
      account <- RiderAccountTable.findByUsername(connection, username)
      response <- account match
        case None => IO.pure(None)
        case Some(value) =>
          for
            assignedOrders <- OrderTable.listByRiderId(connection, value.profile.rider.id)
            availableOrders <- OrderTable.listAvailableUnassigned(connection)
            records <- RiderAssignmentTable.listByRider(connection, value.profile.rider.id)
            reviewSummary <- RiderReviewTable.summaryByRider(connection, value.profile.rider.id)
            reviews <- RiderReviewTable.listByRider(connection, value.profile.rider.id)
          yield
            val reviewedRider =
              if reviewSummary.reviewCount > 0 then value.profile.rider.copy(rating = reviewSummary.averageRating)
              else value.profile.rider
            val nextAccount = value.copy(profile =
              value.profile.copy(
                rider = reviewedRider,
                pendingOrders = assignedOrders.filterNot(order => RiderDeliveryService.isHistoryOrderStatus(order.status)),
                historyOrders = assignedOrders.filter(order => RiderDeliveryService.isHistoryOrderStatus(order.status))
              )
            )
            val deliveryStatuses = records.map(record => RiderDeliveryService.statusView(record, nextAccount.profile.rider.timeoutCardCount > 0))
            Some(RiderApiSupport.riderMeResponse(username, nextAccount, availableOrders, deliveryStatuses).copy(reviewSummary = reviewSummary, reviews = reviews))
      output <- response match
        case None => IO.raiseError(HttpApiError.NotFound(RiderApiSupport.riderNotFound.error))
        case Some(value) => IO.pure(value)
    yield output
