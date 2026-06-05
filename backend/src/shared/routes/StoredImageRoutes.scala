package delivery.shared.routes

import cats.effect.IO
import delivery.shared.objects.ErrorBody
import delivery.shared.json.ApiJsonCodecs.given
import delivery.shared.tables.storedimage.StoredImageTable
import fs2.Stream
import org.http4s.*
import org.http4s.circe.CirceEntityCodec.given
import org.http4s.dsl.io.*
import org.http4s.headers.`Content-Type`

import java.sql.Connection
import java.util.regex.Pattern
import javax.sql.DataSource

object StoredImageRoutes:
  private val storedImagePattern: Pattern =
    Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|jpeg|png|gif|webp)$", Pattern.CASE_INSENSITIVE)

  def routes(ds: DataSource, scope: String): HttpRoutes[IO] =
    HttpRoutes.of[IO] { case GET -> Root / imageFile =>
      if !storedImagePattern.matcher(imageFile).matches() then BadRequest(ErrorBody("非法文件名"))
      else
        withConnection(ds) { connection =>
          StoredImageTable.find(connection, scope, imageFile)
        }.flatMap {
          case Some(image) =>
            Ok(Stream.emits(image.bytes).covary[IO]).map(_.putHeaders(`Content-Type`(mediaTypeFor(image.contentType, image.id))))
          case None => NotFound()
        }
    }

  private def withConnection[A](ds: DataSource)(use: Connection => IO[A]): IO[A] =
    IO.blocking(ds.getConnection).bracket(use)(connection => IO.blocking(connection.close()).void)

  private def mediaTypeFor(contentType: String, fileName: String): MediaType =
    contentType.toLowerCase match
      case "image/png"  => MediaType.image.png
      case "image/gif"  => MediaType.image.gif
      case "image/webp" => MediaType.image.webp
      case _ =>
        val lower = fileName.toLowerCase
        if lower.endsWith(".png") then MediaType.image.png
        else if lower.endsWith(".gif") then MediaType.image.gif
        else if lower.endsWith(".webp") then MediaType.image.webp
        else MediaType.image.jpeg

end StoredImageRoutes
