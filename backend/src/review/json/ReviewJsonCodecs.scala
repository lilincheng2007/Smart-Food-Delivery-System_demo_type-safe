package delivery.review.json

import delivery.platform.json.CommonJsonCodecs.given
import delivery.review.api.*
import delivery.review.objects.*
import delivery.review.objects.apiTypes.*
import io.circe.Codec
import io.circe.generic.semiauto.*

object ReviewJsonCodecs:

  given Codec[MerchantReviewsAPIMessage] = deriveCodec
  given Codec[CustomerSubmitOrderReviewAPIMessage] = deriveCodec
  given Codec[CustomerReviewVoteAPIMessage] = deriveCodec
  given Codec[MerchantReviewReplyAPIMessage] = deriveCodec
  given Codec[CustomerReviewImageFileAPIMessage] = deriveCodec

  given Codec[MerchantReview] = deriveCodec
  given Codec[RiderReview] = deriveCodec
  given Codec[ReviewSummary] = deriveCodec
  given Codec[MerchantReviewsResponse] = deriveCodec
  given Codec[RiderReviewsResponse] = deriveCodec

end ReviewJsonCodecs
