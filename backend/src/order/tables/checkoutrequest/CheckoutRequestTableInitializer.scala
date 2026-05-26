package delivery.order.tables.checkoutrequest

import cats.effect.IO

import java.sql.Connection

object CheckoutRequestTableInitializer:

  private val initTableSql: String =
    """
      |CREATE TABLE IF NOT EXISTS checkout_requests (
      |  id BIGSERIAL PRIMARY KEY,
      |  customer_username VARCHAR(80) NOT NULL,
      |  lines JSONB NOT NULL,
      |  customer_name VARCHAR(120),
      |  customer_phone VARCHAR(40),
      |  delivery_address TEXT,
      |  created_order_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
      |  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
      |);
      |""".stripMargin

  def initialize(connection: Connection): IO[Unit] =
    IO.blocking {
      val statement = connection.createStatement()
      try
        val _ = statement.execute(initTableSql)
        ()
      finally statement.close()
    }

end CheckoutRequestTableInitializer
