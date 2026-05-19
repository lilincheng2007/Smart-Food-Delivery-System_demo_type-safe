package delivery.shared.db

import cats.effect.IO
import cats.syntax.all.*
import delivery.admin.tables.AdminServiceState
import delivery.merchant.tables.MerchantServiceState
import delivery.order.tables.OrderServiceState
import delivery.rider.tables.RiderServiceState
import delivery.shared.json.ApiJsonCodecs.given
import delivery.shared.objects.DeliveryState
import delivery.user.tables.UserServiceState
import io.circe.parser.decode
import io.circe.syntax.*
import io.circe.{Decoder, Encoder}
import org.postgresql.util.PGobject
import org.typelevel.log4cats.Logger

import javax.sql.DataSource
import java.sql.Connection

object DeliveryStateStore:

  private final case class ServiceTable[S](
      name: String,
      seed: DeliveryState => S,
      read: DeliveryState => S,
      write: (DeliveryState, S) => DeliveryState
  )

  private val userTable: ServiceTable[UserServiceState] =
    ServiceTable("user_service_state", _.user, _.user, (state, value) => state.copy(user = value))

  private val orderTable: ServiceTable[OrderServiceState] =
    ServiceTable("order_service_state", _.order, _.order, (state, value) => state.copy(order = value))

  private val merchantTable: ServiceTable[MerchantServiceState] =
    ServiceTable("merchant_service_state", _.merchant, _.merchant, (state, value) => state.copy(merchant = value))

  private val riderTable: ServiceTable[RiderServiceState] =
    ServiceTable("rider_service_state", _.rider, _.rider, (state, value) => state.copy(rider = value))

  private val adminTable: ServiceTable[AdminServiceState] =
    ServiceTable("admin_service_state", _.admin, _.admin, (state, value) => state.copy(admin = value))

  def migrate(ds: DataSource)(using log: Logger[IO]): IO[Unit] =
    withConnection(ds) { connection =>
      for
        _ <- List(
          createTable(connection, userTable.name),
          createTable(connection, orderTable.name),
          createTable(connection, merchantTable.name),
          createTable(connection, riderTable.name),
          createTable(connection, adminTable.name)
        ).sequence_
        seed <- readLegacySnapshot(connection).map(_.getOrElse(DeliveryState.seed))
        _ <- List(
          insertSeedIfMissing(connection, userTable, seed),
          insertSeedIfMissing(connection, orderTable, seed),
          insertSeedIfMissing(connection, merchantTable, seed),
          insertSeedIfMissing(connection, riderTable, seed),
          insertSeedIfMissing(connection, adminTable, seed)
        ).sequence_
      yield ()
    }.flatTap(_ => log.info("DB schema ready (service state tables)"))

  def load(ds: DataSource): IO[DeliveryState] =
    withConnection(ds) { connection =>
      for
        user <- readTable(connection, userTable)
        order <- readTable(connection, orderTable)
        merchant <- readTable(connection, merchantTable)
        rider <- readTable(connection, riderTable)
        admin <- readTable(connection, adminTable)
      yield DeliveryState(user, order, merchant, rider, admin)
    }

  def save(ds: DataSource)(state: DeliveryState): IO[Unit] =
    withConnection(ds) { connection =>
      List(
        upsertTable(connection, userTable.name, userTable.read(state)),
        upsertTable(connection, orderTable.name, orderTable.read(state)),
        upsertTable(connection, merchantTable.name, merchantTable.read(state)),
        upsertTable(connection, riderTable.name, riderTable.read(state)),
        upsertTable(connection, adminTable.name, adminTable.read(state))
      ).sequence_.void
    }

  private def createTable(connection: Connection, tableName: String): IO[Unit] =
    IO.blocking {
      val statement = connection.createStatement()
      try
        val _ = statement.execute(
          s"""CREATE TABLE IF NOT EXISTS $tableName (
             |  singleton SMALLINT PRIMARY KEY CHECK (singleton = 1),
             |  payload JSONB NOT NULL
             |)""".stripMargin
        )
        ()
      finally statement.close()
    }

  private def insertSeedIfMissing[S: Encoder](connection: Connection, table: ServiceTable[S], seed: DeliveryState): IO[Unit] =
    IO.blocking {
      val statement = connection.prepareStatement(s"SELECT 1 FROM ${table.name} WHERE singleton = 1")
      try
        val resultSet = statement.executeQuery()
        try resultSet.next()
        finally resultSet.close()
      finally statement.close()
    }.flatMap { exists =>
      if exists then IO.unit else upsertTable(connection, table.name, table.seed(seed))
    }

  private def readTable[S: Decoder: Encoder](connection: Connection, table: ServiceTable[S]): IO[S] =
    IO.blocking {
      val statement = connection.prepareStatement(s"SELECT payload::text FROM ${table.name} WHERE singleton = 1")
      try
        val resultSet = statement.executeQuery()
        try
          if resultSet.next() then Some(resultSet.getString(1))
          else None
        finally resultSet.close()
      finally statement.close()
    }.flatMap {
      case Some(raw) =>
        decode[S](raw) match
          case Right(value) => IO.pure(value)
          case Left(err)   => IO.raiseError(new IllegalStateException(s"Failed to decode ${table.name}: ${err.getMessage}", err))
      case None =>
        val value = table.seed(DeliveryState.seed)
        upsertTable(connection, table.name, value).as(value)
    }

  private def readLegacySnapshot(connection: Connection)(using log: Logger[IO]): IO[Option[DeliveryState]] =
    tableExists(connection, "delivery_app_state").flatMap {
      case false => IO.pure(None)
      case true =>
        IO.blocking {
          val statement = connection.prepareStatement("SELECT payload::text FROM delivery_app_state WHERE singleton = 1")
          try
            val resultSet = statement.executeQuery()
            try
              if resultSet.next() then Some(resultSet.getString(1))
              else None
            finally resultSet.close()
          finally statement.close()
        }.flatMap {
          case None => IO.pure(None)
          case Some(raw) =>
            decode[DeliveryState](raw) match
              case Right(value) => IO.pure(Some(value))
              case Left(err) =>
                log.warn(s"Ignoring incompatible legacy delivery_app_state snapshot: ${err.getMessage}").as(None)
        }
    }

  private def tableExists(connection: Connection, tableName: String): IO[Boolean] =
    IO.blocking {
      val resultSet = connection.getMetaData.getTables(null, null, tableName, Array("TABLE"))
      try resultSet.next()
      finally resultSet.close()
    }

  private def upsertTable[S: Encoder](connection: Connection, tableName: String, value: S): IO[Unit] =
    IO.blocking {
      val pg = PGobject()
      pg.setType("jsonb")
      pg.setValue(value.asJson.noSpaces)
      val statement = connection.prepareStatement(
        s"""INSERT INTO $tableName (singleton, payload) VALUES (1, ?)
           |ON CONFLICT (singleton) DO UPDATE SET payload = EXCLUDED.payload""".stripMargin
      )
      try
        statement.setObject(1, pg)
        val _ = statement.executeUpdate()
        ()
      finally statement.close()
    }

  private def withConnection[A](ds: DataSource)(use: Connection => IO[A]): IO[A] =
    IO.blocking(ds.getConnection()).bracket { connection =>
      use(connection)
    } { connection =>
      IO.blocking(connection.close()).handleErrorWith(_ => IO.unit)
    }

end DeliveryStateStore
