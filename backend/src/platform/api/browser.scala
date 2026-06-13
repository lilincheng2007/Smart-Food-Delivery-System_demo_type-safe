package delivery.platform.api

import cats.effect.IO
import scala.concurrent.duration.FiniteDuration

object browser:

  def nowIO(): TaskIO.TaskIO[Long] =
    IO.realTime.map(_.toMillis)

  def sleepIO(duration: FiniteDuration): TaskIO.TaskIO[Unit] =
    IO.sleep(duration)

end browser
