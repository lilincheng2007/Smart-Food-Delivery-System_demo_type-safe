package delivery.platform.api

import delivery.db.DatabaseSession

import javax.sql.DataSource

object sendAPI:

  def apply[Response](message: APIMessage[Response], ds: DataSource): TaskIO.TaskIO[Response] =
    DatabaseSession.withTransactionConnection(ds)(message.plan)

end sendAPI
