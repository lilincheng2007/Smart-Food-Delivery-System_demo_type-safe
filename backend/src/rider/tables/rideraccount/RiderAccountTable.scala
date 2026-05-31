package delivery.rider.tables.rideraccount

import cats.effect.IO
import cats.syntax.all.*
import delivery.rider.objects.RiderProfile
import delivery.rider.tables.RiderAccountRecord
import delivery.shared.objects.RiderStatus
import delivery.rider.tables.riderprofile.RiderProfileTable

import java.sql.{Connection, PreparedStatement, ResultSet}

object RiderAccountTable:

  private val upsertSql: String =
    """
      |INSERT INTO rider_accounts (username, role, password, rider_id)
      |VALUES (?, 'rider', ?, ?)
      |ON CONFLICT (username) DO UPDATE SET
      |  password = EXCLUDED.password,
      |  rider_id = EXCLUDED.rider_id
      |""".stripMargin

  def upsert(connection: Connection, account: RiderAccountRecord): IO[RiderAccountRecord] =
    RiderProfileTable.upsert(connection, account.profile.rider, account.profile.walletBalance) *>
      IO.blocking {
        val statement = connection.prepareStatement(upsertSql)
        try
          statement.setString(1, account.username)
          statement.setString(2, account.password)
          statement.setString(3, account.profile.rider.id)
          val _ = statement.executeUpdate()
          account
        finally statement.close()
      }

  private val findSql: String =
    """
      |SELECT a.username, a.role, a.password, a.rider_id,
      |       p.name, p.phone, p.realtime_location, p.status, p.total_orders,
      |       p.rating, p.station, p.salary, p.energy_points, p.timeout_card_count,
      |       p.timeout_count, p.timeout_exempted_count, p.wallet_balance
      |FROM rider_accounts a
      |JOIN rider_profiles p ON p.id = a.rider_id
      |WHERE a.username = ?
      |""".stripMargin

  private[rider] def findByUsername(connection: Connection, username: String): IO[Option[RiderAccountRecord]] =
    IO.blocking {
      val statement = connection.prepareStatement(findSql)
      try
        statement.setString(1, username)
        val resultSet = statement.executeQuery()
        try
          if resultSet.next() then Some(readAccount(connection, resultSet))
          else None
        finally resultSet.close()
      finally statement.close()
    }

  private def readAccount(connection: Connection, resultSet: ResultSet): RiderAccountRecord =
    val rider = delivery.rider.objects.Rider(
      id = resultSet.getString("rider_id"),
      name = resultSet.getString("name"),
      phone = resultSet.getString("phone"),
      realtimeLocation = resultSet.getString("realtime_location"),
      status = RiderStatus.fromString(resultSet.getString("status")).getOrElse(RiderStatus.空闲),
      totalOrders = resultSet.getInt("total_orders"),
      rating = resultSet.getBigDecimal("rating").doubleValue(),
      station = resultSet.getString("station"),
      salary = resultSet.getBigDecimal("salary").doubleValue(),
      energyPoints = resultSet.getInt("energy_points"),
      timeoutCardCount = resultSet.getInt("timeout_card_count"),
      timeoutCount = resultSet.getInt("timeout_count"),
      timeoutExemptedCount = resultSet.getInt("timeout_exempted_count")
    )
    RiderAccountRecord(
      role = resultSet.getString("role"),
      username = resultSet.getString("username"),
      password = resultSet.getString("password"),
      profile = RiderProfile(
        rider,
        walletBalance = resultSet.getBigDecimal("wallet_balance").doubleValue(),
        pendingOrders = Nil,
        historyOrders = Nil
      )
    )

end RiderAccountTable
