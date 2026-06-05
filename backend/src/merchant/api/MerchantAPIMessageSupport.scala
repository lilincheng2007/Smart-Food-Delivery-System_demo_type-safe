package delivery.merchant.api

import cats.effect.IO
import delivery.merchant.objects.Merchant
import delivery.merchant.tables.merchantstore.MerchantStoreTable
import delivery.shared.api.HttpApiError
import delivery.shared.objects.{InventoryStatus, ListingStatus, MerchantId, OrderStatus}

import java.sql.Connection

object MerchantAPIMessageSupport:

  def isHistoryOrderStatus(status: OrderStatus): Boolean =
    OrderStatus.history.contains(status)

  def canAcceptOrder(status: OrderStatus): Boolean =
    status == OrderStatus.待商家接单

  def canRejectOrder(status: OrderStatus): Boolean =
    status == OrderStatus.待商家接单

  def canFinishCooking(status: OrderStatus): Boolean =
    status == OrderStatus.制作中

  def inventoryStatus(remainingStock: Int, listingStatus: ListingStatus): InventoryStatus =
    if listingStatus == ListingStatus.下架 || remainingStock <= 0 then InventoryStatus.售罄
    else if remainingStock <= 20 then InventoryStatus.紧张
    else InventoryStatus.充足

  def requireOwnedStore(connection: Connection, username: String, merchantId: MerchantId): IO[Merchant] =
    MerchantStoreTable.listByOwner(connection, username).flatMap { stores =>
      IO.fromOption(stores.find(_.id == merchantId))(HttpApiError.BadRequest("无权操作该店铺"))
    }

  def listOwnedStores(connection: Connection, username: String): IO[List[Merchant]] =
    MerchantStoreTable.listByOwner(connection, username)

  def validateImageUrl(rawUrl: String): IO[Option[String]] =
    validateImageUrl(rawUrl, "/api/merchant/store-images/", "图片链接须为 http(s) 地址、本地上传生成的 /api/merchant/store-images/... 路径，或留空以清除")

  def validateProductImageUrl(rawUrl: String): IO[String] =
    validateImageUrl(
      rawUrl,
      "/api/merchant/product-images/",
      "菜品图片链接须为 http(s) 地址、本地上传生成的 /api/merchant/product-images/... 路径，或留空使用默认图"
    ).map(_.getOrElse(defaultProductImageUrl))

  def defaultProductImageUrl: String = "https://picsum.photos/seed/default-product/200/120"

  def normalizeProductCategoryName(categoryName: Option[String]): String =
    val trimmed = categoryName.getOrElse("").trim
    if trimmed.isEmpty then "默认分类" else trimmed.take(40)

  private def validateImageUrl(rawUrl: String, localPrefix: String, errorMessage: String): IO[Option[String]] =
    val trimmed = rawUrl.trim
    val urlOk =
      trimmed.isEmpty ||
        trimmed.startsWith("http://") ||
        trimmed.startsWith("https://") ||
        trimmed.startsWith(localPrefix)
    if trimmed.nonEmpty && !urlOk then
      IO.raiseError(HttpApiError.BadRequest(errorMessage))
    else IO.pure(if trimmed.isEmpty then None else Some(trimmed))

  def storeImageExtension(contentTypeLower: String, filenameHint: Option[String]): Either[String, String] =
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

end MerchantAPIMessageSupport
