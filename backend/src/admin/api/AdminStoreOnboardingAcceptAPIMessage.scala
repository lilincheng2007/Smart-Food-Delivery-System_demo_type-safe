package delivery.admin.api

import cats.effect.IO
import delivery.admin.tables.storeonboarding.StoreOnboardingRequestTable
import delivery.merchant.objects.Merchant
import delivery.merchant.tables.merchantaccount.MerchantAccountTable
import delivery.merchant.tables.merchantstore.MerchantStoreTable
import delivery.platform.api.{APIWithRoleMessage, HttpApiError}
import delivery.domain.MerchantCategory
import delivery.domain.apiTypes.OkResponse

import java.sql.Connection

final case class AdminStoreOnboardingAcceptAPIMessage(requestId: String) extends APIWithRoleMessage[OkResponse]:
  override def plan(connection: Connection, username: String): IO[OkResponse] =
    if requestId.trim.isEmpty then IO.raiseError(HttpApiError.BadRequest("申请 ID 不能为空"))
    else
      for
        request <- StoreOnboardingRequestTable.findPending(connection, requestId.trim)
        onboarding <- IO.fromOption(request)(HttpApiError.NotFound("未找到待审核的店铺申请"))
        account <- MerchantAccountTable.findByUsername(connection, onboarding.ownerUsername)
        merchantAccount <- IO.fromOption(account)(HttpApiError.BadRequest("未找到申请对应的商家账号"))
        nowMillis <- IO.realTime.map(_.toMillis)
        merchant = Merchant(
          id = s"m-local-$nowMillis",
          storeName = onboarding.storeName,
          category = categoryFromTags(onboarding.tags),
          address = onboarding.address,
          phone = merchantAccount.profile.phone,
          rating = 5,
          tags = if onboarding.tags.nonEmpty then onboarding.tags else List("新店"),
          featuredProductIds = Nil,
          imageUrl = None,
          description = onboarding.description
        )
        _ <- MerchantStoreTable.upsert(connection, onboarding.ownerUsername, merchant)
        _ <- StoreOnboardingRequestTable.accept(connection, onboarding.id, username)
      yield OkResponse(ok = true)

  private def categoryFromTags(tags: List[String]): MerchantCategory =
    tags.collectFirst {
      case "中餐"   => MerchantCategory.中餐
      case "西餐"   => MerchantCategory.西餐
      case "饮品甜点" => MerchantCategory.饮品甜点
      case "其它"   => MerchantCategory.其它
    }.getOrElse(MerchantCategory.中餐)
