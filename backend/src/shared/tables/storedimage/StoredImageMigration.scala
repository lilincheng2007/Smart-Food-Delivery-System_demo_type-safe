package delivery.shared.tables.storedimage

import cats.effect.IO
import cats.syntax.all.*

import java.nio.file.{Files, Path}
import java.sql.Connection
import java.util.regex.Pattern
import scala.jdk.CollectionConverters.*

object StoredImageMigration:
  private val imageNamePattern: Pattern =
    Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|jpeg|png|gif|webp)$", Pattern.CASE_INSENSITIVE)

  def importDirectory(connection: Connection, scope: String, directory: Path): IO[Unit] =
    IO.blocking {
      if Files.isDirectory(directory) then
        Files.list(directory).iterator().asScala.toList
      else Nil
    }.flatMap { paths =>
      paths.traverse_ { path =>
        val fileName = path.getFileName.toString
        if Files.isRegularFile(path) && imageNamePattern.matcher(fileName).matches() then
          IO.blocking(Files.readAllBytes(path)).flatMap { bytes =>
            StoredImageTable.upsert(connection, StoredImage(fileName, scope, contentTypeFor(fileName), bytes)).void
          }
        else IO.unit
      }
    }

  def contentTypeFor(fileName: String): String =
    val lower = fileName.toLowerCase
    if lower.endsWith(".png") then "image/png"
    else if lower.endsWith(".gif") then "image/gif"
    else if lower.endsWith(".webp") then "image/webp"
    else "image/jpeg"

end StoredImageMigration
