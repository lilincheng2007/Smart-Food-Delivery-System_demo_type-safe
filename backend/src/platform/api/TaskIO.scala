package delivery.platform.api

import cats.effect.IO

object TaskIO:

  type TaskIO[+A] = IO[A]

  def runTask[A](task: TaskIO[A]): IO[A] =
    task

  def mapTask[A, B](task: TaskIO[A], f: A => B): TaskIO[B] =
    task.map(f)

  def flatMapTask[A, B](task: TaskIO[A], f: A => TaskIO[B]): TaskIO[B] =
    task.flatMap(f)

  def taskOf[A](value: A): TaskIO[A] =
    IO.pure(value)

end TaskIO
