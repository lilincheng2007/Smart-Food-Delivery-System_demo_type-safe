package delivery.shared.tables.storedimage

import cats.effect.IO

import java.sql.Connection

object StoredImageTableInitializer:
  private val initSql: String =
    """
      |CREATE TABLE IF NOT EXISTS stored_images (
      |  id VARCHAR(120) PRIMARY KEY,
      |  scope VARCHAR(32) NOT NULL,
      |  content_type VARCHAR(80) NOT NULL,
      |  bytes BYTEA NOT NULL,
      |  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
      |);
      |CREATE INDEX IF NOT EXISTS stored_images_scope_idx ON stored_images(scope);
      |""".stripMargin

  def initialize(connection: Connection): IO[Unit] =
    IO.blocking {
      val statement = connection.createStatement()
      try
        val _ = statement.execute(initSql)
        ()
      finally statement.close()
    }

end StoredImageTableInitializer
