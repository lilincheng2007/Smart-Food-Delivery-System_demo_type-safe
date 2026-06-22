# API Inventory

> 更新日期：2026-06-22
>
> 当前项目通过统一 `APIMessage` 网关暴露业务能力，并已通过类型安全结构检查：`45 passed / 0 failed`。

## 1. 统一网关

所有业务 API 默认使用：

```text
POST /api/{apiName}
```

约定：

- 后端业务 API 文件：`backend/src/{module}/api/XxxAPIMessage.scala`
- 前端业务 API 文件：`frontend/src/apis/{module}/XxxAPI.ts`
- 前端文件名等于后端文件名去掉 `Message`
- 业务 API 需在对应 `backend/src/{module}/routes/*Routes.scala` 注册
- 公开静态图片路由是明确例外，不计入 APIMessage 数量

## 2. API 数量概览

| 模块 | API 数量 | 说明 |
|---|---:|---|
| `admin` | 9 | 入驻审核、退款仲裁、订单监控、平台优惠 |
| `ai` | 7 | AI 搜索、饮食周报、订单叙事、评价摘要、商家文案与经营建议 |
| `merchant` | 22 | 商家资料、店铺、商品、营业、订单、退款 |
| `order` | 25 | 顾客订单、结算预估、结算、退款、聊天、通知、图片 |
| `review` | 5 | 评价、回复、投票、评价图片 |
| `rider` | 6 | 骑手主页、可抢单、抢单、配送、免责卡 |
| `user` | 6 | 登录注册、顾客资料、钱包、优惠券 |
| **合计** | **80** | 统一经 `POST /api/{apiName}` 暴露 |

## 3. API 列表

### 3.1 admin

| apiName | 角色 | 后端 API 文件 | 前端 API 文件 | 响应 |
|---|---|---|---|---|
| `adminstoreonboardingrequestsapi` | `admin` | `backend/src/admin/api/AdminStoreOnboardingRequestsAPIMessage.scala` | `frontend/src/apis/admin/AdminStoreOnboardingRequestsAPI.ts` | `StoreOnboardingRequestsResponse` |
| `adminstoreonboardingacceptapi` | `admin` | `backend/src/admin/api/AdminStoreOnboardingAcceptAPIMessage.scala` | `frontend/src/apis/admin/AdminStoreOnboardingAcceptAPI.ts` | `OkResponse` |
| `adminstoreonboardingrejectapi` | `admin` | `backend/src/admin/api/AdminStoreOnboardingRejectAPIMessage.scala` | `frontend/src/apis/admin/AdminStoreOnboardingRejectAPI.ts` | `OkResponse` |
| `adminrefundrequestsapi` | `admin` | `backend/src/admin/api/AdminRefundRequestsAPIMessage.scala` | `frontend/src/apis/admin/AdminRefundRequestsAPI.ts` | `AdminRefundRequestsResponse` |
| `adminrefundacceptapi` | `admin` | `backend/src/admin/api/AdminRefundAcceptAPIMessage.scala` | `frontend/src/apis/admin/AdminRefundAcceptAPI.ts` | `OkResponse` |
| `adminrefundrejectapi` | `admin` | `backend/src/admin/api/AdminRefundRejectAPIMessage.scala` | `frontend/src/apis/admin/AdminRefundRejectAPI.ts` | `OkResponse` |
| `adminordermonitorapi` | `admin` | `backend/src/admin/api/AdminOrderMonitorAPIMessage.scala` | `frontend/src/apis/admin/AdminOrderMonitorAPI.ts` | `AdminOrderMonitorResponse` |
| `adminplatformpromotionsapi` | `admin` | `backend/src/admin/api/AdminPlatformPromotionsAPIMessage.scala` | `frontend/src/apis/admin/AdminPlatformPromotionsAPI.ts` | `PlatformPromotionsResponse` |
| `adminplatformpromotionsupdateapi` | `admin` | `backend/src/admin/api/AdminPlatformPromotionsUpdateAPIMessage.scala` | `frontend/src/apis/admin/AdminPlatformPromotionsUpdateAPI.ts` | `OkResponse` |

