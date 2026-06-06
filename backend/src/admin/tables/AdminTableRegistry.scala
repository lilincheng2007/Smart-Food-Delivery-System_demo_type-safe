package delivery.admin.tables

import cats.effect.IO
import cats.syntax.all.*
import delivery.admin.tables.platformpromotion.PlatformPromotionTableInitializer
import delivery.admin.tables.storeonboarding.StoreOnboardingRequestTableInitializer

import java.sql.Connection

object AdminTableRegistry:
  val StoreOnboardingRequests = "store_onboarding_requests"

  def initialize(connection: Connection): IO[Unit] =
    List(
      StoreOnboardingRequestTableInitializer.initialize(connection),
      PlatformPromotionTableInitializer.initialize(connection)
    ).sequence_.void

end AdminTableRegistry
