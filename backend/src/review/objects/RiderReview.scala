package delivery.review.objects

import delivery.domain.{OrderId, RiderId, UserId}

final case class RiderReview(
    id: String,
    orderId: OrderId,
    riderId: RiderId,
    customerId: UserId,
    customerName: String,
    rating: Int,
    createdAt: String
)
