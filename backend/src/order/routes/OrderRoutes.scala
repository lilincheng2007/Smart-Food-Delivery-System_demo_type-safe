package delivery.order.routes

import delivery.order.api.*
import delivery.order.objects.Order
import delivery.order.objects.apiTypes.{CheckoutQuoteResponse, CheckoutResponse, CustomerOrdersResponse, NotificationFeedResponse, NotificationReadStatesResponse, OrderCancelResponse, OrderChatMessagesResponse, OrderChatUnreadCountsResponse, OrderRefundRequestResponse}
import delivery.platform.api.RegisteredAPIMessage
import delivery.platform.api.RegisteredAPIMessage.{apiWithRole, apiWithRoles}
import delivery.platform.json.ApiJsonCodecs.given

object OrderRoutes:
  val apiMessages: List[RegisteredAPIMessage] =
    val registered = List(
    apiWithRole[CustomerOrdersAPIMessage, CustomerOrdersResponse]("customer"),
    apiWithRole[OrderDetailAPIMessage, Order]("customer"),
    apiWithRole[OrderCancelAPIMessage, OrderCancelResponse]("customer"),
    apiWithRole[OrderCompleteAPIMessage, Order]("customer"),
    apiWithRole[OrderRefundRequestAPIMessage, OrderRefundRequestResponse]("customer"),
    apiWithRole[OrderRefundAppealAPIMessage, OrderRefundRequestResponse]("customer"),
    apiWithRole[CustomerRefundImageFileAPIMessage, String]("customer"),
    apiWithRole[CustomerOrderImageFileAPIMessage, String]("customer"),
    apiWithRole[MerchantOrderImageFileAPIMessage, String]("merchant"),
    apiWithRole[RiderOrderImageFileAPIMessage, String]("rider"),
    apiWithRole[CustomerOrderChatMessagesAPIMessage, OrderChatMessagesResponse]("customer"),
    apiWithRole[CustomerSendOrderChatMessageAPIMessage, OrderChatMessagesResponse]("customer"),
    apiWithRole[CustomerOrderChatUnreadCountsAPIMessage, OrderChatUnreadCountsResponse]("customer"),
    apiWithRole[MerchantOrderChatMessagesAPIMessage, OrderChatMessagesResponse]("merchant"),
    apiWithRole[MerchantSendOrderChatMessageAPIMessage, OrderChatMessagesResponse]("merchant"),
    apiWithRole[MerchantOrderChatUnreadCountsAPIMessage, OrderChatUnreadCountsResponse]("merchant"),
    apiWithRole[RiderOrderChatMessagesAPIMessage, OrderChatMessagesResponse]("rider"),
    apiWithRole[RiderSendOrderChatMessageAPIMessage, OrderChatMessagesResponse]("rider"),
    apiWithRole[RiderOrderChatUnreadCountsAPIMessage, OrderChatUnreadCountsResponse]("rider"),
    apiWithRoles[NotificationFeedAPIMessage, NotificationFeedResponse](Set("customer", "merchant", "rider", "admin")),
    apiWithRoles[NotificationReadStatesAPIMessage, NotificationReadStatesResponse](Set("customer", "merchant", "rider", "admin")),
    apiWithRoles[NotificationMarkReadAPIMessage, delivery.domain.apiTypes.OkResponse](Set("customer", "merchant", "rider", "admin")),
    apiWithRoles[NotificationMarkAllReadAPIMessage, delivery.domain.apiTypes.OkResponse](Set("customer", "merchant", "rider", "admin")),
    apiWithRole[CheckoutQuoteAPIMessage, CheckoutQuoteResponse]("customer"),
    apiWithRole[CheckoutAPIMessage, CheckoutResponse]("customer")
    )
    val requiredNames = Set(
      "customerorderchatmessagesapi",
      "customersendorderchatmessageapi",
      "customerorderchatunreadcountsapi",
      "customerorderimagefileapi"
    )
    val registeredNames = registered.map(_.apiName).toSet
    val missing = requiredNames.diff(registeredNames)
    require(missing.isEmpty, s"订单聊天 API 未注册：${missing.toList.sorted.mkString(", ")}")
    registered

end OrderRoutes