### 3.2 ai

| apiName | 角色 | 后端 API 文件 | 前端 API 文件 | 响应 |
|---|---|---|---|---|
| `aisearchapi` | `customer` | `backend/src/ai/api/AISearchAPIMessage.scala` | `frontend/src/apis/ai/AISearchAPI.ts` | `AISearchResponse` |
| `aidietweeklyreportapi` | `customer` | `backend/src/ai/api/AIDietWeeklyReportAPIMessage.scala` | `frontend/src/apis/ai/AIDietWeeklyReportAPI.ts` | `AIDietWeeklyReportResponse` |
| `aiorderprogressnarrativesapi` | `customer` | `backend/src/ai/api/AIOrderProgressNarrativesAPIMessage.scala` | `frontend/src/apis/ai/AIOrderProgressNarrativesAPI.ts` | `AIOrderProgressNarrativesResponse` |
| `aireviewsummaryapi` | `customer` | `backend/src/ai/api/AIReviewSummaryAPIMessage.scala` | `frontend/src/apis/ai/AIReviewSummaryAPI.ts` | `AIReviewSummaryResponse` |
| `aimerchantbusinesssuggestionsapi` | `merchant` | `backend/src/ai/api/AIMerchantBusinessSuggestionsAPIMessage.scala` | `frontend/src/apis/ai/AIMerchantBusinessSuggestionsAPI.ts` | `AIMerchantBusinessSuggestionsResponse` |
| `aimerchantstoredescriptionapi` | `merchant` | `backend/src/ai/api/AIMerchantStoreDescriptionAPIMessage.scala` | `frontend/src/apis/ai/AIMerchantStoreDescriptionAPI.ts` | `AIMerchantStoreDescriptionResponse` |
| `aimerchantproductdescriptionsapi` | `merchant` | `backend/src/ai/api/AIMerchantProductDescriptionsAPIMessage.scala` | `frontend/src/apis/ai/AIMerchantProductDescriptionsAPI.ts` | `AIMerchantProductDescriptionsResponse` |

### 3.3 merchant

