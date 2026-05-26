package delivery.shared.db

import cats.effect.IO
import cats.syntax.all.*
import delivery.merchant.tables.MerchantTableRegistry
import delivery.merchant.tables.catalogproduct.CatalogProductTable
import delivery.merchant.tables.merchantaccount.MerchantAccountTable
import delivery.merchant.tables.merchantstore.MerchantStoreTable
import delivery.order.tables.OrderTableRegistry
import delivery.order.tables.order.OrderTable
import delivery.rider.tables.RiderTableRegistry
import delivery.rider.tables.rideraccount.RiderAccountTable
import delivery.rider.tables.riderassignment.RiderAssignmentTable
import delivery.shared.bootstrap.{SeedBootstrap, SeedData}
import delivery.user.tables.UserTableRegistry
import delivery.user.tables.authcredential.AuthCredentialTable
import delivery.user.tables.customer.CustomerTable
import delivery.user.tables.customerprofile.CustomerProfileTable
import org.typelevel.log4cats.Logger

import java.sql.Connection
import javax.sql.DataSource

object DeliveryStateStore:

  def migrate(ds: DataSource)(using log: Logger[IO]): IO[Unit] =
    DatabaseSession.withTransactionConnection(ds) { connection =>
      for
        _ <- initializeTables(connection)
        hasData <- normalizedHasData(connection)
        _ <- if hasData then IO.unit else seedNormalized(connection)
      yield ()
    }.flatTap(_ => log.info("DB schema ready (normalized delivery tables)"))

  private def initializeTables(connection: Connection): IO[Unit] =
    List(
      UserTableRegistry.initialize(connection),
      MerchantTableRegistry.initialize(connection),
      OrderTableRegistry.initialize(connection),
      RiderTableRegistry.initialize(connection)
    ).sequence_.void

  private def normalizedHasData(connection: Connection): IO[Boolean] =
    IO.blocking {
      val statement = connection.createStatement()
      try
        val resultSet = statement.executeQuery("SELECT 1 FROM auth_credentials LIMIT 1")
        try resultSet.next()
        finally resultSet.close()
      finally statement.close()
    }

  private def seedNormalized(connection: Connection): IO[Unit] =
    for
      _ <- SeedBootstrap.authCredentials.traverse_(AuthCredentialTable.upsert(connection, _))
      _ <- SeedData.seedCustomers.traverse_(CustomerTable.upsert(connection, _))
      _ <- SeedBootstrap.customerAccounts.traverse_(CustomerProfileTable.upsert(connection, _))
      _ <- SeedBootstrap.merchantAccounts.traverse_(MerchantAccountTable.upsert(connection, _))
      _ <- SeedBootstrap.merchantAccounts.traverse_ { account =>
        account.profile.stores.traverse_(store => MerchantStoreTable.upsert(connection, account.username, store.merchant))
      }
      _ <- SeedData.seedProducts.traverse_(CatalogProductTable.upsert(connection, _))
      _ <- SeedBootstrap.riderAccounts.traverse_(RiderAccountTable.upsert(connection, _))
      _ <- SeedData.seedOrders.traverse_(OrderTable.upsert(connection, _))
      _ <- SeedData.seedOrders.traverse_ { order =>
        order.riderId match
          case Some(riderId) => RiderAssignmentTable.upsert(connection, riderId, order.id, order.status)
          case None          => IO.unit
      }
    yield ()

end DeliveryStateStore
