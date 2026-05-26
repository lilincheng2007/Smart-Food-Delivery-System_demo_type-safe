package delivery.order.tables.order

import cats.effect.IO
import cats.syntax.all.*
import delivery.order.objects.Order
import delivery.order.tables.orderitem.OrderItemTable

import java.sql.{Connection, PreparedStatement, ResultSet}

object OrderTable:

  private val insertSql: String =
    """
      |INSERT INTO orders (
      |  id, customer_id, customer_name, customer_phone, merchant_id, rider_id,
      |  total_amount, delivery_address, status, placed_at, updated_at
      |)
      |VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now())
      |ON CONFLICT (id) DO UPDATE SET
      |  customer_id = EXCLUDED.customer_id,
      |  customer_name = EXCLUDED.customer_name,
      |  customer_phone = EXCLUDED.customer_phone,
      |  merchant_id = EXCLUDED.merchant_id,
      |  rider_id = EXCLUDED.rider_id,
      |  total_amount = EXCLUDED.total_amount,
      |  delivery_address = EXCLUDED.delivery_address,
      |  status = EXCLUDED.status,
      |  placed_at = EXCLUDED.placed_at,
      |  updated_at = now()
      |""".stripMargin

  private[order] def upsert(connection: Connection, order: Order): IO[Order] =
    IO.blocking {
      val statement = connection.prepareStatement(insertSql)
      try
        bindOrder(statement, order)
        val _ = statement.executeUpdate()
        order
      finally statement.close()
    }.flatTap(saved => OrderItemTable.replaceForOrder(connection, saved.id, saved.items))

  private val listSql: String =
    """
      |SELECT id, customer_id, customer_name, customer_phone, merchant_id, rider_id,
      |       total_amount, delivery_address, status, placed_at
      |FROM orders
      |ORDER BY created_at DESC
      |""".stripMargin

  private[order] def list(connection: Connection): IO[List[Order]] =
    queryMany(connection.prepareStatement(listSql))

  private val findByIdSql: String =
    """
      |SELECT id, customer_id, customer_name, customer_phone, merchant_id, rider_id,
      |       total_amount, delivery_address, status, placed_at
      |FROM orders
      |WHERE id = ?
      |""".stripMargin

  private[order] def findById(connection: Connection, id: String): IO[Option[Order]] =
    queryOne(connection.prepareStatement(findByIdSql))(_.setString(1, id))

  private val updateStatusSql: String =
    """
      |UPDATE orders
      |SET status = ?, updated_at = now()
      |WHERE id = ?
      |""".stripMargin

  private[order] def updateStatus(connection: Connection, id: String, status: String): IO[Boolean] =
    IO.blocking {
      val statement = connection.prepareStatement(updateStatusSql)
      try
        statement.setString(1, status)
        statement.setString(2, id)
        statement.executeUpdate() == 1
      finally statement.close()
    }

  private val deleteSql: String =
    "DELETE FROM orders WHERE id = ?"

  private[order] def delete(connection: Connection, id: String): IO[Boolean] =
    IO.blocking {
      val statement = connection.prepareStatement(deleteSql)
      try
        statement.setString(1, id)
        statement.executeUpdate() == 1
      finally statement.close()
    }

  private def bindOrder(statement: PreparedStatement, order: Order): Unit =
    statement.setString(1, order.id)
    statement.setString(2, order.customerId)
    statement.setString(3, order.customerName)
    statement.setString(4, order.customerPhone)
    statement.setString(5, order.merchantId)
    order.riderId match
      case Some(value) => statement.setString(6, value)
      case None        => statement.setNull(6, java.sql.Types.VARCHAR)
    statement.setDouble(7, order.totalAmount)
    statement.setString(8, order.deliveryAddress)
    statement.setString(9, order.status)
    statement.setString(10, order.placedAt)

  private def queryOne(statement: PreparedStatement)(bind: PreparedStatement => Unit): IO[Option[Order]] =
    IO.blocking {
      try
        bind(statement)
        val resultSet = statement.executeQuery()
        try
          if resultSet.next() then Some(readOrder(statement.getConnection, resultSet))
          else None
        finally resultSet.close()
      finally statement.close()
    }

  private def queryMany(statement: PreparedStatement): IO[List[Order]] =
    IO.blocking {
      try
        val resultSet = statement.executeQuery()
        try
          val builder = List.newBuilder[Order]
          while resultSet.next() do builder += readOrder(statement.getConnection, resultSet)
          builder.result()
        finally resultSet.close()
      finally statement.close()
    }

  private def readOrder(connection: Connection, resultSet: ResultSet): Order =
    val id = resultSet.getString("id")
    Order(
      id = id,
      customerId = resultSet.getString("customer_id"),
      customerName = resultSet.getString("customer_name"),
      customerPhone = resultSet.getString("customer_phone"),
      merchantId = resultSet.getString("merchant_id"),
      riderId = Option(resultSet.getString("rider_id")),
      items = OrderItemTable.listByOrderIdSync(connection, id),
      totalAmount = resultSet.getBigDecimal("total_amount").doubleValue(),
      deliveryAddress = resultSet.getString("delivery_address"),
      status = resultSet.getString("status"),
      placedAt = resultSet.getString("placed_at")
    )

end OrderTable
