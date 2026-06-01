package delivery.user.objects

import delivery.shared.objects.{OrderId, UserId, Voucher}

final case class Customer(
    id: UserId,
    name: String,
    phone: String,
    defaultAddress: String,
    walletBalance: Double,
    orderHistoryIds: List[OrderId],
    vouchers: List[Voucher],
    foodiePoints: Int = 0,
    foodieLevel: Int = 1
)
