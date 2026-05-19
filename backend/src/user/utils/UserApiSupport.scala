package delivery.user.utils

import delivery.shared.objects.ErrorBody
import delivery.user.objects.{CustomerAccountPublic, CustomerMeResponse}
import delivery.user.tables.CustomerAccount

object UserApiSupport:

  def customerNotFound: ErrorBody = ErrorBody("未找到账号")

  def invalidRole: ErrorBody = ErrorBody("无效角色")

  def customerMeResponse(username: String, account: CustomerAccount): CustomerMeResponse =
    CustomerMeResponse(
      username = username,
      role = "customer",
      customerAccount = CustomerAccountPublic(account.role, account.username, account.profile)
    )

end UserApiSupport
