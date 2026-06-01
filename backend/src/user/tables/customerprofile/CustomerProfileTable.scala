package delivery.user.tables.customerprofile

import cats.effect.IO
import delivery.order.objects.Order
import delivery.shared.json.ApiJsonCodecs.given
import delivery.shared.objects.Voucher
import delivery.user.objects.{CustomerDeliveryContact, CustomerProfile}
import delivery.user.tables.CustomerAccountRecord
import io.circe.parser.decode
import io.circe.syntax.*
import org.postgresql.util.PGobject

import java.sql.{Connection, PreparedStatement, ResultSet}

object CustomerProfileTable:

  private val upsertSql: String =
    """
      |INSERT INTO customer_profiles (
      |  id, username, role, name, phone, default_address, wallet_balance,
      |  vouchers, pending_orders, history_orders, delivery_contacts,
      |  foodie_points, foodie_level, updated_at
      |)
      |VALUES (?, ?, 'customer', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now())
      |ON CONFLICT (id) DO UPDATE SET
      |  username = EXCLUDED.username,
      |  name = EXCLUDED.name,
      |  phone = EXCLUDED.phone,
      |  default_address = EXCLUDED.default_address,
      |  wallet_balance = EXCLUDED.wallet_balance,
      |  vouchers = EXCLUDED.vouchers,
      |  pending_orders = EXCLUDED.pending_orders,
      |  history_orders = EXCLUDED.history_orders,
      |  delivery_contacts = EXCLUDED.delivery_contacts,
      |  foodie_points = EXCLUDED.foodie_points,
      |  foodie_level = EXCLUDED.foodie_level,
      |  updated_at = now()
      |""".stripMargin

  def upsert(connection: Connection, account: CustomerAccountRecord): IO[CustomerAccountRecord] =
    IO.blocking {
      val p = account.profile
      val statement = connection.prepareStatement(upsertSql)
      try
        statement.setString(1, p.id)
        statement.setString(2, account.username)
        statement.setString(3, p.name)
        statement.setString(4, p.phone)
        statement.setString(5, p.defaultAddress)
        statement.setDouble(6, p.walletBalance)
        statement.setObject(7, jsonb(p.vouchers.asJson.noSpaces))
        statement.setObject(8, jsonb(p.pendingOrders.asJson.noSpaces))
        statement.setObject(9, jsonb(p.historyOrders.asJson.noSpaces))
        statement.setObject(10, jsonb(p.deliveryContacts.asJson.noSpaces))
        statement.setInt(11, p.foodiePoints)
        statement.setInt(12, p.foodieLevel)
        val _ = statement.executeUpdate()
        account
      finally statement.close()
    }

  private val findSql: String =
    """
      |SELECT id, username, role, name, phone, default_address, wallet_balance,
      |       vouchers, pending_orders, history_orders, delivery_contacts, foodie_points, foodie_level
      |FROM customer_profiles
      |WHERE username = ?
      |""".stripMargin

  def findByUsername(connection: Connection, username: String): IO[Option[CustomerAccountRecord]] =
    queryOne(connection.prepareStatement(findSql))(_.setString(1, username))

  private def queryOne(statement: PreparedStatement)(bind: PreparedStatement => Unit): IO[Option[CustomerAccountRecord]] =
    IO.blocking {
      try
        bind(statement)
        val resultSet = statement.executeQuery()
        try
          if resultSet.next() then Some(readAccount(resultSet))
          else None
        finally resultSet.close()
      finally statement.close()
    }

  private def readAccount(resultSet: ResultSet): CustomerAccountRecord =
    CustomerAccountRecord(
      role = resultSet.getString("role"),
      username = resultSet.getString("username"),
      password = "",
      profile = CustomerProfile(
        id = resultSet.getString("id"),
        name = resultSet.getString("name"),
        phone = resultSet.getString("phone"),
        defaultAddress = resultSet.getString("default_address"),
        vouchers = decode[List[Voucher]](resultSet.getString("vouchers")).getOrElse(Nil),
        walletBalance = resultSet.getBigDecimal("wallet_balance").doubleValue(),
        pendingOrders = decode[List[Order]](resultSet.getString("pending_orders")).getOrElse(Nil),
        historyOrders = decode[List[Order]](resultSet.getString("history_orders")).getOrElse(Nil),
        deliveryContacts = decode[List[CustomerDeliveryContact]](resultSet.getString("delivery_contacts")).getOrElse(Nil),
        foodiePoints = resultSet.getInt("foodie_points"),
        foodieLevel = resultSet.getInt("foodie_level")
      )
    )

  private def jsonb(value: String): PGobject =
    val pg = PGobject()
    pg.setType("jsonb")
    pg.setValue(value)
    pg

end CustomerProfileTable
