package delivery.user.tables.customer

import cats.effect.IO

import java.sql.Connection

object CustomerTableInitializer:

  private val initTableSql: String =
    """
      |CREATE TABLE IF NOT EXISTS customers (
      |  id VARCHAR(80) PRIMARY KEY,
      |  name VARCHAR(120) NOT NULL,
      |  phone VARCHAR(40) NOT NULL,
      |  default_address TEXT NOT NULL,
      |  wallet_balance NUMERIC(12, 2) NOT NULL CHECK (wallet_balance >= 0),
      |  order_history_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
      |  vouchers JSONB NOT NULL DEFAULT '[]'::jsonb,
      |  foodie_points INTEGER NOT NULL DEFAULT 0 CHECK (foodie_points >= 0),
      |  foodie_level INTEGER NOT NULL DEFAULT 1 CHECK (foodie_level >= 1),
      |  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
      |  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
      |);
      |
      |ALTER TABLE customers ADD COLUMN IF NOT EXISTS foodie_points INTEGER NOT NULL DEFAULT 0 CHECK (foodie_points >= 0);
      |ALTER TABLE customers ADD COLUMN IF NOT EXISTS foodie_level INTEGER NOT NULL DEFAULT 1 CHECK (foodie_level >= 1);
      |""".stripMargin

  def initialize(connection: Connection): IO[Unit] =
    IO.blocking {
      val statement = connection.createStatement()
      try
        val _ = statement.execute(initTableSql)
        ()
      finally statement.close()
    }

end CustomerTableInitializer
