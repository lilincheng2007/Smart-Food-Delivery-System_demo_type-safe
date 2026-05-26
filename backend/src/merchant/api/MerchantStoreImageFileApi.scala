package delivery.merchant.api

import cats.effect.IO
import delivery.merchant.tables.MerchantDomainOps
import delivery.merchant.utils.StoreImageUploads
import delivery.shared.api.ApiPlan
import delivery.shared.objects.DeliveryState
import delivery.shared.db.DeliveryStateOps
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.nio.file.Files
import java.util.UUID

object MerchantStoreImageFileApi extends ApiPlan[
  MerchantStoreImageFileApi.MerchantStoreImageFileCommand,
  Either[String, MerchantStoreImageFileApi.MerchantStoreImageFileSuccess]
]:

  private val maxBytes = 2 * 1024 * 1024

  final case class MerchantStoreImageFileCommand(
      state: DeliveryState,
      username: String,
      merchantId: String,
      bytes: Array[Byte],
      contentTypeLower: String,
      filenameHint: Option[String]
  )

  final case class MerchantStoreImageFileSuccess(nextState: DeliveryState, response: String)

  private val logger = Slf4jLogger.getLogger[IO]

  override val name: String = "MerchantStoreImageFileApi"

  private def extensionFrom(contentTypeLower: String, filenameHint: Option[String]): Either[String, String] =
    val fromCt =
      if contentTypeLower.contains("jpeg") || contentTypeLower.contains("jpg") then Some(".jpg")
      else if contentTypeLower.contains("png") then Some(".png")
      else if contentTypeLower.contains("gif") then Some(".gif")
      else if contentTypeLower.contains("webp") then Some(".webp")
      else None
    fromCt.orElse {
      filenameHint.flatMap { n =>
        val lower = n.toLowerCase
        if lower.endsWith(".jpg") || lower.endsWith(".jpeg") then Some(".jpg")
        else if lower.endsWith(".png") then Some(".png")
        else if lower.endsWith(".gif") then Some(".gif")
        else if lower.endsWith(".webp") then Some(".webp")
        else None
      }
    }.toRight("仅支持 JPEG、PNG、GIF、WebP 图片")

  override def plan(input: MerchantStoreImageFileCommand): IO[Either[String, MerchantStoreImageFileSuccess]] =
    for
      _ <- logger.info(s"$name started, username=${input.username}, merchantId=${input.merchantId}, bytes=${input.bytes.length}")
      result <-
        if input.bytes.length > maxBytes then IO.pure(Left("图片不能超过 2MB"))
        else if input.bytes.isEmpty then IO.pure(Left("未收到文件内容"))
        else
          extensionFrom(input.contentTypeLower, input.filenameHint) match
            case Left(msg) => IO.pure(Left(msg))
            case Right(ext) =>
              val id = UUID.randomUUID().toString
              val storedName = s"$id$ext"
              val path = StoreImageUploads.directory.resolve(storedName)
              val publicPath = s"/api/merchant/store-images/$storedName"
              IO.blocking(Files.write(path, input.bytes)) *>
                IO.pure(
                  MerchantDomainOps
                    .updateStoreImage(input.state.merchant, input.username, input.merchantId, publicPath)
                    .map(nextMerchant =>
                      MerchantStoreImageFileSuccess(
                        DeliveryStateOps.withMerchantState(input.state, nextMerchant),
                        publicPath
                      )
                    )
                )
      _ <- logger.info(s"$name finished, success=${result.isRight}")
    yield result

end MerchantStoreImageFileApi
