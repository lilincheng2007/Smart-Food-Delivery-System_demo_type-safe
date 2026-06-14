package delivery.merchant.services

import cats.effect.IO
import delivery.merchant.objects.{Merchant, Product, ProductBundleGroup, ProductBundleSelectionType, ProductInventoryMode}
import delivery.merchant.tables.merchantstore.MerchantStoreTable
import delivery.merchant.validators.MerchantStoreOwnershipValidator
import delivery.platform.api.HttpApiError
import delivery.domain.{InventoryStatus, ListingStatus, MerchantId, OrderStatus}

import java.sql.Connection

object MerchantBusinessService:

  def isHistoryOrderStatus(status: OrderStatus): Boolean =
    OrderStatus.history.contains(status)

  def canAcceptOrder(status: OrderStatus): Boolean =
    status == OrderStatus.待商家接单

  def canRejectOrder(status: OrderStatus): Boolean =
    status == OrderStatus.待商家接单

  def canFinishCooking(status: OrderStatus): Boolean =
    status == OrderStatus.制作中

  def inventoryStatus(remainingStock: Int, listingStatus: ListingStatus, inventoryMode: ProductInventoryMode): InventoryStatus =
    if listingStatus == ListingStatus.下架 || inventoryMode == ProductInventoryMode.soldOut then InventoryStatus.售罄
    else if inventoryMode == ProductInventoryMode.unlimited then InventoryStatus.充足
    else if remainingStock <= 0 then InventoryStatus.售罄
    else if remainingStock <= 20 then InventoryStatus.紧张
    else InventoryStatus.充足

  def normalizeInventoryMode(value: Option[ProductInventoryMode]): ProductInventoryMode =
    value.getOrElse(ProductInventoryMode.finite)

  def normalizeMaxPerOrder(value: Option[Int]): Option[Int] =
    value.filter(_ > 0).map(limit => math.min(limit, 999))

  def requireOwnedStore(connection: Connection, username: String, merchantId: MerchantId): IO[Merchant] =
    MerchantStoreOwnershipValidator.requireOwnedStore(connection, username, merchantId)

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

  def validateBundleGroups(groups: List[ProductBundleGroup], products: List[Product], merchantId: MerchantId, selfId: Option[String]): Either[String, Unit] =
    if groups.isEmpty then Right(())
    else
      groups.foldLeft[Either[String, Unit]](Right(())) { case (acc, group) =>
        acc.flatMap { _ =>
          if group.name.trim.isEmpty then Left("套餐类别名称不能为空")
          else if group.quantity <= 0 then Left("套餐类别可选件数必须大于 0")
          else if group.options.isEmpty then Left(s"${group.name}至少需要选择一个菜品")
          else
            val optionProducts = group.options.flatMap(option => products.find(product => product.id == option.productId))
            val invalid = optionProducts.length != group.options.length || optionProducts.exists(product => product.merchantId != merchantId || product.bundleGroups.nonEmpty || selfId.contains(product.id))
            val selectionTypeValid = ProductBundleSelectionType.values.contains(group.selectionType)
            val hasFreeOption = group.options.exists { option =>
              optionProducts.find(_.id == option.productId).exists { product =>
                val extraPrice =
                  if option.customExtraPrice || option.extraPrice > 0 then math.max(0.0, option.extraPrice)
                  else if group.includedPrice > 0 then math.max(0.0, product.price - group.includedPrice)
                  else 0.0
                extraPrice <= 0
              }
            }
            if invalid then Left(s"${group.name}包含不可选菜品")
            else if !selectionTypeValid then Left(s"${group.name}套餐类型不合法")
            else if group.includedPrice < 0 || group.options.exists(_.extraPrice < 0) then Left(s"${group.name}包含价和加价不能为负数")
            else if !hasFreeOption then Left(s"${group.name}至少需要包含一个不加价菜品")
            else Right(())
        }
      }

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

end MerchantBusinessService
