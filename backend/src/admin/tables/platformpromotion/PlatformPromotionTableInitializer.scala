package delivery.admin.tables.platformpromotion

import cats.effect.IO

import java.sql.Connection

object PlatformPromotionTableInitializer:
  private val initTableSql: String =
    """
      |CREATE TABLE IF NOT EXISTS platform_promotions (
      |  id VARCHAR(40) PRIMARY KEY,
      |  promotions JSONB NOT NULL DEFAULT '[]'::jsonb,
      |  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
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

end PlatformPromotionTableInitializer

