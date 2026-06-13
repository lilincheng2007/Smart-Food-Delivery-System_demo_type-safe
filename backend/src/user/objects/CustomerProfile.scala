package delivery.user.objects

import delivery.order.objects.Order
import delivery.domain.{UserId, Voucher}

final case class CustomerProfile(
    id: UserId,
    name: String,
    phone: String,
    defaultAddress: String,
    vouchers: List[Voucher],
    walletBalance: Double,
    pendingOrders: List[Order],
    historyOrders: List[Order],
    deliveryContacts: List[CustomerDeliveryContact] = Nil,
    foodiePoints: Int = 0,
    foodieLevel: Int = 1
)
