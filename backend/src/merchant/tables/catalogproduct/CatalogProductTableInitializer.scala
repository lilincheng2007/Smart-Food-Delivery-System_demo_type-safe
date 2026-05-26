package delivery.merchant.tables.catalogproduct

import cats.effect.IO

import java.sql.Connection

object CatalogProductTableInitializer:

  private val initTableSql: String =
    """
      |CREATE TABLE IF NOT EXISTS catalog_products (
      |  id VARCHAR(80) PRIMARY KEY,
      |  merchant_id VARCHAR(80) NOT NULL REFERENCES merchant_stores(id) ON DELETE CASCADE,
      |  name VARCHAR(160) NOT NULL,
      |  price NUMERIC(12, 2) NOT NULL CHECK (price >= 0),
      |  description TEXT NOT NULL,
      |  image_url TEXT NOT NULL,
      |  monthly_sales INTEGER NOT NULL CHECK (monthly_sales >= 0),
      |  remaining_stock INTEGER NOT NULL CHECK (remaining_stock >= 0),
      |  listing_status VARCHAR(32) NOT NULL CHECK (listing_status IN ('上架', '下架')),
      |  inventory_status VARCHAR(32) NOT NULL CHECK (inventory_status IN ('充足', '紧张', '售罄')),
      |  discount_text TEXT,
      |  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
      |  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
      |);
      |
      |CREATE INDEX IF NOT EXISTS catalog_products_merchant_id_idx ON catalog_products(merchant_id);
      |CREATE INDEX IF NOT EXISTS catalog_products_listing_status_idx ON catalog_products(listing_status);
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
