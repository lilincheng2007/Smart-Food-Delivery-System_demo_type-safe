# 前后端契约对照表

> 快照日期：2026-06-02。当前项目已按 `sample/` 风格迁移：后端 `XxxAPIMessage.scala`，前端 `src/apis/**/XxxAPI.ts`，请求/响应对象进入 `objects/**/apiTypes/`。

## API 消息对照

| 模块 | 后端目录 | 前端目录 | API 数量 |
|---|---|---|---:|
| ai | `backend/src/ai/api/*APIMessage.scala` | `frontend/src/apis/ai/*API.ts` | 5 |
| user | `backend/src/user/api/*APIMessage.scala` | `frontend/src/apis/user/*API.ts` | 6 |
| merchant | `backend/src/merchant/api/*APIMessage.scala` | `frontend/src/apis/merchant/*API.ts` | 11 |
| order | `backend/src/order/api/*APIMessage.scala` | `frontend/src/apis/order/*API.ts` | 5 |
| rider | `backend/src/rider/api/*APIMessage.scala` | `frontend/src/apis/rider/*API.ts` | 6 |
| 合计 | - | - | 33 |

### ai

- `AISearchAPIMessage.scala` ↔ `AISearchAPI.ts` (`aisearchapi`)
- `AIDietWeeklyReportAPIMessage.scala` ↔ `AIDietWeeklyReportAPI.ts` (`aidietweeklyreportapi`)
- `AIOrderProgressNarrativesAPIMessage.scala` ↔ `AIOrderProgressNarrativesAPI.ts` (`aiorderprogressnarrativesapi`)
- `AIMerchantStoreDescriptionAPIMessage.scala` ↔ `AIMerchantStoreDescriptionAPI.ts` (`aimerchantstoredescriptionapi`)
- `AIMerchantProductDescriptionsAPIMessage.scala` ↔ `AIMerchantProductDescriptionsAPI.ts` (`aimerchantproductdescriptionsapi`)

### user

- `LoginAPIMessage.scala` ↔ `LoginAPI.ts` (`loginapi`)
- `RegisterAPIMessage.scala` ↔ `RegisterAPI.ts` (`registerapi`)
- `CustomerMeAPIMessage.scala` ↔ `CustomerMeAPI.ts` (`customermeapi`)
- `CustomerProfilePatchAPIMessage.scala` ↔ `CustomerProfilePatchAPI.ts` (`customerprofilepatchapi`)
- `CustomerVoucherDiscardAPIMessage.scala` ↔ `CustomerVoucherDiscardAPI.ts` (`customervoucherdiscardapi`)
- `CustomerRechargeAPIMessage.scala` ↔ `CustomerRechargeAPI.ts` (`customerrechargeapi`)

### merchant

- `CatalogAPIMessage.scala` ↔ `CatalogAPI.ts` (`catalogapi`)
- `MerchantMeAPIMessage.scala` ↔ `MerchantMeAPI.ts` (`merchantmeapi`)
- `MerchantProfileAPIMessage.scala` ↔ `MerchantProfileAPI.ts` (`merchantprofileapi`)
- `MerchantStoreAPIMessage.scala` ↔ `MerchantStoreAPI.ts` (`merchantstoreapi`)
- `MerchantStoreDescriptionAPIMessage.scala` ↔ `MerchantStoreDescriptionAPI.ts` (`merchantstoredescriptionapi`)
- `MerchantStoreImageAPIMessage.scala` ↔ `MerchantStoreImageAPI.ts` (`merchantstoreimageapi`)
- `MerchantStoreImageFileAPIMessage.scala` ↔ `MerchantStoreImageFileAPI.ts` (`merchantstoreimagefileapi`)
- `MerchantCreateProductAPIMessage.scala` ↔ `MerchantCreateProductAPI.ts` (`merchantcreateproductapi`)
- `MerchantProductAPIMessage.scala` ↔ `MerchantProductAPI.ts` (`merchantproductapi`)
- `MerchantProductDescriptionsAPIMessage.scala` ↔ `MerchantProductDescriptionsAPI.ts` (`merchantproductdescriptionsapi`)
- `MerchantOrderReadyAPIMessage.scala` ↔ `MerchantOrderReadyAPI.ts` (`merchantorderreadyapi`)

### order

- `CheckoutAPIMessage.scala` ↔ `CheckoutAPI.ts` (`checkoutapi`)
- `CustomerOrdersAPIMessage.scala` ↔ `CustomerOrdersAPI.ts` (`customerordersapi`)
- `OrderDetailAPIMessage.scala` ↔ `OrderDetailAPI.ts` (`orderdetailapi`)
- `OrderCancelAPIMessage.scala` ↔ `OrderCancelAPI.ts` (`ordercancelapi`)
- `OrderCompleteAPIMessage.scala` ↔ `OrderCompleteAPI.ts` (`ordercompleteapi`)