| apiName | 角色 | 后端 API 文件 | 前端 API 文件 | 响应 |
|---|---|---|---|---|
| `catalogapi` | public | `backend/src/merchant/api/CatalogAPIMessage.scala` | `frontend/src/apis/merchant/CatalogAPI.ts` | `CatalogResponse` |
| `merchantmeapi` | `merchant` | `backend/src/merchant/api/MerchantMeAPIMessage.scala` | `frontend/src/apis/merchant/MerchantMeAPI.ts` | `MerchantMeResponse` |
| `merchantcreatestoreonboardingrequestapi` | `merchant` | `backend/src/merchant/api/MerchantCreateStoreOnboardingRequestAPIMessage.scala` | `frontend/src/apis/merchant/MerchantCreateStoreOnboardingRequestAPI.ts` | `String` |
| `merchantstoreapi` | `merchant` | `backend/src/merchant/api/MerchantStoreAPIMessage.scala` | `frontend/src/apis/merchant/MerchantStoreAPI.ts` | `String`（兼容旧调用，建议迁移） |
| `merchantstoreonboardingrequestsapi` | `merchant` | `backend/src/merchant/api/MerchantStoreOnboardingRequestsAPIMessage.scala` | `frontend/src/apis/merchant/MerchantStoreOnboardingRequestsAPI.ts` | `StoreOnboardingRequestsResponse` |
| `merchantstoredescriptionapi` | `merchant` | `backend/src/merchant/api/MerchantStoreDescriptionAPIMessage.scala` | `frontend/src/apis/merchant/MerchantStoreDescriptionAPI.ts` | `OkResponse` |
| `merchantstoreannouncementapi` | `merchant` | `backend/src/merchant/api/MerchantStoreAnnouncementAPIMessage.scala` | `frontend/src/apis/merchant/MerchantStoreAnnouncementAPI.ts` | `OkResponse` |
| `merchantbusinesshoursapi` | `merchant` | `backend/src/merchant/api/MerchantBusinessHoursAPIMessage.scala` | `frontend/src/apis/merchant/MerchantBusinessHoursAPI.ts` | `OkResponse` |
| `merchantstorepromotionsapi` | `merchant` | `backend/src/merchant/api/MerchantStorePromotionsAPIMessage.scala` | `frontend/src/apis/merchant/MerchantStorePromotionsAPI.ts` | `OkResponse` |
| `merchantstoreimageapi` | `merchant` | `backend/src/merchant/api/MerchantStoreImageAPIMessage.scala` | `frontend/src/apis/merchant/MerchantStoreImageAPI.ts` | `OkResponse` |
| `merchantstoreimagefileapi` | `merchant` | `backend/src/merchant/api/MerchantStoreImageFileAPIMessage.scala` | `frontend/src/apis/merchant/MerchantStoreImageFileAPI.ts` | `String` |
| `merchantcreateproductapi` | `merchant` | `backend/src/merchant/api/MerchantCreateProductAPIMessage.scala` | `frontend/src/apis/merchant/MerchantCreateProductAPI.ts` | `Product` |
| `merchantproductapi` | `merchant` | `backend/src/merchant/api/MerchantProductAPIMessage.scala` | `frontend/src/apis/merchant/MerchantProductAPI.ts` | `Product` |
| `merchantproductimagefileapi` | `merchant` | `backend/src/merchant/api/MerchantProductImageFileAPIMessage.scala` | `frontend/src/apis/merchant/MerchantProductImageFileAPI.ts` | `Product` |
| `merchantproductdescriptionsapi` | `merchant` | `backend/src/merchant/api/MerchantProductDescriptionsAPIMessage.scala` | `frontend/src/apis/merchant/MerchantProductDescriptionsAPI.ts` | `OkResponse` |
| `merchantorderacceptapi` | `merchant` | `backend/src/merchant/api/MerchantOrderAcceptAPIMessage.scala` | `frontend/src/apis/merchant/MerchantOrderAcceptAPI.ts` | `OkResponse` |
| `merchantorderrejectapi` | `merchant` | `backend/src/merchant/api/MerchantOrderRejectAPIMessage.scala` | `frontend/src/apis/merchant/MerchantOrderRejectAPI.ts` | `OkResponse` |
| `merchantorderreadyapi` | `merchant` | `backend/src/merchant/api/MerchantOrderReadyAPIMessage.scala` | `frontend/src/apis/merchant/MerchantOrderReadyAPI.ts` | `OkResponse` |
| `merchantorderprepdelayapi` | `merchant` | `backend/src/merchant/api/MerchantOrderPrepDelayAPIMessage.scala` | `frontend/src/apis/merchant/MerchantOrderPrepDelayAPI.ts` | `OkResponse` |
| `merchantrefundrequestsapi` | `merchant` | `backend/src/merchant/api/MerchantRefundRequestsAPIMessage.scala` | `frontend/src/apis/merchant/MerchantRefundRequestsAPI.ts` | `MerchantRefundRequestsResponse` |
| `merchantrefundacceptapi` | `merchant` | `backend/src/merchant/api/MerchantRefundAcceptAPIMessage.scala` | `frontend/src/apis/merchant/MerchantRefundAcceptAPI.ts` | `OkResponse` |
| `merchantrefundrejectapi` | `merchant` | `backend/src/merchant/api/MerchantRefundRejectAPIMessage.scala` | `frontend/src/apis/merchant/MerchantRefundRejectAPI.ts` | `OkResponse` |

### 3.4 order

