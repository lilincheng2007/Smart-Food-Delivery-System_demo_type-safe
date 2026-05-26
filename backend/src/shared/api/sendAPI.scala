package delivery.shared.api

import javax.sql.DataSource

object sendAPI:

  def apply[Response](message: APIMessage[Response], ds: DataSource): TaskIO.TaskIO[Response] =
    message.plan(ds)

end sendAPI
