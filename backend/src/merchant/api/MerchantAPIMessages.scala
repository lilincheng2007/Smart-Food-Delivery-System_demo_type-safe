package delivery.merchant.api

import cats.effect.IO
import cats.syntax.all.*
import delivery.merchant.objects.*
import delivery.merchant.tables.catalogproduct.CatalogProductTable
import delivery.merchant.tables.merchantaccount.MerchantAccountTable
import delivery.merchant.tables.merchantstore.MerchantStoreTable
import delivery.merchant.utils.MerchantApiSupport
import delivery.merchant.utils.StoreImageUploads
import delivery.order.tables.order.OrderTable
import delivery.shared.api.{APIMessage, APIWithRoleMessage, HttpApiError}
import delivery.shared.objects.OkResponse

import java.nio.file.Files
import java.util.Base64
import java.util.UUID
import java.sql.Connection

private def isHistoryOrderStatus(status: String): Boolean =
  status == "已送达" || status == "已完成" || status == "已取消"

private def canFinishCooking(status: String): Boolean =
  status == "制作中" || status == "待接单"

private def inventoryStatus(remainingStock: Int, listingStatus: String): String =
  if listingStatus == "下架" || remainingStock <= 0 then "售罄"
  else if remainingStock <= 20 then "紧张"
  else "充足"

private def requireOwnedStore(connection: Connection, username: String, merchantId: String): IO[Merchant] =
  MerchantStoreTable.listByOwner(connection, username).flatMap { stores =>
    IO.fromOption(stores.find(_.id == merchantId))(HttpApiError.BadRequest("无权操作该店铺"))
  }

private def validateImageUrl(rawUrl: String): IO[Option[String]] =
  val trimmed = rawUrl.trim
  val urlOk =
    trimmed.isEmpty ||
      trimmed.startsWith("http://") ||
      trimmed.startsWith("https://") ||
      trimmed.startsWith("/api/merchant/store-images/")
  if trimmed.nonEmpty && !urlOk then
    IO.raiseError(HttpApiError.BadRequest("图片链接须为 http(s) 地址、本地上传生成的 /api/merchant/store-images/... 路径，或留空以清除"))
  else IO.pure(if trimmed.isEmpty then None else Some(trimmed))

private def storeImageExtension(contentTypeLower: String, filenameHint: Option[String]): Either[String, String] =
  val fromContentType =
    if contentTypeLower.contains("jpeg") || contentTypeLower.contains("jpg") then Some(".jpg")
    else if contentTypeLower.contains("png") then Some(".png")
    else if contentTypeLower.contains("gif") then Some(".gif")
    else if contentTypeLower.contains("webp") then Some(".webp")
    else None
  fromContentType.orElse {
    filenameHint.flatMap { name =>
      val lower = name.toLowerCase
      if lower.endsWith(".jpg") || lower.endsWith(".jpeg") then Some(".jpg")
      else if lower.endsWith(".png") then Some(".png")
      else if lower.endsWith(".gif") then Some(".gif")
      else if lower.endsWith(".webp") then Some(".webp")
      else None
    }
  }.toRight("仅支持 JPEG、PNG、GIF、WebP 图片")

final case class CatalogAPIMessage() extends APIMessage[CatalogResponse]:
  override def plan(connection: Connection): IO[CatalogResponse] =
    for
      products <- CatalogProductTable.list(connection)
      merchants <- MerchantStoreTable.listCatalog(connection)
      visibleProducts = products.filter(_.listingStatus == "上架")
      visibleProductIds = visibleProducts.map(_.id).toSet
      visibleMerchants = merchants.map(merchant => merchant.copy(featuredProductIds = merchant.featuredProductIds.filter(visibleProductIds.contains)))
    yield CatalogResponse(visibleMerchants, visibleProducts)

