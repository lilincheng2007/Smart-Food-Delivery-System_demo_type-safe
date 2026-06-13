package delivery.rider.tables.riderassignment

import cats.effect.IO
import delivery.rider.tables.RiderAssignmentRecord
import delivery.domain.{OrderId, OrderStatus, RiderId}

import java.sql.{Connection, PreparedStatement, ResultSet, Timestamp}
import java.time.Instant

object RiderAssignmentTable:

  private val upsertSql: String =
    """
      |INSERT INTO rider_assignments (
      |  rider_id, order_id, status, completed_at, deadline_at,
      |  was_timeout, timeout_exempted, timeout_card_used, overtime_seconds
      |)
      |VALUES (?, ?, ?, ?, ?, false, false, false, 0)
      |ON CONFLICT (rider_id, order_id) DO UPDATE SET
      |  status = EXCLUDED.status,
      |  completed_at = COALESCE(rider_assignments.completed_at, EXCLUDED.completed_at),
      |  deadline_at = COALESCE(rider_assignments.deadline_at, EXCLUDED.deadline_at)
      |""".stripMargin

  def upsert(connection: Connection, riderId: RiderId, orderId: OrderId, status: OrderStatus): IO[Unit] =
    IO.blocking {
      val statement = connection.prepareStatement(upsertSql)
      try
        statement.setString(1, riderId)
        statement.setString(2, orderId)
        statement.setString(3, status.toString)
        if OrderStatus.history.contains(status) then statement.setTimestamp(4, Timestamp.from(Instant.now()))
        else statement.setNull(4, java.sql.Types.TIMESTAMP_WITH_TIMEZONE)
        statement.setNull(5, java.sql.Types.TIMESTAMP_WITH_TIMEZONE)
        val _ = statement.executeUpdate()
        ()
      finally statement.close()
    }

  private val completeSql: String =
    """
      |UPDATE rider_assignments
      |SET status = ?,
      |    completed_at = COALESCE(completed_at, ?),
      |    deadline_at = ?,
      |    was_timeout = ?,
      |    timeout_exempted = ?,
      |    timeout_card_used = ?,
      |    overtime_seconds = ?,
      |    updated_at = now()
      |WHERE rider_id = ? AND order_id = ?
      |""".stripMargin

  def completeDelivery(
      connection: Connection,
      riderId: RiderId,
      orderId: OrderId,
      status: OrderStatus,
      completedAt: Instant,
      deadlineAt: Instant,
      wasTimeout: Boolean,
      timeoutExempted: Boolean,
      timeoutCardUsed: Boolean,
      overtimeSeconds: Int
  ): IO[Unit] =
    IO.blocking {
      val statement = connection.prepareStatement(completeSql)
      try
        statement.setString(1, status.toString)
        statement.setTimestamp(2, Timestamp.from(completedAt))
        statement.setTimestamp(3, Timestamp.from(deadlineAt))
        statement.setBoolean(4, wasTimeout)
        statement.setBoolean(5, timeoutExempted)
        statement.setBoolean(6, timeoutCardUsed)
        statement.setInt(7, overtimeSeconds)
        statement.setString(8, riderId)
        statement.setString(9, orderId)
        val _ = statement.executeUpdate()
        ()
      finally statement.close()
    }

  private val markExemptedSql: String =
    """
      |UPDATE rider_assignments
      |SET timeout_exempted = true,
      |    timeout_card_used = true,
      |    updated_at = now()
      |WHERE rider_id = ? AND order_id = ?
      |""".stripMargin

  def markTimeoutExempted(connection: Connection, riderId: RiderId, orderId: OrderId): IO[Unit] =
    IO.blocking {
      val statement = connection.prepareStatement(markExemptedSql)
      try
        statement.setString(1, riderId)
        statement.setString(2, orderId)
        val _ = statement.executeUpdate()
        ()
      finally statement.close()
    }

  private val findSql: String =
    """
      |SELECT rider_id, order_id, status, assigned_at, completed_at, deadline_at,
      |       was_timeout, timeout_exempted, timeout_card_used, overtime_seconds
      |FROM rider_assignments
      |WHERE rider_id = ? AND order_id = ?
      |""".stripMargin

  def find(connection: Connection, riderId: RiderId, orderId: OrderId): IO[Option[RiderAssignmentRecord]] =
    IO.blocking {
      val statement = connection.prepareStatement(findSql)
      try
        statement.setString(1, riderId)
        statement.setString(2, orderId)
        val resultSet = statement.executeQuery()
        try
          if resultSet.next() then Some(readRecord(resultSet)) else None
        finally resultSet.close()
      finally statement.close()
    }

  private val listByRiderSql: String =
    """
      |SELECT rider_id, order_id, status, assigned_at, completed_at, deadline_at,
      |       was_timeout, timeout_exempted, timeout_card_used, overtime_seconds
      |FROM rider_assignments
      |WHERE rider_id = ?
      |""".stripMargin

  def listByRider(connection: Connection, riderId: RiderId): IO[List[RiderAssignmentRecord]] =
    IO.blocking {
      val statement = connection.prepareStatement(listByRiderSql)
      try
        statement.setString(1, riderId)
        val resultSet = statement.executeQuery()
        try
          val builder = List.newBuilder[RiderAssignmentRecord]
          while resultSet.next() do builder += readRecord(resultSet)
          builder.result()
        finally resultSet.close()
      finally statement.close()
    }

  private val listAllSql: String =
    """
      |SELECT rider_id, order_id, status, assigned_at, completed_at, deadline_at,
      |       was_timeout, timeout_exempted, timeout_card_used, overtime_seconds
      |FROM rider_assignments
      |ORDER BY assigned_at DESC
      |""".stripMargin

  def listAll(connection: Connection): IO[List[RiderAssignmentRecord]] =
    IO.blocking {
      val statement = connection.prepareStatement(listAllSql)
      try
        val resultSet = statement.executeQuery()
        try
          val builder = List.newBuilder[RiderAssignmentRecord]
          while resultSet.next() do builder += readRecord(resultSet)
          builder.result()
        finally resultSet.close()
      finally statement.close()
    }

  private def readRecord(resultSet: ResultSet): RiderAssignmentRecord =
    RiderAssignmentRecord(
      riderId = resultSet.getString("rider_id"),
      orderId = resultSet.getString("order_id"),
      status = OrderStatus.fromString(resultSet.getString("status")).getOrElse(OrderStatus.配送中),
      assignedAt = resultSet.getTimestamp("assigned_at").toInstant,
      completedAt = Option(resultSet.getTimestamp("completed_at")).map(_.toInstant),
      deadlineAt = Option(resultSet.getTimestamp("deadline_at")).map(_.toInstant),
      wasTimeout = resultSet.getBoolean("was_timeout"),
      timeoutExempted = resultSet.getBoolean("timeout_exempted"),
      timeoutCardUsed = resultSet.getBoolean("timeout_card_used"),
      overtimeSeconds = resultSet.getInt("overtime_seconds")
    )

end RiderAssignmentTable
