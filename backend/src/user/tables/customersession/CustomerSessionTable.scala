package delivery.user.tables.customersession

import cats.effect.IO

import java.sql.{Connection, Timestamp}
import java.time.Instant

object CustomerSessionTable:

  private val insertSql: String =
    """
      |INSERT INTO customer_sessions (token_hash, role, username, expires_at)
      |VALUES (?, ?, ?, ?)
      |ON CONFLICT (token_hash) DO UPDATE SET
      |  role = EXCLUDED.role,
      |  username = EXCLUDED.username,
      |  expires_at = EXCLUDED.expires_at
      |""".stripMargin

  private[user] def upsert(connection: Connection, tokenHash: String, role: String, username: String, expiresAt: Instant): IO[Unit] =
    IO.blocking {
      val statement = connection.prepareStatement(insertSql)
      try
        statement.setString(1, tokenHash)
        statement.setString(2, role)
        statement.setString(3, username)
        statement.setTimestamp(4, Timestamp.from(expiresAt))
        val _ = statement.executeUpdate()
        ()
      finally statement.close()
    }

  private val deleteSql: String =
    "DELETE FROM customer_sessions WHERE token_hash = ?"

  private[user] def delete(connection: Connection, tokenHash: String): IO[Unit] =
    IO.blocking {
      val statement = connection.prepareStatement(deleteSql)
      try
        statement.setString(1, tokenHash)
        val _ = statement.executeUpdate()
        ()
      finally statement.close()
    }

end CustomerSessionTable
