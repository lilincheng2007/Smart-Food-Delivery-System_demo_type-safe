package delivery.user.tables

object UserTables:
  val ServiceState = "user_service_state"
  val Credentials = "user_credentials"
  val CustomerProfiles = "customer_profiles"
  val CustomerSessions = "customer_sessions"

  val all: List[String] = List(ServiceState, Credentials, CustomerProfiles, CustomerSessions)

end UserTables
