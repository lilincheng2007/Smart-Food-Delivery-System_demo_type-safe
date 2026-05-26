package delivery.merchant.tables.merchantaccount

import cats.effect.IO
import delivery.merchant.objects.MerchantProfile
import delivery.merchant.tables.MerchantAccount

import java.sql.{Connection, PreparedStatement, ResultSet}

object MerchantAccountTable:

  private val upsertSql: String =
    """
      |INSERT INTO merchant_accounts (username, role, password, profile_id, owner_name, phone, updated_at)
      |VALUES (?, 'merchant', ?, ?, ?, ?, now())
      |ON CONFLICT (username) DO UPDATE SET
      |  password = EXCLUDED.password,
      |  profile_id = EXCLUDED.profile_id,
      |  owner_name = EXCLUDED.owner_name,
      |  phone = EXCLUDED.phone,
      |  updated_at = now()
      |""".stripMargin

  private[merchant] def upsert(connection: Connection, account: MerchantAccount): IO[MerchantAccount] =
    IO.blocking {
      val statement = connection.prepareStatement(upsertSql)
      try
        statement.setString(1, account.username)
        statement.setString(2, account.password)
        statement.setString(3, account.profile.id)
        statement.setString(4, account.profile.ownerName)
        statement.setString(5, account.profile.phone)
        val _ = statement.executeUpdate()
        account
      finally statement.close()
    }

  private val findSql: String =
    """
      |SELECT username, role, password, profile_id, owner_name, phone
      |FROM merchant_accounts
      |WHERE username = ?
      |""".stripMargin

  private[merchant] def findByUsername(connection: Connection, username: String): IO[Option[MerchantAccount]] =
    queryOne(connection.prepareStatement(findSql))(_.setString(1, username))

  private def queryOne(statement: PreparedStatement)(bind: PreparedStatement => Unit): IO[Option[MerchantAccount]] =
    IO.blocking {
      try
        bind(statement)
        val resultSet = statement.executeQuery()
        try
          if resultSet.next() then Some(readAccount(resultSet))
          else None
        finally resultSet.close()
      finally statement.close()
    }

  private def readAccount(resultSet: ResultSet): MerchantAccount =
    MerchantAccount(
      role = resultSet.getString("role"),
      username = resultSet.getString("username"),
      password = resultSet.getString("password"),
      profile = MerchantProfile(
        id = resultSet.getString("profile_id"),
        ownerName = resultSet.getString("owner_name"),
        phone = resultSet.getString("phone"),
        stores = Nil
      )
    )

end MerchantAccountTable
