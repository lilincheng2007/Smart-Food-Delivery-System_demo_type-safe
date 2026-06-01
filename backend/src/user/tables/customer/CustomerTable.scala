package delivery.user.tables.customer

import cats.effect.IO
import delivery.shared.json.ApiJsonCodecs.given
import delivery.shared.objects.Voucher
import delivery.user.objects.Customer
import io.circe.parser.decode
import io.circe.syntax.*
import org.postgresql.util.PGobject

import java.sql.{Connection, PreparedStatement, ResultSet}

object CustomerTable:

  private val upsertSql: String =
    """
      |INSERT INTO customers (
      |  id, name, phone, default_address, wallet_balance, order_history_ids, vouchers,
      |  foodie_points, foodie_level, updated_at
      |)
      |VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, now())
      |ON CONFLICT (id) DO UPDATE SET
      |  name = EXCLUDED.name,
      |  phone = EXCLUDED.phone,
      |  default_address = EXCLUDED.default_address,
      |  wallet_balance = EXCLUDED.wallet_balance,
      |  order_history_ids = EXCLUDED.order_history_ids,
      |  vouchers = EXCLUDED.vouchers,
      |  updated_at = now()
      |""".stripMargin

  def upsert(connection: Connection, customer: Customer): IO[Customer] =
    IO.blocking {
      val statement = connection.prepareStatement(upsertSql)
      try
        statement.setString(1, customer.id)
        statement.setString(2, customer.name)
        statement.setString(3, customer.phone)
        statement.setString(4, customer.defaultAddress)
        statement.setDouble(5, customer.walletBalance)
        statement.setObject(6, jsonb(customer.orderHistoryIds.asJson.noSpaces))
        statement.setObject(7, jsonb(customer.vouchers.asJson.noSpaces))
        statement.setInt(8, customer.foodiePoints)
        statement.setInt(9, customer.foodieLevel)
        val _ = statement.executeUpdate()
        customer
      finally statement.close()
    }

  private val findSql: String =
    """
      |SELECT id, name, phone, default_address, wallet_balance, order_history_ids, vouchers,
      |       foodie_points, foodie_level
      |FROM customers
      |WHERE id = ?
      |""".stripMargin

  private[user] def findById(connection: Connection, id: String): IO[Option[Customer]] =
    queryOne(connection.prepareStatement(findSql))(_.setString(1, id))

  private val listSql: String =
    "SELECT id, name, phone, default_address, wallet_balance, order_history_ids, vouchers, foodie_points, foodie_level FROM customers ORDER BY created_at ASC"

  private[user] def list(connection: Connection): IO[List[Customer]] =
    IO.blocking {
      val statement = connection.prepareStatement(listSql)
      try
        val resultSet = statement.executeQuery()
        try
          val builder = List.newBuilder[Customer]
          while resultSet.next() do builder += readCustomer(resultSet)
          builder.result()
        finally resultSet.close()
      finally statement.close()
    }

  private def queryOne(statement: PreparedStatement)(bind: PreparedStatement => Unit): IO[Option[Customer]] =
    IO.blocking {
      try
        bind(statement)
        val resultSet = statement.executeQuery()
        try
          if resultSet.next() then Some(readCustomer(resultSet))
          else None
        finally resultSet.close()
      finally statement.close()
    }

  private def readCustomer(resultSet: ResultSet): Customer =
    Customer(
      id = resultSet.getString("id"),
      name = resultSet.getString("name"),
      phone = resultSet.getString("phone"),
      defaultAddress = resultSet.getString("default_address"),
      walletBalance = resultSet.getBigDecimal("wallet_balance").doubleValue(),
      orderHistoryIds = decode[List[String]](resultSet.getString("order_history_ids")).getOrElse(Nil),
      vouchers = decode[List[Voucher]](resultSet.getString("vouchers")).getOrElse(Nil),
      foodiePoints = resultSet.getInt("foodie_points"),
      foodieLevel = resultSet.getInt("foodie_level")
    )

  private def jsonb(value: String): PGobject =
    val pg = PGobject()
    pg.setType("jsonb")
    pg.setValue(value)
    pg

end CustomerTable
