package delivery.order.tables.order

import cats.effect.IO
import delivery.shared.objects.OrderStatus

import java.sql.Connection

object OrderTableInitializer:

  private val orderStatusSql: String = OrderStatus.values.map(status => s"'${status.toString}'").mkString(", ")

  private val initTableSql: String =
    s"""
      |CREATE TABLE IF NOT EXISTS orders (
      |  id VARCHAR(80) PRIMARY KEY,
      |  customer_id VARCHAR(80) NOT NULL,
      |  customer_name VARCHAR(120) NOT NULL,
      |  customer_phone VARCHAR(40) NOT NULL,
      |  merchant_id VARCHAR(80) NOT NULL,
      |  rider_id VARCHAR(80),
      |  total_amount NUMERIC(12, 2) NOT NULL CHECK (total_amount >= 0),
      |  original_amount NUMERIC(12, 2) NOT NULL DEFAULT 0 CHECK (original_amount >= 0),
      |  discount_amount NUMERIC(12, 2) NOT NULL DEFAULT 0 CHECK (discount_amount >= 0),
      |  payable_amount NUMERIC(12, 2) NOT NULL DEFAULT 0 CHECK (payable_amount >= 0),
      |  used_voucher JSONB,
      |  points_awarded INTEGER NOT NULL DEFAULT 0 CHECK (points_awarded >= 0),
      |  delivery_address TEXT NOT NULL,
      |  status VARCHAR(32) NOT NULL CHECK (status IN ($orderStatusSql)),
      |  placed_at VARCHAR(40) NOT NULL,
      |  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
      |  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
      |);
      |
      |ALTER TABLE orders ADD COLUMN IF NOT EXISTS original_amount NUMERIC(12, 2) NOT NULL DEFAULT 0 CHECK (original_amount >= 0);
      |ALTER TABLE orders ADD COLUMN IF NOT EXISTS discount_amount NUMERIC(12, 2) NOT NULL DEFAULT 0 CHECK (discount_amount >= 0);
      |ALTER TABLE orders ADD COLUMN IF NOT EXISTS payable_amount NUMERIC(12, 2) NOT NULL DEFAULT 0 CHECK (payable_amount >= 0);
      |ALTER TABLE orders ADD COLUMN IF NOT EXISTS used_voucher JSONB;
      |ALTER TABLE orders ADD COLUMN IF NOT EXISTS points_awarded INTEGER NOT NULL DEFAULT 0 CHECK (points_awarded >= 0);
      |ALTER TABLE orders DROP CONSTRAINT IF EXISTS orders_status_check;
      |UPDATE orders SET status = '待骑手接单' WHERE status = '待接单';
      |ALTER TABLE orders ADD CONSTRAINT orders_status_check CHECK (status IN ($orderStatusSql));
      |UPDATE orders SET original_amount = total_amount WHERE original_amount = 0;
      |UPDATE orders SET payable_amount = total_amount WHERE payable_amount = 0;
      |
      |CREATE INDEX IF NOT EXISTS orders_customer_id_idx ON orders(customer_id);
      |CREATE INDEX IF NOT EXISTS orders_merchant_id_idx ON orders(merchant_id);
      |CREATE INDEX IF NOT EXISTS orders_rider_id_idx ON orders(rider_id);
      |CREATE INDEX IF NOT EXISTS orders_status_idx ON orders(status);
      |CREATE INDEX IF NOT EXISTS orders_customer_created_idx ON orders(customer_id, created_at DESC);
      |CREATE INDEX IF NOT EXISTS orders_merchant_created_idx ON orders(merchant_id, created_at DESC);
      |CREATE INDEX IF NOT EXISTS orders_rider_created_idx ON orders(rider_id, created_at DESC);
      |CREATE INDEX IF NOT EXISTS orders_available_idx ON orders(status, rider_id, created_at DESC);
      |""".stripMargin

  def initialize(connection: Connection): IO[Unit] =
    IO.blocking {
      val statement = connection.createStatement()
      try
        val _ = statement.execute(initTableSql)
        ()
      finally statement.close()
    }

end OrderTableInitializer
