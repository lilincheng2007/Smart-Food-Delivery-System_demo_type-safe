package delivery.merchant.tables.catalogproduct

import cats.effect.IO
import delivery.domain.{InventoryStatus, ListingStatus}

import java.sql.Connection

object CatalogProductTableInitializer:

  private val listingStatusSql: String = ListingStatus.values.map(status => s"'${status.toString}'").mkString(", ")
  private val inventoryStatusSql: String = InventoryStatus.values.map(status => s"'${status.toString}'").mkString(", ")

  private val initTableSql: String =
    s"""
      |CREATE TABLE IF NOT EXISTS catalog_products (
      |  id VARCHAR(80) PRIMARY KEY,
      |  merchant_id VARCHAR(80) NOT NULL REFERENCES merchant_stores(id) ON DELETE CASCADE,
      |  name VARCHAR(160) NOT NULL,
      |  price NUMERIC(12, 2) NOT NULL CHECK (price >= 0),
      |  description TEXT NOT NULL,
      |  image_url TEXT NOT NULL,
      |  monthly_sales INTEGER NOT NULL CHECK (monthly_sales >= 0),
      |  remaining_stock INTEGER NOT NULL CHECK (remaining_stock >= 0),
      |  listing_status VARCHAR(32) NOT NULL CHECK (listing_status IN ($listingStatusSql)),
      |  inventory_status VARCHAR(32) NOT NULL CHECK (inventory_status IN ($inventoryStatusSql)),
      |  discount_text TEXT,
      |  category_name TEXT NOT NULL DEFAULT '默认分类',
      |  bundle_config JSONB NOT NULL DEFAULT '[]'::jsonb,
      |  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
      |  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
      |);
      |
      |ALTER TABLE catalog_products
      |  ADD COLUMN IF NOT EXISTS category_name TEXT NOT NULL DEFAULT '默认分类';
      |
      |ALTER TABLE catalog_products
      |  ADD COLUMN IF NOT EXISTS bundle_config JSONB NOT NULL DEFAULT '[]'::jsonb;
      |
      |ALTER TABLE catalog_products
      |  ADD COLUMN IF NOT EXISTS inventory_mode VARCHAR(32) NOT NULL DEFAULT 'finite';
      |
      |ALTER TABLE catalog_products
      |  ADD COLUMN IF NOT EXISTS max_per_order INTEGER;
      |
      |ALTER TABLE catalog_products DROP CONSTRAINT IF EXISTS catalog_products_inventory_mode_check;
      |ALTER TABLE catalog_products ADD CONSTRAINT catalog_products_inventory_mode_check CHECK (inventory_mode IN ('unlimited', 'finite', 'soldOut'));
      |ALTER TABLE catalog_products DROP CONSTRAINT IF EXISTS catalog_products_max_per_order_check;
      |ALTER TABLE catalog_products ADD CONSTRAINT catalog_products_max_per_order_check CHECK (max_per_order IS NULL OR max_per_order > 0);
      |
      |UPDATE catalog_products
      |SET category_name = '默认分类'
      |WHERE btrim(category_name) = '';
      |
      |CREATE INDEX IF NOT EXISTS catalog_products_merchant_id_idx ON catalog_products(merchant_id);
      |CREATE INDEX IF NOT EXISTS catalog_products_listing_status_idx ON catalog_products(listing_status);
      |CREATE INDEX IF NOT EXISTS catalog_products_category_name_idx ON catalog_products(category_name);
      |""".stripMargin

  def initialize(connection: Connection): IO[Unit] =
    IO.blocking {
      val statement = connection.createStatement()
      try
        val _ = statement.execute(initTableSql)
        ()
      finally statement.close()
    }

end CatalogProductTableInitializer
