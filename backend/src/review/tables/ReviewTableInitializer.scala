package delivery.review.tables

import cats.effect.IO

import java.sql.Connection

object ReviewTableInitializer:
  private val initTableSql: String =
    """
      |CREATE TABLE IF NOT EXISTS merchant_reviews (
      |  id VARCHAR(80) PRIMARY KEY,
      |  order_id VARCHAR(80) NOT NULL UNIQUE REFERENCES orders(id) ON DELETE CASCADE,
      |  merchant_id VARCHAR(80) NOT NULL REFERENCES merchant_stores(id) ON DELETE CASCADE,
      |  customer_id VARCHAR(80) NOT NULL,
      |  customer_name VARCHAR(120) NOT NULL,
      |  rating INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
      |  description TEXT NOT NULL,
      |  image_url TEXT,
      |  merchant_reply TEXT,
      |  merchant_reply_at TIMESTAMPTZ,
      |  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
      |);
      |
      |ALTER TABLE merchant_reviews
      |  ADD COLUMN IF NOT EXISTS merchant_reply TEXT;
      |
      |ALTER TABLE merchant_reviews
      |  ADD COLUMN IF NOT EXISTS merchant_reply_at TIMESTAMPTZ;
      |
      |CREATE TABLE IF NOT EXISTS merchant_review_votes (
      |  review_id VARCHAR(80) NOT NULL REFERENCES merchant_reviews(id) ON DELETE CASCADE,
      |  customer_id VARCHAR(80) NOT NULL,
      |  vote VARCHAR(8) NOT NULL CHECK (vote IN ('up', 'down')),
      |  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
      |  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
      |  PRIMARY KEY (review_id, customer_id)
      |);
      |
      |CREATE TABLE IF NOT EXISTS rider_reviews (
      |  id VARCHAR(80) PRIMARY KEY,
      |  order_id VARCHAR(80) NOT NULL UNIQUE REFERENCES orders(id) ON DELETE CASCADE,
      |  rider_id VARCHAR(80) NOT NULL REFERENCES rider_profiles(id) ON DELETE CASCADE,
      |  customer_id VARCHAR(80) NOT NULL,
      |  customer_name VARCHAR(120) NOT NULL,
      |  rating INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
      |  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
      |);
      |
      |CREATE INDEX IF NOT EXISTS merchant_reviews_merchant_id_idx ON merchant_reviews(merchant_id);
      |CREATE INDEX IF NOT EXISTS rider_reviews_rider_id_idx ON rider_reviews(rider_id);
      |""".stripMargin

  def initialize(connection: Connection): IO[Unit] =
    IO.blocking {
      val statement = connection.createStatement()
      try
        val _ = statement.execute(initTableSql)
        ()
      finally statement.close()
    }
