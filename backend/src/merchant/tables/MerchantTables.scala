package delivery.merchant.tables

object MerchantTables:
  val ServiceState = "merchant_service_state"
  val MerchantAccounts = "merchant_accounts"
  val MerchantStores = "merchant_stores"
  val CatalogMerchants = "catalog_merchants"
  val CatalogProducts = "catalog_products"

  val all: List[String] = List(ServiceState, MerchantAccounts, MerchantStores, CatalogMerchants, CatalogProducts)

end MerchantTables
