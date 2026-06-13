package delivery.platform.api

object client:

  def apiFetchIO[A](task: TaskIO.TaskIO[A]): TaskIO.TaskIO[A] =
    task

  def apiGetIO[A](task: TaskIO.TaskIO[A]): TaskIO.TaskIO[A] =
    task

  def apiPostIO[A](task: TaskIO.TaskIO[A]): TaskIO.TaskIO[A] =
    task

  def apiPatchIO[A](task: TaskIO.TaskIO[A]): TaskIO.TaskIO[A] =
    task

  def apiPutIO[A](task: TaskIO.TaskIO[A]): TaskIO.TaskIO[A] =
    task

end client
