package delivery.rider.tables.rideraccount

import cats.effect.IO

import java.sql.Connection

object RiderAccountTableInitializer:

  private val initTableSql: String =
    """
      |CREATE TABLE IF NOT EXISTS rider_accounts (
      |  username VARCHAR(80) PRIMARY KEY,
      |  role VARCHAR(32) NOT NULL DEFAULT 'rider' CHECK (role = 'rider'),
      |  password VARCHAR(256) NOT NULL,
      |  rider_id VARCHAR(80) UNIQUE NOT NULL REFERENCES rider_profiles(id) ON DELETE CASCADE,
      |  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
      |);
      |
      |CREATE INDEX IF NOT EXISTS rider_accounts_rider_id_idx ON rider_accounts(rider_id);
      |""".stripMargin

  def initialize(connection: Connection): IO[Unit] =
    IO.blocking {
      val statement = connection.createStatement()
      try
        val _ = statement.execute(initTableSql)
        ()
      finally statement.close()
    }

end RiderAccountTableInitializer
