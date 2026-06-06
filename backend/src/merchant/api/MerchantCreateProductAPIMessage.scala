package delivery.merchant.api

import cats.effect.IO
import delivery.merchant.objects.{Product, ProductBundleGroup}
import delivery.merchant.tables.catalogproduct.CatalogProductTable
import delivery.merchant.tables.merchantstore.MerchantStoreTable
import delivery.shared.api.{APIWithRoleMessage, HttpApiError}
import delivery.shared.objects.{ListingStatus, MerchantId}

import java.sql.Connection

final case class MerchantCreateProductAPIMessage(
    merchantId: MerchantId,
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
        merchant <- MerchantAPIMessageSupport.requireOwnedStore(connection, username, merchantId)
        existingProducts <- CatalogProductTable.list(connection)
        productImageUrl <- MerchantAPIMessageSupport.validateProductImageUrl(imageUrl.getOrElse(""))
        productCategoryName = MerchantAPIMessageSupport.normalizeProductCategoryName(categoryName)
        normalizedBundleGroups = bundleGroups.getOrElse(Nil)
        bundlePrice <- bundleBasePrice(normalizedBundleGroups, existingProducts, merchantId, None) match
          case Left(message) => IO.raiseError(HttpApiError.BadRequest(message))
          case Right(value) => IO.pure(value)
        nowMillis <- IO.realTime.map(_.toMillis)
        product = Product(
          id = s"p-local-$nowMillis",
          merchantId = merchantId,
          name = name.trim,
          price = if normalizedBundleGroups.nonEmpty then bundlePrice else price,
          description = description.trim,
          imageUrl = productImageUrl,
          categoryName = productCategoryName,
          monthlySales = 0,
          remainingStock = remainingStock,
          listingStatus = listingStatus,
          inventoryStatus = MerchantAPIMessageSupport.inventoryStatus(remainingStock, listingStatus),
          discountText = None,
          bundleGroups = normalizedBundleGroups
        )
        _ <- CatalogProductTable.upsert(connection, product)
        _ <- MerchantStoreTable.upsert(connection, username, merchant.copy(featuredProductIds = merchant.featuredProductIds :+ product.id))
      yield product

  private def bundleBasePrice(groups: List[ProductBundleGroup], products: List[Product], merchantId: MerchantId, selfId: Option[String]): Either[String, Double] =
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
