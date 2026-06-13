package delivery.rider.tables.riderassignment

import cats.effect.IO
import delivery.domain.OrderStatus

import java.sql.Connection

object RiderAssignmentTableInitializer:

  private val assignmentStatusSql: String =
    List(OrderStatus.配送中, OrderStatus.已送达, OrderStatus.已完成, OrderStatus.已取消)
      .map(status => s"'${status.toString}'")
      .mkString(", ")

  private val initTableSql: String =
    s"""
      |CREATE TABLE IF NOT EXISTS rider_assignments (
      |  id BIGSERIAL PRIMARY KEY,
      |  rider_id VARCHAR(80) NOT NULL REFERENCES rider_profiles(id) ON DELETE CASCADE,
      |  order_id VARCHAR(80) NOT NULL,
      |  status VARCHAR(32) NOT NULL CHECK (status IN ($assignmentStatusSql)),
      |  assigned_at TIMESTAMPTZ NOT NULL DEFAULT now(),
      |  completed_at TIMESTAMPTZ,
      |  deadline_at TIMESTAMPTZ,
      |  was_timeout BOOLEAN NOT NULL DEFAULT false,
      |  timeout_exempted BOOLEAN NOT NULL DEFAULT false,
      |  timeout_card_used BOOLEAN NOT NULL DEFAULT false,
      |  overtime_seconds INTEGER NOT NULL DEFAULT 0 CHECK (overtime_seconds >= 0),
      |  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
      |  UNIQUE (rider_id, order_id)
      |);
      |
      |ALTER TABLE rider_assignments ADD COLUMN IF NOT EXISTS deadline_at TIMESTAMPTZ;
      |ALTER TABLE rider_assignments ADD COLUMN IF NOT EXISTS was_timeout BOOLEAN NOT NULL DEFAULT false;
      |ALTER TABLE rider_assignments ADD COLUMN IF NOT EXISTS timeout_exempted BOOLEAN NOT NULL DEFAULT false;
      |ALTER TABLE rider_assignments ADD COLUMN IF NOT EXISTS timeout_card_used BOOLEAN NOT NULL DEFAULT false;
      |ALTER TABLE rider_assignments ADD COLUMN IF NOT EXISTS overtime_seconds INTEGER NOT NULL DEFAULT 0 CHECK (overtime_seconds >= 0);
      |ALTER TABLE rider_assignments ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT now();
      |
      |CREATE INDEX IF NOT EXISTS rider_assignments_rider_id_idx ON rider_assignments(rider_id);
      |CREATE INDEX IF NOT EXISTS rider_assignments_order_id_idx ON rider_assignments(order_id);
      |""".stripMargin

  def initialize(connection: Connection): IO[Unit] =
    IO.blocking {
      val statement = connection.createStatement()
      try
        val _ = statement.execute(initTableSql)
        ()
      finally statement.close()
    }

end RiderAssignmentTableInitializer
