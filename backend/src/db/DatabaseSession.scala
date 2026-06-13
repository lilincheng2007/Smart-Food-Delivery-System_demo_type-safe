package delivery.db

import cats.effect.IO

import java.sql.Connection
import javax.sql.DataSource

object DatabaseSession:

  def withTransactionConnection[A](ds: DataSource)(operation: Connection => IO[A]): IO[A] =
    withConnection(ds) { connection =>
      withTransaction(connection)(operation(connection))
    }

  private def withConnection[A](ds: DataSource)(use: Connection => IO[A]): IO[A] =
    IO.blocking(ds.getConnection()).bracket { connection =>
      use(connection)
    } { connection =>
      IO.blocking(connection.close()).handleErrorWith(_ => IO.unit)
    }

  private def withTransaction[A](connection: Connection)(use: IO[A]): IO[A] =
    for
      originalAutoCommit <- IO.blocking(connection.getAutoCommit)
      _ <- IO.blocking(connection.setAutoCommit(false))
      result <- use.attempt
      _ <- result match
        case Right(_) => IO.blocking(connection.commit())
        case Left(_)  => IO.blocking(connection.rollback()).handleErrorWith(_ => IO.unit)
      _ <- IO.blocking(connection.setAutoCommit(originalAutoCommit)).handleErrorWith(_ => IO.unit)
      value <- IO.fromEither(result)
    yield value

end DatabaseSession
