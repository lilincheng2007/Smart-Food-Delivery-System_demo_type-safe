package delivery.rider.tables

import cats.effect.IO
import cats.syntax.all.*
import delivery.rider.tables.rideraccount.RiderAccountTableInitializer
import delivery.rider.tables.riderassignment.RiderAssignmentTableInitializer
import delivery.rider.tables.riderprofile.RiderProfileTableInitializer

import java.sql.Connection

object RiderTables:
  val ServiceState = "rider_service_state"
  val RiderAccounts = "rider_accounts"
  val RiderProfiles = "rider_profiles"
  val RiderAssignments = "rider_assignments"

  val all: List[String] = List(ServiceState, RiderAccounts, RiderProfiles, RiderAssignments)

  def initialize(connection: Connection): IO[Unit] =
    List(
      RiderProfileTableInitializer.initialize(connection),
      RiderAccountTableInitializer.initialize(connection),
      RiderAssignmentTableInitializer.initialize(connection)
    ).sequence_.void

end RiderTables
