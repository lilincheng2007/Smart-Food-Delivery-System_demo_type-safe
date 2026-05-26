package delivery.rider.tables.riderassignment

import cats.effect.IO

import java.sql.{Connection, Timestamp}
import java.time.Instant

object RiderAssignmentTable:

  private val upsertSql: String =
    """
      |INSERT INTO rider_assignments (rider_id, order_id, status, completed_at)
      |VALUES (?, ?, ?, ?)
      |ON CONFLICT (rider_id, order_id) DO UPDATE SET
      |  status = EXCLUDED.status,
      |  completed_at = EXCLUDED.completed_at
      |""".stripMargin

  private[rider] def upsert(connection: Connection, riderId: String, orderId: String, status: String): IO[Unit] =
    IO.blocking {
      val statement = connection.prepareStatement(upsertSql)
      try
        statement.setString(1, riderId)
        statement.setString(2, orderId)
        statement.setString(3, status)
        if status == "已送达" || status == "已完成" || status == "已取消" then
          statement.setTimestamp(4, Timestamp.from(Instant.now()))
        else statement.setNull(4, java.sql.Types.TIMESTAMP_WITH_TIMEZONE)
        val _ = statement.executeUpdate()
        ()
      finally statement.close()
    }

end RiderAssignmentTable