final case class MerchantMeAPIMessage() extends APIWithRoleMessage[MerchantMeResponse]:
  override def plan(connection: Connection, username: String): IO[MerchantMeResponse] =
    for
      account <- MerchantAccountTable.findByUsername(connection, username)
      response <- account match
        case None => IO.pure(None)
        case Some(value) =>
          for
            stores <- MerchantStoreTable.listByOwner(connection, username)
            products <- CatalogProductTable.list(connection)
            orders <- OrderTable.list(connection)
          yield
            val storeProfiles = stores.map { merchant =>
              val merchantOrders = orders.filter(_.merchantId == merchant.id)
              MerchantStoreProfile(
                merchant = merchant,
                products = products.filter(_.merchantId == merchant.id),
                pendingOrders = merchantOrders.filterNot(order => isHistoryOrderStatus(order.status)),
                historyOrders = merchantOrders.filter(order => isHistoryOrderStatus(order.status))
              )
            }
            Some(MerchantApiSupport.merchantMeResponse(username, value.copy(profile = value.profile.copy(stores = storeProfiles))))
      output <- response match
        case None => IO.raiseError(HttpApiError.NotFound(MerchantApiSupport.merchantNotFound.error))
        case Some(value) => IO.pure(value)
    yield output

final case class MerchantProfileAPIMessage(profile: MerchantProfile) extends APIWithRoleMessage[OkResponse]:
  override def plan(connection: Connection, username: String): IO[OkResponse] =
    for
      existing <- MerchantAccountTable.findByUsername(connection, username)
      account <- existing match
        case Some(value) => IO.pure(value)
        case None        => IO.raiseError(HttpApiError.NotFound(MerchantApiSupport.merchantNotFound.error))
      nextAccount = account.copy(profile = profile)
      _ <- MerchantAccountTable.upsert(connection, nextAccount)
      _ <- profile.stores.traverse_(store => MerchantStoreTable.upsert(connection, username, store.merchant))
    yield OkResponse(ok = true)

final case class MerchantStoreAPIMessage(storeName: String, address: String) extends APIWithRoleMessage[String]:
  override def plan(connection: Connection, username: String): IO[String] =
    if storeName.trim.isEmpty || address.trim.isEmpty then IO.raiseError(HttpApiError.BadRequest("店铺名称和地址不能为空"))
    else
      for
        account <- MerchantAccountTable.findByUsername(connection, username)
        _ <- IO.fromOption(account)(HttpApiError.BadRequest("未找到商家账号"))
        nowMillis <- IO.realTime.map(_.toMillis)
        merchant = Merchant(s"m-local-$nowMillis", storeName.trim, "中餐", address.trim, account.map(_.profile.phone).getOrElse(""), 5, List("新店"), Nil, None)
        _ <- MerchantStoreTable.upsert(connection, username, merchant)
      yield merchant.id

final case class MerchantStoreImageAPIMessage(merchantId: String, imageUrl: String) extends APIWithRoleMessage[OkResponse]:
  override def plan(connection: Connection, username: String): IO[OkResponse] =
    for
      merchant <- requireOwnedStore(connection, username, merchantId)
      imageOpt <- validateImageUrl(imageUrl)
      _ <- MerchantStoreTable.upsert(connection, username, merchant.copy(imageUrl = imageOpt))
    yield OkResponse(ok = true)

final case class MerchantStoreImageFileAPIMessage(
    merchantId: String,
    bytesBase64: String,
    contentTypeLower: String,
    filenameHint: Option[String]
) extends APIWithRoleMessage[String]:
  override def plan(connection: Connection, username: String): IO[String] =
    for
      bytes <- IO.blocking(Base64.getDecoder.decode(bytesBase64)).handleErrorWith(_ => IO.raiseError(HttpApiError.BadRequest("图片内容格式错误")))
      _ <- if bytes.length > 2 * 1024 * 1024 then IO.raiseError(HttpApiError.BadRequest("图片不能超过 2MB")) else IO.unit
      _ <- if bytes.isEmpty then IO.raiseError(HttpApiError.BadRequest("未收到文件内容")) else IO.unit
      ext <- IO.fromEither(storeImageExtension(contentTypeLower, filenameHint).left.map(HttpApiError.BadRequest.apply))
      merchant <- requireOwnedStore(connection, username, merchantId)
      storedName = s"${UUID.randomUUID()}$ext"
      publicPath = s"/api/merchant/store-images/$storedName"
      _ <- IO.blocking(Files.write(StoreImageUploads.directory.resolve(storedName), bytes))
      _ <- MerchantStoreTable.upsert(connection, username, merchant.copy(imageUrl = Some(publicPath)))
    yield publicPath

