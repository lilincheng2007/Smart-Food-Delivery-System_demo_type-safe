package delivery.merchant.routes

import cats.effect.IO
import cats.effect.kernel.Ref
import cats.syntax.all.*
import delivery.merchant.api.*
import delivery.merchant.objects.{CreateProductRequest, CreateStoreRequest, MerchantProfileBody, UpdateProductRequest, UpdateStoreImageRequest}
import delivery.merchant.utils.{MerchantApiSupport, StoreImageUploads}
import delivery.shared.http.AuthHttp
import delivery.shared.json.ApiJsonCodecs.given
import delivery.shared.objects.{DeliveryState, ErrorBody}
import fs2.Chunk
import org.http4s.*
import org.http4s.circe.CirceEntityCodec.given
import org.http4s.dsl.io.*
import org.http4s.headers.`Content-Type`
import org.http4s.multipart.{Multipart, Part}

import java.nio.file.Files
import java.util.regex.Pattern

object MerchantRoutes:

  given EntityDecoder[IO, Multipart[IO]] = EntityDecoder.multipart[IO]

  private val storedImagePattern: Pattern =
    Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|jpeg|png|gif|webp)$", Pattern.CASE_INSENSITIVE)

  /** 挂载在 `Router("/api/merchant/store-images" -> ...)` 下，避免嵌套在 `/api/merchant` 时 path 匹配不到。 */
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

  private def pickFilePart(mp: Multipart[IO]): Option[Part[IO]] =
    mp.parts.find(_.name.exists(_ == "file")).orElse(mp.parts.find(_.filename.exists(_.trim.nonEmpty)))

  def routes(ref: Ref[IO, DeliveryState], persist: DeliveryState => IO[Unit]): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case GET -> Root / "catalog" =>
        ref.get.flatMap(state => CatalogApi.plan(CatalogApi.CatalogQuery(state))).flatMap(Ok(_))

      case req @ GET -> Root / "me" =>
        AuthHttp.requireRole(req, "merchant") { username =>
          ref.get.flatMap(state => MerchantMeApi.plan(MerchantMeApi.MerchantMeQuery(state, username))).flatMap {
            case None => NotFound(MerchantApiSupport.merchantNotFound)
            case Some(output) => Ok(output)
          }
        }

      case req @ PUT -> Root / "me" / "profile" =>
        AuthHttp.requireRole(req, "merchant") { username =>
          for
            body <- req.as[MerchantProfileBody]
            current <- ref.get
            response <- MerchantProfileApi
              .plan(MerchantProfileApi.MerchantProfileCommand(current, username, body))
              .flatMap {
                case Left(msg) => BadRequest(ErrorBody(msg))
                case Right(output) => ref.set(output.nextState) *> persist(output.nextState) *> Ok(output.response)
              }
          yield response
        }

      case req @ POST -> Root / "me" / "stores" =>
        AuthHttp.requireRole(req, "merchant") { username =>
          for
            body <- req.as[CreateStoreRequest]
            current <- ref.get
            response <- MerchantStoreApi.plan(MerchantStoreApi.MerchantStoreCommand(current, username, body)).flatMap {
              case Left(msg) => BadRequest(ErrorBody(msg))
              case Right(output) => ref.set(output.nextState) *> persist(output.nextState) *> Ok(output.response)
            }
          yield response
        }

      case req @ PUT -> Root / "me" / "stores" / merchantId / "image" =>
        AuthHttp.requireRole(req, "merchant") { username =>
          for
            body <- req.as[UpdateStoreImageRequest]
            current <- ref.get
            response <- MerchantStoreImageApi
              .plan(MerchantStoreImageApi.MerchantStoreImageCommand(current, username, merchantId, body))
              .flatMap {
                case Left(msg) => BadRequest(ErrorBody(msg))
                case Right(output) => ref.set(output.nextState) *> persist(output.nextState) *> Ok(output.response)
              }
          yield response
        }

      case req @ POST -> Root / "me" / "stores" / merchantId / "image-file" =>
        AuthHttp.requireRole(req, "merchant") { username =>
          req.as[Multipart[IO]].flatMap { mp =>
            pickFilePart(mp) match
              case None => BadRequest(ErrorBody("请使用表单字段名 file 上传图片"))
              case Some(part) =>
                part.body.compile.to(Chunk).flatMap { ch =>
                  val bytes = ch.toArray
                  val ctLower =
                    part.headers.get[`Content-Type`].fold("") { h =>
                      val m = h.mediaType
                      s"${m.mainType}/${m.subType}".toLowerCase
                    }
                  ref.get.flatMap { current =>
                    MerchantStoreImageFileApi
                      .plan(
                        MerchantStoreImageFileApi.MerchantStoreImageFileCommand(
                          current,
                          username,
                          merchantId,
                          bytes,
                          ctLower,
                          part.filename
                        )
                      )
                      .flatMap {
                        case Left(msg) => BadRequest(ErrorBody(msg))
                        case Right(output) => ref.set(output.nextState) *> persist(output.nextState) *> Ok(output.response)
                      }
                  }
                }
          }
        }

      case req @ POST -> Root / "me" / "products" =>
        AuthHttp.requireRole(req, "merchant") { username =>
          for
            body <- req.as[CreateProductRequest]
            current <- ref.get
            response <- MerchantCreateProductApi.plan(MerchantCreateProductApi.MerchantCreateProductCommand(current, username, body)).flatMap {
              case Left(msg) => BadRequest(ErrorBody(msg))
              case Right(output) => ref.set(output.nextState) *> persist(output.nextState) *> Ok(output.response)
            }
          yield response
        }

      case req @ PUT -> Root / "me" / "products" / productId =>
        AuthHttp.requireRole(req, "merchant") { username =>
          for
            body <- req.as[UpdateProductRequest]
            current <- ref.get
            response <- MerchantProductApi.plan(MerchantProductApi.MerchantProductCommand(current, username, productId, body)).flatMap {
              case Left(msg) => BadRequest(ErrorBody(msg))
              case Right(output) => ref.set(output.nextState) *> persist(output.nextState) *> Ok(output.response)
            }
          yield response
        }

      case req @ POST -> Root / "me" / "orders" / orderId / "finish" =>
        AuthHttp.requireRole(req, "merchant") { username =>
          for
            current <- ref.get
            response <- MerchantOrderReadyApi.plan(MerchantOrderReadyApi.MerchantOrderReadyCommand(current, username, orderId)).flatMap {
              case Left(msg) => BadRequest(ErrorBody(msg))
              case Right(output) => ref.set(output.nextState) *> persist(output.nextState) *> Ok(output.response)
            }
          yield response
        }

      case req @ POST -> Root / "me" / "orders" / orderId / "ready" =>
        AuthHttp.requireRole(req, "merchant") { username =>
          for
            current <- ref.get
            response <- MerchantOrderReadyApi.plan(MerchantOrderReadyApi.MerchantOrderReadyCommand(current, username, orderId)).flatMap {
              case Left(msg) => BadRequest(ErrorBody(msg))
              case Right(output) => ref.set(output.nextState) *> persist(output.nextState) *> Ok(output.response)
            }
          yield response
        }
    }

end MerchantRoutes
