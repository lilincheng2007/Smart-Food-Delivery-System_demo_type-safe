package delivery.user.tables.authcredential

import cats.effect.IO
import delivery.user.tables.AuthCredentialRecord

import java.sql.{Connection, PreparedStatement, ResultSet}

object AuthCredentialTable:

  private val upsertSql: String =
    """
      |INSERT INTO auth_credentials (role, username, password)
      |VALUES (?, ?, ?)
      |ON CONFLICT (role, username) DO UPDATE SET password = EXCLUDED.password
      |""".stripMargin

  def upsert(connection: Connection, credential: AuthCredentialRecord): IO[AuthCredentialRecord] =
    IO.blocking {
      val statement = connection.prepareStatement(upsertSql)
      try
        statement.setString(1, credential.role)
        statement.setString(2, credential.username)
        statement.setString(3, credential.password)
        val _ = statement.executeUpdate()
        credential
      finally statement.close()
    }

  private val findSql: String =
    """
      |SELECT role, username, password
      |FROM auth_credentials
      |WHERE role = ? AND username = ?
      |""".stripMargin

  private[user] def find(connection: Connection, role: String, username: String): IO[Option[AuthCredentialRecord]] =
    queryOne(connection.prepareStatement(findSql)) { statement =>
      statement.setString(1, role)
      statement.setString(2, username)
    }

  private val listSql: String =
    "SELECT role, username, password FROM auth_credentials ORDER BY created_at ASC"

  private[user] def list(connection: Connection): IO[List[AuthCredentialRecord]] =
    IO.blocking {
      val statement = connection.prepareStatement(listSql)
      try
        val resultSet = statement.executeQuery()
        try
          val builder = List.newBuilder[AuthCredentialRecord]
          while resultSet.next() do builder += readCredential(resultSet)
          builder.result()
        finally resultSet.close()
      finally statement.close()
    }

  private def queryOne(statement: PreparedStatement)(bind: PreparedStatement => Unit): IO[Option[AuthCredentialRecord]] =
    IO.blocking {
      try
        bind(statement)
        val resultSet = statement.executeQuery()
        try
          if resultSet.next() then Some(readCredential(resultSet))
          else None
        finally resultSet.close()
      finally statement.close()
    }

  private def readCredential(resultSet: ResultSet): AuthCredentialRecord =
    AuthCredentialRecord(
      role = resultSet.getString("role"),
      username = resultSet.getString("username"),
      password = resultSet.getString("password")
    )

end AuthCredentialTable
