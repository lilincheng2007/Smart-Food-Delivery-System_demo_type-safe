package delivery.user.tables

import cats.effect.IO
import cats.syntax.all.*
import delivery.user.tables.authcredential.AuthCredentialTableInitializer
import delivery.user.tables.customer.CustomerTableInitializer
import delivery.user.tables.customerprofile.CustomerProfileTableInitializer
import delivery.user.tables.customersession.CustomerSessionTableInitializer

import java.sql.Connection

object UserTables:
  val ServiceState = "user_service_state"
  val Credentials = "auth_credentials"
  val Customers = "customers"
  val CustomerProfiles = "customer_profiles"
  val CustomerSessions = "customer_sessions"

  val all: List[String] = List(ServiceState, Credentials, Customers, CustomerProfiles, CustomerSessions)

  def initialize(connection: Connection): IO[Unit] =
    List(
      AuthCredentialTableInitializer.initialize(connection),
      CustomerTableInitializer.initialize(connection),
      CustomerProfileTableInitializer.initialize(connection),
      CustomerSessionTableInitializer.initialize(connection)
    ).sequence_.void

end UserTables
