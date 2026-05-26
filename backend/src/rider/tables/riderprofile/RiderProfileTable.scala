package delivery.rider.tables.riderprofile

import cats.effect.IO
import delivery.rider.objects.Rider

import java.sql.{Connection, PreparedStatement, ResultSet}

object RiderProfileTable:

  private val upsertSql: String =
    """
      |INSERT INTO rider_profiles (
      |  id, name, phone, realtime_location, status, total_orders,
      |  rating, station, salary, wallet_balance, updated_at
      |)
      |VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now())
      |ON CONFLICT (id) DO UPDATE SET
      |  name = EXCLUDED.name,
      |  phone = EXCLUDED.phone,
      |  realtime_location = EXCLUDED.realtime_location,
      |  status = EXCLUDED.status,
      |  total_orders = EXCLUDED.total_orders,
      |  rating = EXCLUDED.rating,
      |  station = EXCLUDED.station,
      |  salary = EXCLUDED.salary,
      |  wallet_balance = EXCLUDED.wallet_balance,
      |  updated_at = now()
      |""".stripMargin

  private[rider] def upsert(connection: Connection, rider: Rider, walletBalance: Double): IO[Rider] =
    IO.blocking {
      val statement = connection.prepareStatement(upsertSql)
      try
        bindRider(statement, rider, walletBalance)
        val _ = statement.executeUpdate()
        rider
      finally statement.close()
    }

  private val findSql: String =
    """
      |SELECT id, name, phone, realtime_location, status, total_orders, rating, station, salary
      |FROM rider_profiles
      |WHERE id = ?
      |""".stripMargin

  private[rider] def findById(connection: Connection, id: String): IO[Option[Rider]] =
    queryOne(connection.prepareStatement(findSql))(_.setString(1, id))

  private def queryOne(statement: PreparedStatement)(bind: PreparedStatement => Unit): IO[Option[Rider]] =
    IO.blocking {
      try
        bind(statement)
        val resultSet = statement.executeQuery()
        try
          if resultSet.next() then Some(readRider(resultSet))
          else None
        finally resultSet.close()
      finally statement.close()
    }

  private def bindRider(statement: PreparedStatement, rider: Rider, walletBalance: Double): Unit =
    statement.setString(1, rider.id)
    statement.setString(2, rider.name)
    statement.setString(3, rider.phone)
    statement.setString(4, rider.realtimeLocation)
    statement.setString(5, rider.status)
    statement.setInt(6, rider.totalOrders)
    statement.setDouble(7, rider.rating)
    statement.setString(8, rider.station)
    statement.setDouble(9, rider.salary)
    statement.setDouble(10, walletBalance)

  private def readRider(resultSet: ResultSet): Rider =
    Rider(
      id = resultSet.getString("id"),
      name = resultSet.getString("name"),
      phone = resultSet.getString("phone"),
      realtimeLocation = resultSet.getString("realtime_location"),
      status = resultSet.getString("status"),
      totalOrders = resultSet.getInt("total_orders"),
      rating = resultSet.getBigDecimal("rating").doubleValue(),
      station = resultSet.getString("station"),
      salary = resultSet.getBigDecimal("salary").doubleValue()
    )

end RiderProfileTable