final case class MerchantCreateProductAPIMessage(
    merchantId: String,
    name: String,
    description: String,
    price: Double,
    remainingStock: Int,
    listingStatus: String
) extends APIWithRoleMessage[Product]:
  override def plan(connection: Connection, username: String): IO[Product] =
    if listingStatus != "上架" && listingStatus != "下架" then IO.raiseError(HttpApiError.BadRequest("上/下架状态不合法"))
    else if name.trim.isEmpty || description.trim.isEmpty then IO.raiseError(HttpApiError.BadRequest("菜品名称和描述不能为空"))
    else if price < 0 || remainingStock < 0 then IO.raiseError(HttpApiError.BadRequest("价格和库存不能为负数"))
    else
      for
        merchant <- requireOwnedStore(connection, username, merchantId)
        nowMillis <- IO.realTime.map(_.toMillis)
        product = Product(
          id = s"p-local-$nowMillis",
          merchantId = merchantId,
          name = name.trim,
          price = price,
          description = description.trim,
          imageUrl = s"https://picsum.photos/200/120?product-$nowMillis",
          monthlySales = 0,
          remainingStock = remainingStock,
          listingStatus = listingStatus,
          inventoryStatus = inventoryStatus(remainingStock, listingStatus),
          discountText = None
        )
        _ <- CatalogProductTable.upsert(connection, product)
        _ <- MerchantStoreTable.upsert(connection, username, merchant.copy(featuredProductIds = merchant.featuredProductIds :+ product.id))
      yield product

final case class MerchantProductAPIMessage(
    productId: String,
    name: String,
    description: String,
    price: Double,
    remainingStock: Int,
    listingStatus: String
) extends APIWithRoleMessage[Product]:
  override def plan(connection: Connection, username: String): IO[Product] =
    if listingStatus != "上架" && listingStatus != "下架" then IO.raiseError(HttpApiError.BadRequest("上/下架状态不合法"))
    else if name.trim.isEmpty || description.trim.isEmpty then IO.raiseError(HttpApiError.BadRequest("菜品名称和描述不能为空"))
    else if price < 0 || remainingStock < 0 then IO.raiseError(HttpApiError.BadRequest("价格和库存不能为负数"))
    else
      for
        existing <- CatalogProductTable.findById(connection, productId).flatMap {
          case Some(value) => IO.pure(value)
          case None        => IO.raiseError(HttpApiError.BadRequest("未找到菜品"))
        }
        _ <- requireOwnedStore(connection, username, existing.merchantId)
        updated = existing.copy(
          name = name.trim,
          description = description.trim,
          price = price,
          remainingStock = remainingStock,
          listingStatus = listingStatus,
          inventoryStatus = inventoryStatus(remainingStock, listingStatus)
        )
        _ <- CatalogProductTable.upsert(connection, updated)
      yield updated

final case class MerchantOrderReadyAPIMessage(orderId: String) extends APIWithRoleMessage[OkResponse]:
  override def plan(connection: Connection, username: String): IO[OkResponse] =
    for
      order <- OrderTable.findById(connection, orderId).flatMap {
        case Some(value) => IO.pure(value)
        case None        => IO.raiseError(HttpApiError.BadRequest("未找到订单"))
      }
      _ <- requireOwnedStore(connection, username, order.merchantId)
      _ <-
        if canFinishCooking(order.status) then IO.unit
        else IO.raiseError(HttpApiError.BadRequest(s"当前状态不可执行出餐完成：${order.status}"))
      _ <- OrderTable.upsert(connection, order.copy(status = "待接单"))
    yield OkResponse(ok = true)
