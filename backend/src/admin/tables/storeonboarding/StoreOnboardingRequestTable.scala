package delivery.admin.tables.storeonboarding

import cats.effect.IO
import delivery.admin.objects.StoreOnboardingRequest
import delivery.platform.json.ApiJsonCodecs.given
import io.circe.parser.decode
import io.circe.syntax.*
import org.postgresql.util.PGobject

import java.sql.{Connection, ResultSet}

object StoreOnboardingRequestTable:

  private val insertSql: String =
    """
      |INSERT INTO store_onboarding_requests (
      |  id, owner_username, store_name, address, description, tags, status, created_at
      |)
      |VALUES (?, ?, ?, ?, ?, ?, 'pending', now())
      |""".stripMargin

  def create(connection: Connection, request: StoreOnboardingRequest): IO[StoreOnboardingRequest] =
    IO.blocking {
      val statement = connection.prepareStatement(insertSql)
      try
        statement.setString(1, request.id)
        statement.setString(2, request.ownerUsername)
        statement.setString(3, request.storeName)
        statement.setString(4, request.address)
        statement.setString(5, request.description)
        statement.setObject(6, jsonb(request.tags.asJson.noSpaces))
        val _ = statement.executeUpdate()
        request
      finally statement.close()
    }

  private val listSql: String =
    """
      |SELECT id, owner_username, store_name, address, description, tags, status, rejection_reason, reviewed_by, created_at, reviewed_at
      |FROM store_onboarding_requests
      |ORDER BY
      |  CASE status WHEN 'pending' THEN 0 WHEN 'rejected' THEN 1 ELSE 2 END,
      |  created_at DESC
      |""".stripMargin

  def list(connection: Connection): IO[List[StoreOnboardingRequest]] =
    IO.blocking {
      val statement = connection.prepareStatement(listSql)
      try
        val resultSet = statement.executeQuery()
        try
          val builder = List.newBuilder[StoreOnboardingRequest]
          while resultSet.next() do builder += read(resultSet)
          builder.result()
        finally resultSet.close()
      finally statement.close()
    }

  private val listByOwnerSql: String =
    """
      |SELECT id, owner_username, store_name, address, description, tags, status, rejection_reason, reviewed_by, created_at, reviewed_at
      |FROM store_onboarding_requests
      |WHERE owner_username = ?
      |ORDER BY
      |  CASE status WHEN 'pending' THEN 0 WHEN 'rejected' THEN 1 ELSE 2 END,
      |  created_at DESC
      |""".stripMargin

  def listByOwner(connection: Connection, ownerUsername: String): IO[List[StoreOnboardingRequest]] =
    IO.blocking {
      val statement = connection.prepareStatement(listByOwnerSql)
      try
        statement.setString(1, ownerUsername)
        val resultSet = statement.executeQuery()
        try
          val builder = List.newBuilder[StoreOnboardingRequest]
          while resultSet.next() do builder += read(resultSet)
          builder.result()
        finally resultSet.close()
      finally statement.close()
    }

  private val findPendingSql: String =
    """
      |SELECT id, owner_username, store_name, address, description, tags, status, rejection_reason, reviewed_by, created_at, reviewed_at
      |FROM store_onboarding_requests
      |WHERE id = ? AND status = 'pending'
      |""".stripMargin

  def findPending(connection: Connection, requestId: String): IO[Option[StoreOnboardingRequest]] =
    IO.blocking {
      val statement = connection.prepareStatement(findPendingSql)
      try
        statement.setString(1, requestId)
        val resultSet = statement.executeQuery()
        try if resultSet.next() then Some(read(resultSet)) else None
        finally resultSet.close()
      finally statement.close()
    }

  private val acceptSql: String =
    """
      |UPDATE store_onboarding_requests
      |SET status = 'accepted', reviewed_by = ?, reviewed_at = now(), rejection_reason = NULL
      |WHERE id = ? AND status = 'pending'
      |""".stripMargin

  def accept(connection: Connection, requestId: String, reviewer: String): IO[Unit] =
    updateReview(connection, acceptSql, requestId, reviewer, None)

  private val rejectSql: String =
    """
      |UPDATE store_onboarding_requests
      |SET status = 'rejected', reviewed_by = ?, reviewed_at = now(), rejection_reason = ?
      |WHERE id = ? AND status = 'pending'
      |""".stripMargin

  def reject(connection: Connection, requestId: String, reviewer: String, reason: String): IO[Unit] =
    updateReview(connection, rejectSql, requestId, reviewer, Some(reason))

  private def updateReview(connection: Connection, sql: String, requestId: String, reviewer: String, reason: Option[String]): IO[Unit] =
    IO.blocking {
      val statement = connection.prepareStatement(sql)
      try
        statement.setString(1, reviewer)
        reason match
          case Some(value) =>
            statement.setString(2, value)
            statement.setString(3, requestId)
          case None =>
            statement.setString(2, requestId)
        val _ = statement.executeUpdate()
        ()
      finally statement.close()
    }

  private def read(resultSet: ResultSet): StoreOnboardingRequest =
    StoreOnboardingRequest(
      id = resultSet.getString("id"),
      ownerUsername = resultSet.getString("owner_username"),
      storeName = resultSet.getString("store_name"),
      address = resultSet.getString("address"),
      description = resultSet.getString("description"),
      tags = decode[List[String]](resultSet.getString("tags")).getOrElse(Nil),
      status = resultSet.getString("status"),
      rejectionReason = Option(resultSet.getString("rejection_reason")),
      reviewedBy = Option(resultSet.getString("reviewed_by")),
      createdAt = resultSet.getTimestamp("created_at").toInstant.toString,
      reviewedAt = Option(resultSet.getTimestamp("reviewed_at")).map(_.toInstant.toString)
    )

  private def jsonb(value: String): PGobject =
    val pg = PGobject()
    pg.setType("jsonb")
    pg.setValue(value)
    pg

end StoreOnboardingRequestTable
