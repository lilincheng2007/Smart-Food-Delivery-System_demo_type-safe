package delivery.merchant.api

import cats.effect.IO
import delivery.admin.objects.StoreOnboardingRequest
import delivery.admin.tables.storeonboarding.StoreOnboardingRequestTable
import delivery.merchant.tables.merchantaccount.MerchantAccountTable
import delivery.platform.api.{APIWithRoleMessage, HttpApiError}

import java.sql.Connection

final case class MerchantStoreAPIMessage(storeName: String, address: String, description: String, tags: Option[List[String]] = None) extends APIWithRoleMessage[String]:
  override def plan(connection: Connection, username: String): IO[String] =
    val trimmedName = storeName.trim
    val trimmedAddress = address.trim
    val trimmedDescription = description.trim
    val normalizedTags = tags.getOrElse(Nil).map(_.trim).filter(_.nonEmpty).distinct
    val invalidTags = normalizedTags.filterNot(allowedStoreTags.contains)
    if trimmedName.isEmpty || trimmedAddress.isEmpty || trimmedDescription.isEmpty then IO.raiseError(HttpApiError.BadRequest("店铺名称、地址和描述不能为空"))
    else if normalizedTags.isEmpty then IO.raiseError(HttpApiError.BadRequest("请至少选择一个店铺标签"))
    else if invalidTags.nonEmpty then IO.raiseError(HttpApiError.BadRequest(s"不支持的店铺标签：${invalidTags.mkString("、")}"))
    else
      for
        account <- MerchantAccountTable.findByUsername(connection, username)
        _ <- IO.fromOption(account)(HttpApiError.BadRequest("未找到商家账号"))
        nowMillis <- IO.realTime.map(_.toMillis)
        requestId = s"store-req-$nowMillis"
        request = StoreOnboardingRequest(
          id = requestId,
          ownerUsername = username,
          storeName = trimmedName,
          address = trimmedAddress,
          description = trimmedDescription,
          tags = normalizedTags,
          status = "pending",
          rejectionReason = None,
          reviewedBy = None,
          createdAt = "",
          reviewedAt = None
        )
        _ <- StoreOnboardingRequestTable.create(connection, request)
      yield requestId

  private val allowedStoreTags: Set[String] = Set("中餐", "西餐", "饮品甜点", "其它")
