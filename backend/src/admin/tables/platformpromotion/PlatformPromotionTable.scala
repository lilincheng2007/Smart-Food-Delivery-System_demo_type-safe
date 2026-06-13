package delivery.admin.tables.platformpromotion

import cats.effect.IO
import delivery.platform.json.ApiJsonCodecs.given
import delivery.domain.Promotion
import io.circe.parser.decode
import io.circe.syntax.*
import org.postgresql.util.PGobject

import java.sql.Connection

object PlatformPromotionTable:
  private val RowId = "global"

  def get(connection: Connection): IO[List[Promotion]] =
    IO.blocking {
      val statement = connection.prepareStatement("SELECT promotions FROM platform_promotions WHERE id = ?")
      try
        statement.setString(1, RowId)
        val resultSet = statement.executeQuery()
        try
          if resultSet.next() then decode[List[Promotion]](resultSet.getString("promotions")).getOrElse(Nil)
          else Nil
        finally resultSet.close()
      finally statement.close()
    }

  def set(connection: Connection, promotions: List[Promotion]): IO[Unit] =
    IO.blocking {
      val statement = connection.prepareStatement(
        """
          |INSERT INTO platform_promotions (id, promotions, updated_at)
          |VALUES (?, ?, now())
          |ON CONFLICT (id) DO UPDATE SET promotions = EXCLUDED.promotions, updated_at = now()
          |""".stripMargin
      )
      try
        statement.setString(1, RowId)
        statement.setObject(2, jsonb(promotions.asJson.noSpaces))
        val _ = statement.executeUpdate()
        ()
      finally statement.close()
    }

  private def jsonb(value: String): PGobject =
    val pg = PGobject()
    pg.setType("jsonb")
    pg.setValue(value)
    pg

end PlatformPromotionTable

