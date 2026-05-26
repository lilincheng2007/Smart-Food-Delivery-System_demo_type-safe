package delivery.merchant.utils

import delivery.merchant.objects.{MerchantAccountPublic, MerchantMeResponse}
import delivery.merchant.tables.MerchantAccountRecord
import delivery.shared.objects.ErrorBody

object MerchantApiSupport:

  def merchantNotFound: ErrorBody = ErrorBody("未找到账号")

  def merchantMeResponse(username: String, account: MerchantAccountRecord): MerchantMeResponse =
    MerchantMeResponse(
      username = username,
      role = "merchant",
      merchantAccount = MerchantAccountPublic(account.role, account.username, account.profile)
    )

end MerchantApiSupport