| apiName | 角色 | 后端 API 文件 | 前端 API 文件 | 响应 |
|---|---|---|---|---|
| `customerordersapi` | `customer` | `backend/src/order/api/CustomerOrdersAPIMessage.scala` | `frontend/src/apis/order/CustomerOrdersAPI.ts` | `CustomerOrdersResponse` |
| `orderdetailapi` | `customer` | `backend/src/order/api/OrderDetailAPIMessage.scala` | `frontend/src/apis/order/OrderDetailAPI.ts` | `Order` |
| `ordercancelapi` | `customer` | `backend/src/order/api/OrderCancelAPIMessage.scala` | `frontend/src/apis/order/OrderCancelAPI.ts` | `OrderCancelResponse` |
| `ordercompleteapi` | `customer` | `backend/src/order/api/OrderCompleteAPIMessage.scala` | `frontend/src/apis/order/OrderCompleteAPI.ts` | `Order` |
| `orderrefundrequestapi` | `customer` | `backend/src/order/api/OrderRefundRequestAPIMessage.scala` | `frontend/src/apis/order/OrderRefundRequestAPI.ts` | `OrderRefundRequestResponse` |
| `orderrefundappealapi` | `customer` | `backend/src/order/api/OrderRefundAppealAPIMessage.scala` | `frontend/src/apis/order/OrderRefundAppealAPI.ts` | `OrderRefundRequestResponse` |
| `customerrefundimagefileapi` | `customer` | `backend/src/order/api/CustomerRefundImageFileAPIMessage.scala` | `frontend/src/apis/order/CustomerRefundImageFileAPI.ts` | `String` |
| `customerorderimagefileapi` | `customer` | `backend/src/order/api/CustomerOrderImageFileAPIMessage.scala` | `frontend/src/apis/order/CustomerOrderImageFileAPI.ts` | `String` |
| `merchantorderimagefileapi` | `merchant` | `backend/src/order/api/MerchantOrderImageFileAPIMessage.scala` | `frontend/src/apis/order/MerchantOrderImageFileAPI.ts` | `String` |
| `riderorderimagefileapi` | `rider` | `backend/src/order/api/RiderOrderImageFileAPIMessage.scala` | `frontend/src/apis/order/RiderOrderImageFileAPI.ts` | `String` |
| `customerorderchatmessagesapi` | `customer` | `backend/src/order/api/CustomerOrderChatMessagesAPIMessage.scala` | `frontend/src/apis/order/CustomerOrderChatMessagesAPI.ts` | `OrderChatMessagesResponse` |
| `customersendorderchatmessageapi` | `customer` | `backend/src/order/api/CustomerSendOrderChatMessageAPIMessage.scala` | `frontend/src/apis/order/CustomerSendOrderChatMessageAPI.ts` | `OrderChatMessagesResponse` |
| `customerorderchatunreadcountsapi` | `customer` | `backend/src/order/api/CustomerOrderChatUnreadCountsAPIMessage.scala` | `frontend/src/apis/order/CustomerOrderChatUnreadCountsAPI.ts` | `OrderChatUnreadCountsResponse` |
| `merchantorderchatmessagesapi` | `merchant` | `backend/src/order/api/MerchantOrderChatMessagesAPIMessage.scala` | `frontend/src/apis/order/MerchantOrderChatMessagesAPI.ts` | `OrderChatMessagesResponse` |
| `merchantsendorderchatmessageapi` | `merchant` | `backend/src/order/api/MerchantSendOrderChatMessageAPIMessage.scala` | `frontend/src/apis/order/MerchantSendOrderChatMessageAPI.ts` | `OrderChatMessagesResponse` |
| `merchantorderchatunreadcountsapi` | `merchant` | `backend/src/order/api/MerchantOrderChatUnreadCountsAPIMessage.scala` | `frontend/src/apis/order/MerchantOrderChatUnreadCountsAPI.ts` | `OrderChatUnreadCountsResponse` |
| `riderorderchatmessagesapi` | `rider` | `backend/src/order/api/RiderOrderChatMessagesAPIMessage.scala` | `frontend/src/apis/order/RiderOrderChatMessagesAPI.ts` | `OrderChatMessagesResponse` |
| `ridersendorderchatmessageapi` | `rider` | `backend/src/order/api/RiderSendOrderChatMessageAPIMessage.scala` | `frontend/src/apis/order/RiderSendOrderChatMessageAPI.ts` | `OrderChatMessagesResponse` |
| `riderorderchatunreadcountsapi` | `rider` | `backend/src/order/api/RiderOrderChatUnreadCountsAPIMessage.scala` | `frontend/src/apis/order/RiderOrderChatUnreadCountsAPI.ts` | `OrderChatUnreadCountsResponse` |
| `notificationfeedapi` | `customer/merchant/rider/admin` | `backend/src/order/api/NotificationFeedAPIMessage.scala` | `frontend/src/apis/order/NotificationFeedAPI.ts` | `NotificationFeedResponse` |
| `notificationreadstatesapi` | `customer/merchant/rider/admin` | `backend/src/order/api/NotificationReadStatesAPIMessage.scala` | `frontend/src/apis/order/NotificationReadStatesAPI.ts` | `NotificationReadStatesResponse` |
| `notificationmarkreadapi` | `customer/merchant/rider/admin` | `backend/src/order/api/NotificationMarkReadAPIMessage.scala` | `frontend/src/apis/order/NotificationMarkReadAPI.ts` | `OkResponse` |
| `notificationmarkallreadapi` | `customer/merchant/rider/admin` | `backend/src/order/api/NotificationMarkAllReadAPIMessage.scala` | `frontend/src/apis/order/NotificationMarkAllReadAPI.ts` | `OkResponse` |
| `checkoutquoteapi` | `customer` | `backend/src/order/api/CheckoutQuoteAPIMessage.scala` | `frontend/src/apis/order/CheckoutQuoteAPI.ts` | `CheckoutQuoteResponse` |
| `checkoutapi` | `customer` | `backend/src/order/api/CheckoutAPIMessage.scala` | `frontend/src/apis/order/CheckoutAPI.ts` | `CheckoutResponse` |

