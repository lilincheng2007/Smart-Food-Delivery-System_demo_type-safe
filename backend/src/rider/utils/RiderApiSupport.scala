package delivery.rider.utils

import delivery.order.objects.Order
import delivery.rider.objects.{RiderAccountPublic, RiderMeResponse}
import delivery.rider.tables.RiderAccountRecord
import delivery.shared.objects.ErrorBody

object RiderApiSupport:

  def riderNotFound: ErrorBody = ErrorBody("未找到账号")

  def riderMeResponse(username: String, account: RiderAccountRecord, availableOrders: List[Order]): RiderMeResponse =
    RiderMeResponse(
      username = username,
      role = "rider",
      riderAccount = RiderAccountPublic(account.role, account.username, account.profile),
      availableOrders = availableOrders
    )

end RiderApiSupport
