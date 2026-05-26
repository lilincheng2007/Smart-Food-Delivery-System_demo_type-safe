package delivery.shared.objects

import delivery.merchant.tables.MerchantServiceState
import delivery.order.tables.OrderServiceState
import delivery.rider.tables.RiderServiceState
import delivery.shared.bootstrap.SeedBootstrap
import delivery.user.tables.UserServiceState

final case class DeliveryState(
    user: UserServiceState,
    order: OrderServiceState,
    merchant: MerchantServiceState,
    rider: RiderServiceState
)

object DeliveryState:
  val seed: DeliveryState =
    DeliveryState(
      user = SeedBootstrap.userState,
      order = SeedBootstrap.orderState,
      merchant = SeedBootstrap.merchantState,
      rider = SeedBootstrap.riderState
    )

end DeliveryState
