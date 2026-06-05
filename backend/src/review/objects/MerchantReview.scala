package delivery.review.objects

import delivery.shared.objects.{MerchantId, OrderId, UserId}

final case class MerchantReview(
    id: String,
    orderId: OrderId,
    merchantId: MerchantId,
    customerId: UserId,
    customerName: String,
    rating: Int,
    description: String,
    imageUrl: Option[String],
    upvotes: Int,
    downvotes: Int,
    createdAt: String,
    merchantReply: Option[String] = None,
    merchantReplyAt: Option[String] = None,
    orderItemNames: List[String] = Nil
)
