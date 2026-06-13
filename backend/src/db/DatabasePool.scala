package delivery.db

import cats.effect.{IO, Resource}
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import org.typelevel.log4cats.Logger

object DatabasePool:

  def resource(using log: Logger[IO]): Resource[IO, HikariDataSource] =
    Resource.eval(DatabaseConfig.fromEnv).flatMap(resourceFor)

  def resourceFor(cfg: DatabaseConfig)(using log: Logger[IO]): Resource[IO, HikariDataSource] =
    Resource.make(acquire(cfg))(release).evalTap { _ =>
      log.info(s"PostgreSQL pool: ${cfg.jdbcUrl} (user=${cfg.user}, maxPool=${cfg.maxPoolSize})")
    }

  private def acquire(cfg: DatabaseConfig): IO[HikariDataSource] =
    IO.blocking {
      Class.forName("org.postgresql.Driver")
      val h = HikariConfig()
      h.setJdbcUrl(cfg.jdbcUrl)
      h.setUsername(cfg.user)
      h.setPassword(cfg.password)
      h.setMaximumPoolSize(cfg.maxPoolSize)
      h.setConnectionTimeout(cfg.connectionTimeoutMs)
      h.setPoolName(cfg.poolName)
      HikariDataSource(h)
    }

  private def release(ds: HikariDataSource): IO[Unit] =
    IO.blocking(ds.close()).handleErrorWith(_ => IO.unit)

end DatabasePool
