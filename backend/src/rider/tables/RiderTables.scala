package delivery.rider.tables

object RiderTables:
  val ServiceState = "rider_service_state"
  val RiderAccounts = "rider_accounts"
  val RiderProfiles = "rider_profiles"
  val RiderAssignments = "rider_assignments"

  val all: List[String] = List(ServiceState, RiderAccounts, RiderProfiles, RiderAssignments)

end RiderTables
