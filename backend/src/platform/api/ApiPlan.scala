package delivery.platform.api

import cats.effect.IO

trait ApiPlan[Input, Output]:

  def name: String

  def plan(input: Input): IO[Output]

end ApiPlan
