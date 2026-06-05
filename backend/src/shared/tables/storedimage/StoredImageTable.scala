package delivery.shared.tables.storedimage

import cats.effect.IO

import java.sql.{Connection, ResultSet}

final case class StoredImage(id: String, scope: String, contentType: String, bytes: Array[Byte])

object StoredImageTable:
  private val upsertSql: String =
    """
      |INSERT INTO stored_images (id, scope, content_type, bytes)
      |VALUES (?, ?, ?, ?)
      |ON CONFLICT (id) DO UPDATE SET
      |  scope = EXCLUDED.scope,
      |  content_type = EXCLUDED.content_type,
      |  bytes = EXCLUDED.bytes
      |""".stripMargin

  def upsert(connection: Connection, image: StoredImage): IO[StoredImage] =
    IO.blocking {
      val statement = connection.prepareStatement(upsertSql)
      try
        statement.setString(1, image.id)
        statement.setString(2, image.scope)
        statement.setString(3, image.contentType)
        statement.setBytes(4, image.bytes)
        val _ = statement.executeUpdate()
        image
      finally statement.close()
    }

  private val findSql: String =
    """
      |SELECT id, scope, content_type, bytes
      |FROM stored_images
      |WHERE id = ? AND scope = ?
      |""".stripMargin

  def find(connection: Connection, scope: String, id: String): IO[Option[StoredImage]] =
    IO.blocking {
      val statement = connection.prepareStatement(findSql)
      try
        statement.setString(1, id)
        statement.setString(2, scope)
        val resultSet = statement.executeQuery()
        try
          if resultSet.next() then Some(read(resultSet))
          else None
        finally resultSet.close()
      finally statement.close()
    }

  private def read(resultSet: ResultSet): StoredImage =
    StoredImage(
      id = resultSet.getString("id"),
      scope = resultSet.getString("scope"),
      contentType = resultSet.getString("content_type"),
      bytes = resultSet.getBytes("bytes")
    )

end StoredImageTable
