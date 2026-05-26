package delivery.rider.tables.riderassignment

import cats.effect.IO

import java.sql.Connection

object RiderAssignmentTableInitializer:

  private val initTableSql: String =
    """
      |CREATE TABLE IF NOT EXISTS rider_assignments (
      |  id BIGSERIAL PRIMARY KEY,
      |  rider_id VARCHAR(80) NOT NULL REFERENCES rider_profiles(id) ON DELETE CASCADE,
      |  order_id VARCHAR(80) NOT NULL,
      |  status VARCHAR(32) NOT NULL CHECK (status IN ('配送中', '已送达', '已完成', '已取消')),
      |  assigned_at TIMESTAMPTZ NOT NULL DEFAULT now(),
      |  completed_at TIMESTAMPTZ,
      |  UNIQUE (rider_id, order_id)
      |);
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
