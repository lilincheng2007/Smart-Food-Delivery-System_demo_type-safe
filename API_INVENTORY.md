# API Inventory

本次重构后的原则：

- 后端运行时只有一个服务：`backend/src/Main.scala`
- 逻辑目录按领域拆分：`admin / merchant / order / rider / user`
- 一个 API 一个文件
- 一个 API contract object 一个文件
- 前后端 contract object 文件名一一对应

## API 列表

| HTTP | 路径 | 后端 API 文件 | 请求类型 | 响应类型 |
|------|------|---------------|----------|----------|
| `GET` | `/api/health` | `backend/src/admin/api/HealthApi.scala` | `HealthOk` 无请求体 | `HealthOk` |
| `POST` | `/api/auth/login` | `backend/src/user/api/LoginApi.scala` | `LoginRequest` | `LoginResponse` |
| `POST` | `/api/auth/register` | `backend/src/user/api/RegisterApi.scala` | `RegisterRequest` | `OkResponse` |
| `GET` | `/api/auth/me` for customer | `backend/src/user/api/CustomerMeApi.scala` | 无请求体 | `CustomerMeResponse` |
| `GET` | `/api/auth/me` for merchant | `backend/src/merchant/api/MerchantMeApi.scala` | 无请求体 | `MerchantMeResponse` |
| `GET` | `/api/auth/me` for rider | `backend/src/rider/api/RiderMeApi.scala` | 无请求体 | `RiderMeResponse` |
| `GET` | `/api/auth/me` for admin | `backend/src/admin/api/AdminMeApi.scala` | 无请求体 | `AdminMeResponse` |
| `PATCH` | `/api/delivery/me/customer/profile` | `backend/src/user/api/CustomerProfilePatchApi.scala` | `CustomerProfilePatch` | `OkResponse` |
| `GET` | `/api/delivery/catalog` | `backend/src/merchant/api/CatalogApi.scala` | 无请求体 | `CatalogResponse` |
| `PUT` | `/api/delivery/me/merchant/profile` | `backend/src/merchant/api/MerchantProfileApi.scala` | `MerchantProfileBody` | `OkResponse` |
| `POST` | `/api/delivery/me/merchant/stores` | `backend/src/merchant/api/MerchantStoreApi.scala` | `CreateStoreRequest` | `CreateStoreResponse` |
| `PUT` | `/api/merchant/me/stores/:merchantId/image` | `backend/src/merchant/api/MerchantStoreImageApi.scala` | `UpdateStoreImageRequest` | `OkResponse` |
| `POST` | `/api/merchant/me/stores/:merchantId/image-file` | `backend/src/merchant/api/MerchantStoreImageFileApi.scala` | `multipart/form-data` 字段 `file` | `StoreImageUploadResponse` |
| `GET` | `/api/merchant/store-images/:fileName` | `MerchantRoutes`（静态文件） | 无 | 图片字节流 |
| `POST` | `/api/delivery/me/customer/checkout` | `backend/src/order/api/CheckoutApi.scala` | `CheckoutRequest` | `CheckoutResponse` |
| `GET` | `/api/delivery/overview` | `backend/src/admin/api/OverviewApi.scala` | 无请求体 | `OverviewResponse` |
| `GET` | `/api/delivery/orders-panel` | `backend/src/admin/api/OrdersPanelApi.scala` | 无请求体 | `OrdersPanelResponse` |
| `GET` | `/api/delivery/platform-meta` | `backend/src/admin/api/PlatformMetaApi.scala` | 无请求体 | `PlatformMetaResponse` |

## Contract 对应文件

### User

| 类型 | 后端 | 前端 |
|------|------|------|
| `LoginRequest` | `backend/src/user/objects/LoginRequest.scala` | `frontend/src/user/objects/LoginRequest.ts` |
| `RegisterRequest` | `backend/src/user/objects/RegisterRequest.scala` | `frontend/src/user/objects/RegisterRequest.ts` |
| `LoginResponse` | `backend/src/user/objects/LoginResponse.scala` | `frontend/src/user/objects/LoginResponse.ts` |
| `CustomerProfilePatch` | `backend/src/user/objects/CustomerProfilePatch.scala` | `frontend/src/user/objects/CustomerProfilePatch.ts` |
| `CustomerAccountPublic` | `backend/src/user/objects/CustomerAccountPublic.scala` | `frontend/src/user/objects/CustomerAccountPublic.ts` |
| `CustomerMeResponse` | `backend/src/user/objects/CustomerMeResponse.scala` | `frontend/src/user/objects/CustomerMeResponse.ts` |

### Order

| 类型 | 后端 | 前端 |
|------|------|------|
| `CheckoutLine` | `backend/src/order/objects/CheckoutLine.scala` | `frontend/src/order/objects/CheckoutLine.ts` |
| `CheckoutRequest` | `backend/src/order/objects/CheckoutRequest.scala` | `frontend/src/order/objects/CheckoutRequest.ts` |
| `CheckoutResponse` | `backend/src/order/objects/CheckoutResponse.scala` | `frontend/src/order/objects/CheckoutResponse.ts` |

