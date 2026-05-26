package delivery.merchant.tables

import cats.effect.IO
import cats.syntax.all.*
import delivery.merchant.tables.catalogproduct.CatalogProductTableInitializer
import delivery.merchant.tables.merchantaccount.MerchantAccountTableInitializer
import delivery.merchant.tables.merchantstore.MerchantStoreTableInitializer

import java.sql.Connection

object MerchantTableRegistry:
  val MerchantAccounts = "merchant_accounts"
  val MerchantStores = "merchant_stores"
  val CatalogMerchants = "catalog_merchants"
  val CatalogProducts = "catalog_products"

  val all: List[String] = List(MerchantAccounts, MerchantStores, CatalogMerchants, CatalogProducts)

  def initialize(connection: Connection): IO[Unit] =
    List(
      MerchantAccountTableInitializer.initialize(connection),
      MerchantStoreTableInitializer.initialize(connection),
      CatalogProductTableInitializer.initialize(connection)
    ).sequence_.void

end MerchantTableRegistry
