package delivery.user.tables.authcredential

import cats.effect.IO

import java.sql.Connection

object AuthCredentialTableInitializer:

  private val initTableSql: String =
    """
      |CREATE TABLE IF NOT EXISTS auth_credentials (
      |  role VARCHAR(32) NOT NULL CHECK (role IN ('customer', 'merchant', 'rider')),
      |  username VARCHAR(80) NOT NULL,
      |  password VARCHAR(256) NOT NULL,
      |  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
      |  PRIMARY KEY (role, username)
      |);
      |
      |CREATE INDEX IF NOT EXISTS auth_credentials_username_idx ON auth_credentials(username);
      |""".stripMargin

  def initialize(connection: Connection): IO[Unit] =
    IO.blocking {
      val statement = connection.createStatement()
      try
        val _ = statement.execute(initTableSql)
        ()
      finally statement.close()
    }

end AuthCredentialTableInitializer