### rider

- `RiderMeAPIMessage.scala` ↔ `RiderMeAPI.ts` (`ridermeapi`)
- `RiderAvailableOrdersAPIMessage.scala` ↔ `RiderAvailableOrdersAPI.ts` (`rideravailableordersapi`)
- `RiderGrabOrderAPIMessage.scala` ↔ `RiderGrabOrderAPI.ts` (`ridergraborderapi`)
- `RiderUpdateOrderStatusAPIMessage.scala` ↔ `RiderUpdateOrderStatusAPI.ts` (`riderupdateorderstatusapi`)
- `RiderRedeemTimeoutCardAPIMessage.scala` ↔ `RiderRedeemTimeoutCardAPI.ts` (`riderredeemtimeoutcardapi`)
- `RiderUseTimeoutCardAPIMessage.scala` ↔ `RiderUseTimeoutCardAPI.ts` (`riderusetimeoutcardapi`)

## Objects 分层对照

### 领域对象根目录

| 模块 | 后端 root objects | 前端 root objects | 说明 |
|---|---|---|---|
| ai | `AIGeneratedProductDescription` | `AIGeneratedProductDescription` | AI 生成结果实体，非 API wrapper |
| user | `Customer`、`CustomerAccountPublic`、`CustomerDeliveryContact`、`CustomerProfile`、`CustomerProfilePatch`、`CustomerWalletTopUp` | 同名 | 顾客资料、联系人、充值输入等系统对象 |
| merchant | `Merchant`、`MerchantAccountPublic`、`MerchantProfile`、`MerchantProfileBody`、`MerchantStoreProfile`、`Product`、`ProductDescriptionPatch` | 同名 | 商家、店铺、商品系统对象 |
| order | `CheckoutLine`、`Order`、`OrderItem` | 同名 | 订单领域对象 |
| rider | `Rider`、`RiderAccountPublic`、`RiderDeliverySettlement`、`RiderDeliveryStatus`、`RiderProfile` | 同名 | 骑手领域对象与配送状态实体 |
| shared | `ErrorBody`、`HealthOk`、`ids`、`Voucher` | 同名 | 共享错误、健康检查、枚举/ID、优惠券 |

### `apiTypes` 请求/响应目录

| 模块 | apiTypes 文件 |
|---|---|
| ai | `AIDietWeeklyReportRequest/Response`、`AIMerchantProductDescriptionsRequest/Response`、`AIMerchantStoreDescriptionRequest/Response`、`AIOrderProgressNarrativesRequest/Response`、`AISearchRequest/Response` |
| user | `CustomerMeResponse`、`CustomerWalletTopUpResponse`、`LoginRequest/Response`、`RegisterRequest`、`MeResponse` |
| merchant | `CatalogResponse`、`CreateProductRequest`、`CreateStoreRequest`、`MerchantMeResponse`、`UpdateProductRequest`、`UpdateStoreImageRequest` |
| order | `CheckoutRequest/Response`、`CustomerOrdersResponse`、`OrderCancelResponse` |
| rider | `RiderAvailableOrdersResponse`、`RiderMeResponse`、`RiderTimeoutCardRedeemResponse`、`RiderUseTimeoutCardResponse` |
| shared | `OkResponse` |

## 页面结构对照

- `frontend/src/pages/CustomerPortal/index.tsx` 装配顾客门户，局部组件在 `CustomerPortal/components/`，工具函数在 `CustomerPortal/functions/`。
- `frontend/src/pages/MerchantConsole/index.tsx` 装配商家控制台，局部组件在 `MerchantConsole/components/`，工具函数在 `MerchantConsole/functions/`。
- `frontend/src/pages/RiderApp/index.tsx` 装配骑手端，局部卡片在 `RiderApp/components/`。
- `frontend/src/pages/Login` 与 `frontend/src/pages/Register` 使用 `components/` 和 `objects/` 分离表单组件与页面常量。
- 登录/注册共用壳组件位于 `frontend/src/components/auth/AuthPageShell.tsx`。

## 当前已知合理例外

- `backend/src/platform/api/APIMessage.scala` 包含 API 注册基础设施，不作为业务 API 拆分对象。
- `frontend/src/apis/shared/sendAPI.ts` 文件名含 `API`，但属于前端 API 基础设施，不参与业务 API 数量对齐。
- 商户图片静态访问路由不是业务 APIMessage，是上传图片后的公共资源访问例外。