### Merchant

| 类型 | 后端 | 前端 |
|------|------|------|
| `MerchantProfileBody` | `backend/src/merchant/objects/MerchantProfileBody.scala` | `frontend/src/merchant/objects/MerchantProfileBody.ts` |
| `CreateStoreRequest` | `backend/src/merchant/objects/CreateStoreRequest.scala` | `frontend/src/merchant/objects/CreateStoreRequest.ts` |
| `CreateStoreResponse` | `backend/src/merchant/objects/CreateStoreResponse.scala` | `frontend/src/merchant/objects/CreateStoreResponse.ts` |
| `UpdateStoreImageRequest` | `backend/src/merchant/objects/UpdateStoreImageRequest.scala` | `frontend/src/objects/merchant/UpdateStoreImageRequest.ts` |
| `StoreImageUploadResponse` | `backend/src/merchant/objects/StoreImageUploadResponse.scala` | `frontend/src/objects/merchant/StoreImageUploadResponse.ts` |
| `CatalogResponse` | `backend/src/merchant/objects/CatalogResponse.scala` | `frontend/src/merchant/objects/CatalogResponse.ts` |
| `MerchantAccountPublic` | `backend/src/merchant/objects/MerchantAccountPublic.scala` | `frontend/src/merchant/objects/MerchantAccountPublic.ts` |
| `MerchantMeResponse` | `backend/src/merchant/objects/MerchantMeResponse.scala` | `frontend/src/merchant/objects/MerchantMeResponse.ts` |

### Rider

| 类型 | 后端 | 前端 |
|------|------|------|
| `RiderAccountPublic` | `backend/src/rider/objects/RiderAccountPublic.scala` | `frontend/src/rider/objects/RiderAccountPublic.ts` |
| `RiderMeResponse` | `backend/src/rider/objects/RiderMeResponse.scala` | `frontend/src/rider/objects/RiderMeResponse.ts` |

### Admin

| 类型 | 后端 | 前端 |
|------|------|------|
| `AdminAccountPublic` | `backend/src/admin/objects/AdminAccountPublic.scala` | `frontend/src/admin/objects/AdminAccountPublic.ts` |
| `AdminMeResponse` | `backend/src/admin/objects/AdminMeResponse.scala` | `frontend/src/admin/objects/AdminMeResponse.ts` |
| `OverviewResponse` | `backend/src/admin/objects/OverviewResponse.scala` | `frontend/src/admin/objects/OverviewResponse.ts` |
| `OrdersPanelResponse` | `backend/src/admin/objects/OrdersPanelResponse.scala` | `frontend/src/admin/objects/OrdersPanelResponse.ts` |
| `PlatformMetaResponse` | `backend/src/admin/objects/PlatformMetaResponse.scala` | `frontend/src/admin/objects/PlatformMetaResponse.ts` |

### Shared

| 类型 | 后端 | 前端 |
|------|------|------|
| `OkResponse` | `backend/src/shared/objects/OkResponse.scala` | `frontend/src/shared/objects/OkResponse.ts` |
| `ErrorBody` | `backend/src/shared/objects/ErrorBody.scala` | `frontend/src/shared/objects/ErrorBody.ts` |
| `HealthOk` | `backend/src/shared/objects/HealthOk.scala` | `frontend/src/shared/objects/HealthOk.ts` |

## 前端 API 函数文件

| API 函数 | 文件 |
|----------|------|
| `loginIO` | `frontend/src/user/api/LoginApi.ts` |
| `registerIO` | `frontend/src/user/api/RegisterApi.ts` |
| `fetchCustomerMeIO` | `frontend/src/user/api/CustomerMeApi.ts` |
| `patchCustomerProfileIO` | `frontend/src/user/api/CustomerProfilePatchApi.ts` |
| `checkoutIO` | `frontend/src/order/api/CheckoutApi.ts` |
| `fetchCatalogIO` | `frontend/src/merchant/api/CatalogApi.ts` |
| `putMerchantProfileIO` | `frontend/src/merchant/api/MerchantProfileApi.ts` |
| `createMerchantStoreIO` | `frontend/src/merchant/api/MerchantStoreApi.ts` |
| `fetchOverviewIO` | `frontend/src/admin/api/OverviewApi.ts` |
| `fetchOrdersPanelIO` | `frontend/src/admin/api/OrdersPanelApi.ts` |
| `fetchPlatformMetaIO` | `frontend/src/admin/api/PlatformMetaApi.ts` |
| `fetchRiderMeIO` | `frontend/src/rider/api/RiderMeApi.ts` |
| `fetchMerchantMeIO` | `frontend/src/merchant/api/MerchantMeApi.ts` |
| `fetchAdminMeIO` | `frontend/src/admin/api/AdminMeApi.ts` |
| `fetchHealthIO` | `frontend/src/shared/api/HealthApi.ts` |
