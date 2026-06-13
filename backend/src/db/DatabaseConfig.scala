package delivery.db

import cats.effect.IO

final case class DatabaseConfig(
    host: String,
    port: Int,
    databaseName: String,
    user: String,
    password: String,
    maxPoolSize: Int,
    connectionTimeoutMs: Long,
    poolName: String
)

object DatabaseConfig:

  extension (config: DatabaseConfig)
    def jdbcUrl: String =
      s"jdbc:postgresql://${config.host}:${config.port}/${config.databaseName}"

  def fromEnv: IO[DatabaseConfig] =
    IO.delay(
      DatabaseConfig(
        host = sys.env.getOrElse("DB_HOST", "127.0.0.1"),
        port = sys.env.get("DB_PORT").flatMap(_.toIntOption).getOrElse(5432),
        databaseName = sys.env.getOrElse("DB_NAME", "delivery_backend"),
        user = sys.env.getOrElse("DB_USER", "postgres"),
        password = sys.env.getOrElse("DB_PASSWORD", "postgres"),
        maxPoolSize = sys.env.get("DB_MAX_POOL_SIZE").flatMap(_.toIntOption).getOrElse(10),
        connectionTimeoutMs = sys.env.get("DB_CONNECTION_TIMEOUT_MS").flatMap(_.toLongOption).getOrElse(3000L),
        poolName = sys.env.getOrElse("DB_POOL_NAME", "delivery-backend-pool")
      )
    )

end DatabaseConfig
