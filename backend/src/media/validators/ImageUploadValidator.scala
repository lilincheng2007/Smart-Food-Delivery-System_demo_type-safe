package delivery.media.validators

import cats.effect.IO
import delivery.platform.api.HttpApiError

import java.util.Base64

object ImageUploadValidator:
  private val maxImageBytes = 2 * 1024 * 1024

  def decodeBase64(bytesBase64: String): IO[Array[Byte]] =
    IO.blocking(Base64.getDecoder.decode(bytesBase64)).handleErrorWith(_ => IO.raiseError(HttpApiError.BadRequest("图片内容格式错误")))

  def validateBytes(bytes: Array[Byte]): IO[Unit] =
    if bytes.length > maxImageBytes then IO.raiseError(HttpApiError.BadRequest("图片不能超过 2MB"))
    else if bytes.isEmpty then IO.raiseError(HttpApiError.BadRequest("未收到文件内容"))
    else IO.unit

  def extension(contentTypeLower: String, filenameHint: Option[String]): Either[String, String] =
    val normalizedContentType = contentTypeLower.trim.toLowerCase
    val byContentType =
      if normalizedContentType.contains("jpeg") || normalizedContentType.contains("jpg") then Some(".jpg")
      else if normalizedContentType.contains("png") then Some(".png")
      else if normalizedContentType.contains("gif") then Some(".gif")
      else if normalizedContentType.contains("webp") then Some(".webp")
      else None
    val byName = filenameHint.flatMap { name =>
      val lower = name.toLowerCase
      if lower.endsWith(".jpg") || lower.endsWith(".jpeg") then Some(".jpg")
      else if lower.endsWith(".png") then Some(".png")
      else if lower.endsWith(".gif") then Some(".gif")
      else if lower.endsWith(".webp") then Some(".webp")
      else None
    }
    byContentType.orElse(byName).toRight("仅支持 JPEG、PNG、GIF、WebP 图片")

  def contentTypeForName(fileName: String): String =
    val lower = fileName.toLowerCase
    if lower.endsWith(".png") then "image/png"
    else if lower.endsWith(".gif") then "image/gif"
    else if lower.endsWith(".webp") then "image/webp"
    else "image/jpeg"

end ImageUploadValidator
