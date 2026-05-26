package delivery.merchant.api

import cats.effect.IO
import delivery.merchant.objects.*
import delivery.merchant.utils.MerchantApiSupport
import delivery.shared.api.{APIMessage, APIWithRoleMessage, HttpApiError}
import delivery.shared.db.DeliveryStateStore
import delivery.shared.objects.OkResponse

import java.util.Base64
import javax.sql.DataSource

final case class CatalogAPIMessage() extends APIMessage[CatalogResponse]:
  override def plan(ds: DataSource): IO[CatalogResponse] =
    DeliveryStateStore.load(ds).flatMap(state => CatalogApi.plan(CatalogApi.CatalogQuery(state)))

final case class MerchantMeAPIMessage() extends APIWithRoleMessage[MerchantMeResponse]:
  override def plan(ds: DataSource, username: String): IO[MerchantMeResponse] =
    for
      state <- DeliveryStateStore.load(ds)
      response <- MerchantMeApi.plan(MerchantMeApi.MerchantMeQuery(state, username))
      output <- response match
        case None => IO.raiseError(HttpApiError.NotFound(MerchantApiSupport.merchantNotFound.error))
        case Some(value) => IO.pure(value)
    yield output

final case class MerchantProfileAPIMessage(profile: MerchantProfile) extends APIWithRoleMessage[OkResponse]:
  override def plan(ds: DataSource, username: String): IO[OkResponse] =
    for
      state <- DeliveryStateStore.load(ds)
      response <- MerchantProfileApi.plan(MerchantProfileApi.MerchantProfileCommand(state, username, MerchantProfileBody(profile)))
      output <- response match
        case Left(msg) => IO.raiseError(HttpApiError.BadRequest(msg))
        case Right(value) => DeliveryStateStore.save(ds)(value.nextState).as(value.response)
    yield output

final case class MerchantStoreAPIMessage(storeName: String, address: String) extends APIWithRoleMessage[String]:
  override def plan(ds: DataSource, username: String): IO[String] =
    val body = CreateStoreRequest(storeName, address)
    for
      state <- DeliveryStateStore.load(ds)
      response <- MerchantStoreApi.plan(MerchantStoreApi.MerchantStoreCommand(state, username, body))
      output <- response match
        case Left(msg) => IO.raiseError(HttpApiError.BadRequest(msg))
        case Right(value) => DeliveryStateStore.save(ds)(value.nextState).as(value.response)
    yield output

final case class MerchantStoreImageAPIMessage(merchantId: String, imageUrl: String) extends APIWithRoleMessage[OkResponse]:
  override def plan(ds: DataSource, username: String): IO[OkResponse] =
    val body = UpdateStoreImageRequest(imageUrl)
    for
      state <- DeliveryStateStore.load(ds)
      response <- MerchantStoreImageApi.plan(MerchantStoreImageApi.MerchantStoreImageCommand(state, username, merchantId, body))
      output <- response match
        case Left(msg) => IO.raiseError(HttpApiError.BadRequest(msg))
        case Right(value) => DeliveryStateStore.save(ds)(value.nextState).as(value.response)
    yield output

final case class MerchantStoreImageFileAPIMessage(
    merchantId: String,
    bytesBase64: String,
    contentTypeLower: String,
    filenameHint: Option[String]
) extends APIWithRoleMessage[String]:
  override def plan(ds: DataSource, username: String): IO[String] =
    for
      bytes <- IO.blocking(Base64.getDecoder.decode(bytesBase64)).handleErrorWith(_ => IO.raiseError(HttpApiError.BadRequest("图片内容格式错误")))
      state <- DeliveryStateStore.load(ds)
      response <- MerchantStoreImageFileApi.plan(MerchantStoreImageFileApi.MerchantStoreImageFileCommand(state, username, merchantId, bytes, contentTypeLower, filenameHint))
      output <- response match
        case Left(msg) => IO.raiseError(HttpApiError.BadRequest(msg))
        case Right(value) => DeliveryStateStore.save(ds)(value.nextState).as(value.response)
    yield output

final case class MerchantCreateProductAPIMessage(
    merchantId: String,
    name: String,
    description: String,
    price: Double,
    remainingStock: Int,
    listingStatus: String
) extends APIWithRoleMessage[Product]:
  override def plan(ds: DataSource, username: String): IO[Product] =
    val body = CreateProductRequest(merchantId, name, description, price, remainingStock, listingStatus)
    for
      state <- DeliveryStateStore.load(ds)
      response <- MerchantCreateProductApi.plan(MerchantCreateProductApi.MerchantCreateProductCommand(state, username, body))
      output <- response match
        case Left(msg) => IO.raiseError(HttpApiError.BadRequest(msg))
        case Right(value) => DeliveryStateStore.save(ds)(value.nextState).as(value.response)
    yield output

final case class MerchantProductAPIMessage(
    productId: String,
    name: String,
    description: String,
    price: Double,
    remainingStock: Int,
    listingStatus: String
) extends APIWithRoleMessage[Product]:
  override def plan(ds: DataSource, username: String): IO[Product] =
    val body = UpdateProductRequest(name, description, price, remainingStock, listingStatus)
    for
      state <- DeliveryStateStore.load(ds)
      response <- MerchantProductApi.plan(MerchantProductApi.MerchantProductCommand(state, username, productId, body))
      output <- response match
        case Left(msg) => IO.raiseError(HttpApiError.BadRequest(msg))
        case Right(value) => DeliveryStateStore.save(ds)(value.nextState).as(value.response)
    yield output

final case class MerchantOrderReadyAPIMessage(orderId: String) extends APIWithRoleMessage[OkResponse]:
  override def plan(ds: DataSource, username: String): IO[OkResponse] =
    for
      state <- DeliveryStateStore.load(ds)
      response <- MerchantOrderReadyApi.plan(MerchantOrderReadyApi.MerchantOrderReadyCommand(state, username, orderId))
      output <- response match
        case Left(msg) => IO.raiseError(HttpApiError.BadRequest(msg))
        case Right(value) => DeliveryStateStore.save(ds)(value.nextState).as(value.response)
    yield output
