package delivery.rider.utils

import delivery.order.objects.Order
import delivery.rider.objects.{RiderAccountPublic, RiderDeliveryStatus, RiderMeResponse}
import delivery.rider.tables.RiderAccountRecord
import delivery.shared.objects.{ErrorBody, UserRole}

object RiderApiSupport:

  def riderNotFound: ErrorBody = ErrorBody("未找到账号")

  def riderMeResponse(
      username: String,
      account: RiderAccountRecord,
      availableOrders: List[Order],
      deliveryStatuses: List[RiderDeliveryStatus] = Nil
  ): RiderMeResponse =
    RiderMeResponse(
      username = username,
      role = UserRole.rider,
      riderAccount = RiderAccountPublic(UserRole.rider, account.username, account.profile),
      availableOrders = availableOrders,
      deliveryStatuses = deliveryStatuses
    )

end RiderApiSupport
