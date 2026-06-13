package delivery.merchant.api

import cats.effect.IO
import delivery.admin.tables.platformpromotion.PlatformPromotionTable
import delivery.merchant.objects.apiTypes.CatalogResponse
import delivery.merchant.tables.catalogproduct.CatalogProductTable
import delivery.merchant.tables.merchantstore.MerchantStoreTable
import delivery.review.tables.MerchantReviewTable
import delivery.platform.api.APIMessage
import delivery.domain.ListingStatus

import java.sql.Connection

final case class CatalogAPIMessage() extends APIMessage[CatalogResponse]:
  override def plan(connection: Connection): IO[CatalogResponse] =
    for
      products <- CatalogProductTable.list(connection)
      merchants <- MerchantStoreTable.listCatalog(connection)
      platformPromotions <- PlatformPromotionTable.get(connection)
      reviewSummaries <- MerchantReviewTable.summariesByMerchant(connection, merchants.map(_.id))
      visibleProducts = products.filter(_.listingStatus == ListingStatus.上架)
      visibleProductIds = visibleProducts.map(_.id).toSet
      visibleMerchants = merchants.map { merchant =>
        val reviewedRating = reviewSummaries.get(merchant.id).filter(_.reviewCount > 0).map(_.averageRating)
        merchant.copy(
          rating = reviewedRating.getOrElse(merchant.rating),
          featuredProductIds = merchant.featuredProductIds.filter(visibleProductIds.contains)
        )
      }
    yield CatalogResponse(visibleMerchants, visibleProducts, platformPromotions)
