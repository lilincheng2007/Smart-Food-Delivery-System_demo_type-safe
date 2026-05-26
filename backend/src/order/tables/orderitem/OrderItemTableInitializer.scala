package delivery.order.tables.orderitem

import cats.effect.IO

import java.sql.Connection

object OrderItemTableInitializer:

  private val initTableSql: String =
    """
      |CREATE TABLE IF NOT EXISTS order_items (
      |  id BIGSERIAL PRIMARY KEY,
      |  order_id VARCHAR(80) NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
      |  product_id VARCHAR(80) NOT NULL,
      |  name VARCHAR(160) NOT NULL,
      |  unit_price NUMERIC(12, 2) NOT NULL CHECK (unit_price >= 0),
      |  quantity INTEGER NOT NULL CHECK (quantity > 0)
      |);
      |
      |CREATE INDEX IF NOT EXISTS order_items_order_id_idx ON order_items(order_id);
      |""".stripMargin

  def initialize(connection: Connection): IO[Unit] =
    IO.blocking {
      val statement = connection.createStatement()
      try
        val _ = statement.execute(initTableSql)
        ()
      finally statement.close()
    }

end OrderItemTableInitializer
