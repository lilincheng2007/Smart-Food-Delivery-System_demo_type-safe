package delivery.merchant.tables.merchantaccount

import cats.effect.IO

import java.sql.Connection

object MerchantAccountTableInitializer:

  private val initTableSql: String =
    """
      |CREATE TABLE IF NOT EXISTS merchant_accounts (
      |  username VARCHAR(80) PRIMARY KEY,
      |  role VARCHAR(32) NOT NULL DEFAULT 'merchant' CHECK (role = 'merchant'),
      |  password VARCHAR(256) NOT NULL,
      |  profile_id VARCHAR(80) UNIQUE NOT NULL,
      |  owner_name VARCHAR(120) NOT NULL,
      |  phone VARCHAR(40) NOT NULL,
      |  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
      |  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
      |);
      |""".stripMargin

  def initialize(connection: Connection): IO[Unit] =
    IO.blocking {
      val statement = connection.createStatement()
      try
        val _ = statement.execute(initTableSql)
        ()
      finally statement.close()
    }

end MerchantAccountTableInitializer
