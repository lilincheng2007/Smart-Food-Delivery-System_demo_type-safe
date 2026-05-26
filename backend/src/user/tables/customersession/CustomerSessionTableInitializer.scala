package delivery.user.tables.customersession

import cats.effect.IO

import java.sql.Connection

object CustomerSessionTableInitializer:

  private val initTableSql: String =
    """
      |CREATE TABLE IF NOT EXISTS customer_sessions (
      |  token_hash VARCHAR(128) PRIMARY KEY,
      |  role VARCHAR(32) NOT NULL,
      |  username VARCHAR(80) NOT NULL,
      |  expires_at TIMESTAMPTZ NOT NULL,
      |  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
      |  FOREIGN KEY (role, username) REFERENCES auth_credentials(role, username) ON DELETE CASCADE
      |);
      |
      |CREATE INDEX IF NOT EXISTS customer_sessions_expires_at_idx ON customer_sessions(expires_at);
      |""".stripMargin

  def initialize(connection: Connection): IO[Unit] =
    IO.blocking {
      val statement = connection.createStatement()
      try
        val _ = statement.execute(initTableSql)
        ()
      finally statement.close()
    }

end CustomerSessionTableInitializer
