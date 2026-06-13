package delivery.review.validators

object ReviewImageValidator:
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
