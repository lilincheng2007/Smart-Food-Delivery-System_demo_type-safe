package delivery.merchant.tables.merchantstore

import cats.effect.IO

import java.sql.Connection

object MerchantStoreTableInitializer:

  private val initTableSql: String =
    """
      |CREATE TABLE IF NOT EXISTS merchant_stores (
      |  id VARCHAR(80) PRIMARY KEY,
      |  owner_username VARCHAR(80) NOT NULL REFERENCES merchant_accounts(username) ON DELETE CASCADE,
      |  store_name VARCHAR(160) NOT NULL,
      |  category VARCHAR(32) NOT NULL,
      |  address TEXT NOT NULL,
      |  phone VARCHAR(40) NOT NULL,
      |  rating NUMERIC(3, 2) NOT NULL CHECK (rating >= 0 AND rating <= 5),
      |  tags JSONB NOT NULL DEFAULT '[]'::jsonb,
      |  featured_product_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
      |  image_url TEXT,
      |  description TEXT NOT NULL DEFAULT '',
      |  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
      |  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
      |);
      |
      |CREATE TABLE IF NOT EXISTS catalog_merchants (
      |  id VARCHAR(80) PRIMARY KEY REFERENCES merchant_stores(id) ON DELETE CASCADE,
      |  store_name VARCHAR(160) NOT NULL,
      |  category VARCHAR(32) NOT NULL,
      |  address TEXT NOT NULL,
      |  phone VARCHAR(40) NOT NULL,
      |  rating NUMERIC(3, 2) NOT NULL CHECK (rating >= 0 AND rating <= 5),
      |  tags JSONB NOT NULL DEFAULT '[]'::jsonb,
      |  featured_product_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
      |  image_url TEXT,
      |  description TEXT NOT NULL DEFAULT '',
      |  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
      |);
      |
      |ALTER TABLE merchant_stores ADD COLUMN IF NOT EXISTS description TEXT NOT NULL DEFAULT '';
      |ALTER TABLE catalog_merchants ADD COLUMN IF NOT EXISTS description TEXT NOT NULL DEFAULT '';
      |
      |CREATE INDEX IF NOT EXISTS merchant_stores_owner_username_idx ON merchant_stores(owner_username);
      |""".stripMargin

  def initialize(connection: Connection): IO[Unit] =
    IO.blocking {
      val statement = connection.createStatement()
      try
        val _ = statement.execute(initTableSql)
        ()
      finally statement.close()
    }

end MerchantStoreTableInitializer
