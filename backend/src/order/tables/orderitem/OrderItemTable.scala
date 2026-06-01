package delivery.order.tables.orderitem

import cats.effect.IO
import delivery.order.objects.OrderItem

import java.sql.{Connection, ResultSet}

object OrderItemTable:

  private val deleteByOrderSql: String =
    "DELETE FROM order_items WHERE order_id = ?"

  private val insertSql: String =
    """
      |INSERT INTO order_items (order_id, product_id, name, unit_price, quantity)
      |VALUES (?, ?, ?, ?, ?)
      |""".stripMargin

  private[order] def replaceForOrder(connection: Connection, orderId: String, items: List[OrderItem]): IO[Unit] =
    IO.blocking {
      val deleteStatement = connection.prepareStatement(deleteByOrderSql)
      try
        deleteStatement.setString(1, orderId)
        deleteStatement.executeUpdate()
        ()
      finally deleteStatement.close()

      items.foreach { item =>
        val insertStatement = connection.prepareStatement(insertSql)
        try
          insertStatement.setString(1, orderId)
          insertStatement.setString(2, item.productId)
          insertStatement.setString(3, item.name)
          insertStatement.setDouble(4, item.unitPrice)
          insertStatement.setInt(5, item.quantity)
          insertStatement.executeUpdate()
          ()
        finally insertStatement.close()
      }
    }

  private val listByOrderSql: String =
    """
      |SELECT product_id, name, unit_price, quantity
      |FROM order_items
      |WHERE order_id = ?
      |ORDER BY id ASC
      |""".stripMargin

  private[order] def listByOrderId(connection: Connection, orderId: String): IO[List[OrderItem]] =
    IO.blocking(listByOrderIdSync(connection, orderId))

  private[order] def listByOrderIdSync(connection: Connection, orderId: String): List[OrderItem] =
    val statement = connection.prepareStatement(listByOrderSql)
    try
      statement.setString(1, orderId)
      val resultSet = statement.executeQuery()
      try readItems(resultSet)
      finally resultSet.close()
    finally statement.close()

  private[order] def listByOrderIds(connection: Connection, orderIds: List[String]): IO[Map[String, List[OrderItem]]] =
    IO.blocking(listByOrderIdsSync(connection, orderIds))

  private[order] def listByOrderIdsSync(connection: Connection, orderIds: List[String]): Map[String, List[OrderItem]] =
    val distinctOrderIds = orderIds.distinct
    if distinctOrderIds.isEmpty then Map.empty
    else
      val placeholders = List.fill(distinctOrderIds.size)("?").mkString(", ")
      val statement = connection.prepareStatement(
        s"""
           |SELECT order_id, product_id, name, unit_price, quantity
           |FROM order_items
           |WHERE order_id IN ($placeholders)
           |ORDER BY order_id ASC, id ASC
           |""".stripMargin
      )
      try
        distinctOrderIds.zipWithIndex.foreach { case (orderId, index) => statement.setString(index + 1, orderId) }
        val resultSet = statement.executeQuery()
        try readItemsByOrderId(resultSet)
        finally resultSet.close()
      finally statement.close()

  private def readItems(resultSet: ResultSet): List[OrderItem] =
    val builder = List.newBuilder[OrderItem]
    while resultSet.next() do
      builder += readItem(resultSet)
    builder.result()

  private def readItemsByOrderId(resultSet: ResultSet): Map[String, List[OrderItem]] =
    val builder = List.newBuilder[(String, OrderItem)]
    while resultSet.next() do
      builder += resultSet.getString("order_id") -> readItem(resultSet)
    builder.result().groupMap(_._1)(_._2)

  private def readItem(resultSet: ResultSet): OrderItem =
    OrderItem(
      productId = resultSet.getString("product_id"),
      name = resultSet.getString("name"),
      unitPrice = resultSet.getBigDecimal("unit_price").doubleValue(),
      quantity = resultSet.getInt("quantity")
    )

end OrderItemTable
