package delivery.user.utils

import delivery.domain.{ErrorBody, UserRole}
import delivery.user.objects.{CustomerAccountPublic}
import delivery.user.objects.apiTypes.{CustomerMeResponse}
import delivery.user.tables.CustomerAccountRecord

object UserApiSupport:

  def customerNotFound: ErrorBody = ErrorBody("未找到账号")

  def invalidRole: ErrorBody = ErrorBody("无效角色")

  def customerMeResponse(username: String, account: CustomerAccountRecord): CustomerMeResponse =
    CustomerMeResponse(
      username = username,
      role = UserRole.customer,
      customerAccount = CustomerAccountPublic(UserRole.customer, account.username, account.profile)
    )

end UserApiSupport