### 3.5 review

| apiName | 角色 | 后端 API 文件 | 前端 API 文件 | 响应 |
|---|---|---|---|---|
| `merchantreviewsapi` | public | `backend/src/review/api/MerchantReviewsAPIMessage.scala` | `frontend/src/apis/review/MerchantReviewsAPI.ts` | `MerchantReviewsResponse` |
| `customersubmitorderreviewapi` | `customer` | `backend/src/review/api/CustomerSubmitOrderReviewAPIMessage.scala` | `frontend/src/apis/review/CustomerSubmitOrderReviewAPI.ts` | `OkResponse` |
| `customerreviewvoteapi` | `customer` | `backend/src/review/api/CustomerReviewVoteAPIMessage.scala` | `frontend/src/apis/review/CustomerReviewVoteAPI.ts` | `OkResponse` |
| `merchantreviewreplyapi` | `merchant` | `backend/src/review/api/MerchantReviewReplyAPIMessage.scala` | `frontend/src/apis/review/MerchantReviewReplyAPI.ts` | `OkResponse` |
| `customerreviewimagefileapi` | `customer` | `backend/src/review/api/CustomerReviewImageFileAPIMessage.scala` | `frontend/src/apis/review/CustomerReviewImageFileAPI.ts` | `String` |

### 3.6 rider

