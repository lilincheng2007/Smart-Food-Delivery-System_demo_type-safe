package delivery.shared.db

import delivery.merchant.tables.MerchantServiceState
import delivery.order.tables.OrderServiceState
import delivery.rider.tables.RiderServiceState
import delivery.shared.objects.DeliveryState
import delivery.user.tables.UserServiceState

object DeliveryStateOps:

  def withUserState(state: DeliveryState, nextUser: UserServiceState): DeliveryState =
    state.copy(user = nextUser)

  def withMerchantState(state: DeliveryState, nextMerchant: MerchantServiceState): DeliveryState =
    state.copy(merchant = nextMerchant)

  def withUserAndMerchantState(
      state: DeliveryState,
      nextUser: UserServiceState,
      nextMerchant: MerchantServiceState
  ): DeliveryState =
    state.copy(user = nextUser, merchant = nextMerchant)

  def withUserAndRiderState(
      state: DeliveryState,
      nextUser: UserServiceState,
      nextRider: RiderServiceState
  ): DeliveryState =
    state.copy(user = nextUser, rider = nextRider)

  def withOrderAndMerchantAndUserState(
      state: DeliveryState,
      nextUser: UserServiceState,
      nextOrder: OrderServiceState,
      nextMerchant: MerchantServiceState
  ): DeliveryState =
    state.copy(user = nextUser, order = nextOrder, merchant = nextMerchant)

  def withOrderAndMerchantAndUserAndRiderState(
      state: DeliveryState,
      nextUser: UserServiceState,
      nextOrder: OrderServiceState,
      nextMerchant: MerchantServiceState,
      nextRider: RiderServiceState
  ): DeliveryState =
    state.copy(user = nextUser, order = nextOrder, merchant = nextMerchant, rider = nextRider)

end DeliveryStateOps
