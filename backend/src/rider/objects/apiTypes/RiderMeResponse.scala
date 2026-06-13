package delivery.rider.objects.apiTypes

import delivery.order.objects.Order
import delivery.review.objects.{ReviewSummary, RiderReview}
import delivery.rider.objects.{RiderAccountPublic, RiderDeliveryStatus}
import delivery.domain.UserRole

final case class RiderMeResponse(
    username: String,
    role: UserRole,
    riderAccount: RiderAccountPublic,
    availableOrders: List[Order],
    deliveryStatuses: List[RiderDeliveryStatus] = Nil,
    reviewSummary: ReviewSummary = ReviewSummary(5.0, 0),
    reviews: List[RiderReview] = Nil
)
