package delivery.merchant.utils

import java.nio.file.{Files, Path, Paths}

object StoreImageUploads:

  private val dirName = "store-uploads"

  /** 进程工作目录下的上传目录（`sbt run` 时一般为 `backend/`） */
  def directory: Path =
    val p = Paths.get(dirName).toAbsolutePath.normalize
    Files.createDirectories(p)
    p

end StoreImageUploads
