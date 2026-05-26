package delivery.rider.tables.riderprofile

import cats.effect.IO

import java.sql.Connection

object RiderProfileTableInitializer:

  private val initTableSql: String =
    """
      |CREATE TABLE IF NOT EXISTS rider_profiles (
      |  id VARCHAR(80) PRIMARY KEY,
      |  name VARCHAR(120) NOT NULL,
      |  phone VARCHAR(40) NOT NULL,
      |  realtime_location TEXT NOT NULL,
      |  status VARCHAR(32) NOT NULL CHECK (status IN ('空闲', '接单', '配送中')),
      |  total_orders INTEGER NOT NULL CHECK (total_orders >= 0),
      |  rating NUMERIC(3, 2) NOT NULL CHECK (rating >= 0 AND rating <= 5),
      |  station VARCHAR(120) NOT NULL,
      |  salary NUMERIC(12, 2) NOT NULL CHECK (salary >= 0),
      |  wallet_balance NUMERIC(12, 2) NOT NULL DEFAULT 0 CHECK (wallet_balance >= 0),
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

end RiderProfileTableInitializer