| apiName | 角色 | 后端 API 文件 | 前端 API 文件 | 响应 |
|---|---|---|---|---|
| `ridermeapi` | `rider` | `backend/src/rider/api/RiderMeAPIMessage.scala` | `frontend/src/apis/rider/RiderMeAPI.ts` | `RiderMeResponse` |
| `rideravailableordersapi` | `rider` | `backend/src/rider/api/RiderAvailableOrdersAPIMessage.scala` | `frontend/src/apis/rider/RiderAvailableOrdersAPI.ts` | `RiderAvailableOrdersResponse` |
| `ridergraborderapi` | `rider` | `backend/src/rider/api/RiderGrabOrderAPIMessage.scala` | `frontend/src/apis/rider/RiderGrabOrderAPI.ts` | `OkResponse` |
| `riderupdateorderstatusapi` | `rider` | `backend/src/rider/api/RiderUpdateOrderStatusAPIMessage.scala` | `frontend/src/apis/rider/RiderUpdateOrderStatusAPI.ts` | `RiderDeliverySettlement` |
| `riderredeemtimeoutcardapi` | `rider` | `backend/src/rider/api/RiderRedeemTimeoutCardAPIMessage.scala` | `frontend/src/apis/rider/RiderRedeemTimeoutCardAPI.ts` | `RiderTimeoutCardRedeemResponse` |
| `riderusetimeoutcardapi` | `rider` | `backend/src/rider/api/RiderUseTimeoutCardAPIMessage.scala` | `frontend/src/apis/rider/RiderUseTimeoutCardAPI.ts` | `RiderUseTimeoutCardResponse` |

### 3.7 user

| apiName | 角色 | 后端 API 文件 | 前端 API 文件 | 响应 |
|---|---|---|---|---|
| `loginapi` | public | `backend/src/user/api/LoginAPIMessage.scala` | `frontend/src/apis/user/LoginAPI.ts` | `LoginResponse` |
| `registerapi` | public | `backend/src/user/api/RegisterAPIMessage.scala` | `frontend/src/apis/user/RegisterAPI.ts` | `OkResponse` |
| `customermeapi` | `customer` | `backend/src/user/api/CustomerMeAPIMessage.scala` | `frontend/src/apis/user/CustomerMeAPI.ts` | `CustomerMeResponse` |
| `customerprofilepatchapi` | `customer` | `backend/src/user/api/CustomerProfilePatchAPIMessage.scala` | `frontend/src/apis/user/CustomerProfilePatchAPI.ts` | `OkResponse` |
| `customervoucherdiscardapi` | `customer` | `backend/src/user/api/CustomerVoucherDiscardAPIMessage.scala` | `frontend/src/apis/user/CustomerVoucherDiscardAPI.ts` | `OkResponse` |
| `customerrechargeapi` | `customer` | `backend/src/user/api/CustomerRechargeAPIMessage.scala` | `frontend/src/apis/user/CustomerRechargeAPI.ts` | `CustomerWalletTopUpResponse` |

## 4. Contract 目录

| 类型 | 后端 | 前端 |
|---|---|---|
| 领域对象 | `backend/src/{module}/objects/` | `frontend/src/objects/{module}/` |
| 请求/响应对象 | `backend/src/{module}/objects/apiTypes/` | `frontend/src/objects/{module}/apiTypes/` |
| API 基础设施 | `backend/src/platform/api/` | `frontend/src/apis/shared/` |
| 共享 ID/枚举 | `backend/src/domain/ids.scala` | `frontend/src/objects/shared/ids.ts` |

## 5. 公开静态资源路由

以下路由不属于业务 APIMessage，不参与前后端 API 文件数量对齐：

| HTTP路由 | 说明 |
|---|---|
| `GET /api/merchant/store-images/{fileName}` | 店铺图片 |
| `GET /api/merchant/product-images/{fileName}` | 商品图片 |
| `GET /api/orders/refund-images/{fileName}` | 订单图片，当前包含退款凭证图片、订单聊天图片和订单备注图片 |
| `GET /api/reviews/images/{fileName}` | 评价图片 |

## 6. 对齐验证

已执行：

```bash
.codebuddy/skills/type-safety-audit/scripts/check-type-safety.sh /Users/leonli/Desktop/Type-safe_project
```

结果：`45 PASS / 0 FAIL`。
