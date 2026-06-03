package delivery.merchant.routes

import cats.effect.IO
import delivery.merchant.api.*
import delivery.merchant.objects.*
import delivery.merchant.objects.apiTypes.*
import delivery.merchant.utils.StoreImageUploads
import delivery.shared.api.RegisteredAPIMessage
import delivery.shared.api.RegisteredAPIMessage.{api, apiWithRole}
import delivery.shared.json.ApiJsonCodecs.given
import delivery.shared.objects.ErrorBody
import delivery.shared.objects.apiTypes.OkResponse
import io.circe.generic.auto.*
import org.http4s.*
import org.http4s.circe.CirceEntityCodec.given
import org.http4s.dsl.io.*
import org.http4s.headers.`Content-Type`

import java.nio.file.Files
import java.util.regex.Pattern

object MerchantRoutes:

  private val storedImagePattern: Pattern =
    Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|jpeg|png|gif|webp)$", Pattern.CASE_INSENSITIVE)

  val storeImagePublicRoutes: HttpRoutes[IO] =
    HttpRoutes.of[IO] { case GET -> Root / imageFile =>
      if !storedImagePattern.matcher(imageFile).matches() then BadRequest(ErrorBody("非法文件名"))
      else
        val dir = StoreImageUploads.directory.normalize
        val path = dir.resolve(imageFile).normalize
        if !java.util.Objects.equals(path.getParent, dir) then BadRequest(ErrorBody("非法路径"))
        else if !Files.isRegularFile(path) then NotFound()
        else
          IO.blocking(Files.readAllBytes(path)).flatMap { bytes =>
            val media =
              if imageFile.toLowerCase.endsWith(".png") then MediaType.image.png
              else if imageFile.toLowerCase.endsWith(".gif") then MediaType.image.gif
              else if imageFile.toLowerCase.endsWith(".webp") then MediaType.image.webp
              else MediaType.image.jpeg
            Ok(bytes).map(_.putHeaders(`Content-Type`(media)))
          }
    }

  val apiMessages: List[RegisteredAPIMessage] = List(
    api[CatalogAPIMessage, CatalogResponse],
    apiWithRole[MerchantMeAPIMessage, MerchantMeResponse]("merchant"),
    apiWithRole[MerchantProfileAPIMessage, OkResponse]("merchant"),
    apiWithRole[MerchantStoreAPIMessage, String]("merchant"),
    apiWithRole[MerchantStoreDescriptionAPIMessage, OkResponse]("merchant"),
    apiWithRole[MerchantStoreImageAPIMessage, OkResponse]("merchant"),
    apiWithRole[MerchantStoreImageFileAPIMessage, String]("merchant"),
    apiWithRole[MerchantCreateProductAPIMessage, Product]("merchant"),
    apiWithRole[MerchantProductAPIMessage, Product]("merchant"),
    apiWithRole[MerchantProductDescriptionsAPIMessage, OkResponse]("merchant"),
    apiWithRole[MerchantOrderAcceptAPIMessage, OkResponse]("merchant"),
    apiWithRole[MerchantOrderRejectAPIMessage, OkResponse]("merchant"),
    apiWithRole[MerchantOrderReadyAPIMessage, OkResponse]("merchant")
  )

end MerchantRoutes
