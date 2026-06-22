package delivery.merchant.services

import cats.effect.IO
import delivery.domain.ListingStatus
import delivery.merchant.objects.{Merchant, Product}
import delivery.merchant.tables.catalogproduct.CatalogProductTable
import delivery.merchant.tables.merchantstore.MerchantStoreTable
import delivery.review.tables.MerchantReviewTable

import java.sql.Connection

object CatalogQueryService:

  final case class CatalogSnapshot(
      merchants: List[Merchant],
      products: List[Product]
  )

  def listVisibleCatalog(connection: Connection): IO[CatalogSnapshot] =
    for
      products <- CatalogProductTable.list(connection)
      merchants <- MerchantStoreTable.listCatalog(connection)
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
    yield CatalogSnapshot(visibleMerchants, visibleProducts)

  def findVisibleMerchant(connection: Connection, merchantId: String): IO[Option[Merchant]] =
    listVisibleCatalog(connection).map(_.merchants.find(_.id == merchantId))

end CatalogQueryService
