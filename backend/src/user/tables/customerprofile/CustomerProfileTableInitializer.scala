package delivery.user.tables.customerprofile

import cats.effect.IO

import java.sql.Connection

object CustomerProfileTableInitializer:

  private val initTableSql: String =
    """
      |CREATE TABLE IF NOT EXISTS customer_profiles (
      |  id VARCHAR(80) PRIMARY KEY REFERENCES customers(id) ON DELETE CASCADE,
      |  username VARCHAR(80) UNIQUE NOT NULL,
      |  role VARCHAR(32) NOT NULL DEFAULT 'customer' CHECK (role = 'customer'),
      |  name VARCHAR(120) NOT NULL,
      |  phone VARCHAR(40) NOT NULL,
      |  default_address TEXT NOT NULL,
      |  wallet_balance NUMERIC(12, 2) NOT NULL CHECK (wallet_balance >= 0),
      |  vouchers JSONB NOT NULL DEFAULT '[]'::jsonb,
      |  pending_orders JSONB NOT NULL DEFAULT '[]'::jsonb,
      |  history_orders JSONB NOT NULL DEFAULT '[]'::jsonb,
      |  delivery_contacts JSONB NOT NULL DEFAULT '[]'::jsonb,
      |  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
      |);
      |
      |CREATE INDEX IF NOT EXISTS customer_profiles_username_idx ON customer_profiles(username);
      |""".stripMargin

  def initialize(connection: Connection): IO[Unit] =
    IO.blocking {
      val statement = connection.createStatement()
      try
        val _ = statement.execute(initTableSql)
        ()
      finally statement.close()
    }

end CustomerProfileTableInitializer
