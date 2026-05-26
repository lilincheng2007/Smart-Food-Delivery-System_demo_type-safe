package delivery.order.tables.checkoutrequest

import cats.effect.IO
import delivery.order.objects.CheckoutRequest
import delivery.shared.json.ApiJsonCodecs.given
import io.circe.syntax.*
import org.postgresql.util.PGobject

import java.sql.Connection

object CheckoutRequestTable:

  private val insertSql: String =
    """
      |INSERT INTO checkout_requests (
      |  customer_username, lines, customer_name, customer_phone, delivery_address, created_order_ids
      |)
      |VALUES (?, ?, ?, ?, ?, ?)
      |RETURNING id
      |""".stripMargin

  private[order] def insert(connection: Connection, username: String, request: CheckoutRequest, createdOrderIds: List[String]): IO[Long] =
    IO.blocking {
      val statement = connection.prepareStatement(insertSql)
      try
        statement.setString(1, username)
        statement.setObject(2, jsonb(request.lines.asJson.noSpaces))
        request.customerName match
          case Some(value) => statement.setString(3, value)
          case None        => statement.setNull(3, java.sql.Types.VARCHAR)
        request.customerPhone match
          case Some(value) => statement.setString(4, value)
          case None        => statement.setNull(4, java.sql.Types.VARCHAR)
        request.deliveryAddress match
          case Some(value) => statement.setString(5, value)
          case None        => statement.setNull(5, java.sql.Types.VARCHAR)
        statement.setObject(6, jsonb(createdOrderIds.asJson.noSpaces))
        val resultSet = statement.executeQuery()
        try
          if resultSet.next() then resultSet.getLong("id")
          else throw new IllegalStateException("Checkout request insert returned no row")
        finally resultSet.close()
      finally statement.close()
    }

  private def jsonb(value: String): PGobject =
    val pg = PGobject()
    pg.setType("jsonb")
    pg.setValue(value)
    pg

end CheckoutRequestTable
