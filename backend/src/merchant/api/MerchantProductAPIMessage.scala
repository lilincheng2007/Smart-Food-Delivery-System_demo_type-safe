package delivery.merchant.api

import cats.effect.IO
import delivery.merchant.objects.{Product, ProductBundleGroup}
import delivery.merchant.tables.catalogproduct.CatalogProductTable
import delivery.shared.api.{APIWithRoleMessage, HttpApiError}
import delivery.shared.objects.{ListingStatus, ProductId}

import java.sql.Connection

final case class MerchantProductAPIMessage(
    productId: ProductId,
    name: String,
    description: String,
    imageUrl: Option[String],
    categoryName: Option[String],
    price: Double,
    remainingStock: Int,
    listingStatus: ListingStatus,
    bundleGroups: Option[List[ProductBundleGroup]] = None
) extends APIWithRoleMessage[Product]:
  override def plan(connection: Connection, username: String): IO[Product] =
    if name.trim.isEmpty || description.trim.isEmpty then IO.raiseError(HttpApiError.BadRequest("菜品名称和描述不能为空"))
    else if price < 0 || remainingStock < 0 then IO.raiseError(HttpApiError.BadRequest("价格和库存不能为负数"))
    else
      for
        existing <- CatalogProductTable.findById(connection, productId).flatMap {
          case Some(value) => IO.pure(value)
          case None        => IO.raiseError(HttpApiError.BadRequest("未找到菜品"))
        }
        _ <- MerchantAPIMessageSupport.requireOwnedStore(connection, username, existing.merchantId)
        existingProducts <- CatalogProductTable.list(connection)
        productImageUrl <- MerchantAPIMessageSupport.validateProductImageUrl(imageUrl.getOrElse(existing.imageUrl))
        productCategoryName = MerchantAPIMessageSupport.normalizeProductCategoryName(categoryName.orElse(Some(existing.categoryName)))
        normalizedBundleGroups = bundleGroups.getOrElse(existing.bundleGroups)
        bundlePrice <- bundleBasePrice(normalizedBundleGroups, existingProducts, existing.merchantId, Some(existing.id)) match
          case Left(message) => IO.raiseError(HttpApiError.BadRequest(message))
          case Right(value) => IO.pure(value)
        updated = existing.copy(
          name = name.trim,
          description = description.trim,
          imageUrl = productImageUrl,
          categoryName = productCategoryName,
          price = if normalizedBundleGroups.nonEmpty then bundlePrice else price,
          remainingStock = remainingStock,
          listingStatus = listingStatus,
          inventoryStatus = MerchantAPIMessageSupport.inventoryStatus(remainingStock, listingStatus),
          bundleGroups = normalizedBundleGroups
        )
        _ <- CatalogProductTable.upsert(connection, updated)
      yield updated

  private def bundleBasePrice(groups: List[ProductBundleGroup], products: List[Product], merchantId: String, selfId: Option[String]): Either[String, Double] =
    if groups.isEmpty then Right(0)
    else
      groups.foldLeft[Either[String, Double]](Right(0)) { case (acc, group) =>
        acc.flatMap { sum =>
          if group.name.trim.isEmpty then Left("套餐类别名称不能为空")
          else if group.quantity <= 0 then Left("套餐类别可选件数必须大于 0")
          else if group.options.isEmpty then Left(s"${group.name}至少需要选择一个菜品")
          else
            val optionProducts = group.options.flatMap(option => products.find(product => product.id == option.productId))
            val invalid = optionProducts.length != group.options.length || optionProducts.exists(product => product.merchantId != merchantId || product.bundleGroups.nonEmpty || selfId.contains(product.id))
            if invalid then Left(s"${group.name}包含不可选菜品")
            else Right(sum + optionProducts.map(_.price).min * group.quantity)
        }
      }
