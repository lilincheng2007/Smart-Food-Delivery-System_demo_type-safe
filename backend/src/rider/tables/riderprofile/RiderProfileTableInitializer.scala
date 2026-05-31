package delivery.rider.tables.riderprofile

import cats.effect.IO
import delivery.shared.objects.RiderStatus

import java.sql.Connection

object RiderProfileTableInitializer:

  private val riderStatusSql: String = RiderStatus.values.map(status => s"'${status.toString}'").mkString(", ")

  private val initTableSql: String =
    s"""
      |CREATE TABLE IF NOT EXISTS rider_profiles (
      |  id VARCHAR(80) PRIMARY KEY,
      |  name VARCHAR(120) NOT NULL,
      |  phone VARCHAR(40) NOT NULL,
      |  realtime_location TEXT NOT NULL,
      |  status VARCHAR(32) NOT NULL CHECK (status IN ($riderStatusSql)),
      |  total_orders INTEGER NOT NULL CHECK (total_orders >= 0),
      |  rating NUMERIC(3, 2) NOT NULL CHECK (rating >= 0 AND rating <= 5),
      |  station VARCHAR(120) NOT NULL,
      |  salary NUMERIC(12, 2) NOT NULL CHECK (salary >= 0),
      |  energy_points INTEGER NOT NULL DEFAULT 0 CHECK (energy_points >= 0),
      |  timeout_card_count INTEGER NOT NULL DEFAULT 0 CHECK (timeout_card_count >= 0),
      |  timeout_count INTEGER NOT NULL DEFAULT 0 CHECK (timeout_count >= 0),
      |  timeout_exempted_count INTEGER NOT NULL DEFAULT 0 CHECK (timeout_exempted_count >= 0),
      |  wallet_balance NUMERIC(12, 2) NOT NULL DEFAULT 0 CHECK (wallet_balance >= 0),
      |  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
      |  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
      |);
      |
      |ALTER TABLE rider_profiles ADD COLUMN IF NOT EXISTS energy_points INTEGER NOT NULL DEFAULT 0 CHECK (energy_points >= 0);
      |ALTER TABLE rider_profiles ADD COLUMN IF NOT EXISTS timeout_card_count INTEGER NOT NULL DEFAULT 0 CHECK (timeout_card_count >= 0);
      |ALTER TABLE rider_profiles ADD COLUMN IF NOT EXISTS timeout_count INTEGER NOT NULL DEFAULT 0 CHECK (timeout_count >= 0);
      |ALTER TABLE rider_profiles ADD COLUMN IF NOT EXISTS timeout_exempted_count INTEGER NOT NULL DEFAULT 0 CHECK (timeout_exempted_count >= 0);
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
