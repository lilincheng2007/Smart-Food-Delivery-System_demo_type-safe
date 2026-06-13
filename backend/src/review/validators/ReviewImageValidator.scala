package delivery.review.validators

object ReviewImageValidator:
  final case class MerchantReviewInput(rating: Int, description: String, imageUrl: Option[String])

  def validateMerchantReviewInput(rating: Int, description: String, imageUrl: Option[String]): Either[String, MerchantReviewInput] =
    val trimmedDescription = description.trim
    val normalizedImageUrl = imageUrl.map(_.trim).filter(_.nonEmpty)
    if rating < 1 || rating > 5 then Left("商家评分必须为 1 到 5 星")
    else if trimmedDescription.isEmpty then Left("商家评价文字不能为空")
    else if normalizedImageUrl.exists(url => !isAllowedImageUrl(url)) then Left("评价图片链接必须为 http(s) 或平台上传图片")
    else Right(MerchantReviewInput(rating, trimmedDescription, normalizedImageUrl))

  def validateRiderRating(rating: Int): Either[String, Int] =
    if rating < 1 || rating > 5 then Left("骑手评分必须为 1 到 5 星")
    else Right(rating)

  def imageExtension(contentTypeLower: String, filenameHint: Option[String]): Either[String, String] =
    val byContentType = contentTypeLower.trim.toLowerCase match
      case "image/jpeg" | "image/jpg" => Some(".jpg")
      case "image/png"                => Some(".png")
      case "image/gif"                => Some(".gif")
      case "image/webp"               => Some(".webp")
      case _                          => None
    val byName = filenameHint.flatMap { name =>
      val lower = name.toLowerCase
      if lower.endsWith(".jpg") || lower.endsWith(".jpeg") then Some(".jpg")
      else if lower.endsWith(".png") then Some(".png")
      else if lower.endsWith(".gif") then Some(".gif")
      else if lower.endsWith(".webp") then Some(".webp")
      else None
    }
    byContentType.orElse(byName).toRight("仅支持 JPEG/PNG/GIF/WebP 图片")

  def isAllowedImageUrl(url: String): Boolean =
    val value = url.trim.toLowerCase
    value.isEmpty || value.startsWith("http://") || value.startsWith("https://") || value.startsWith("/api/reviews/images/")
