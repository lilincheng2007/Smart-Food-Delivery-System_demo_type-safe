# API Inventory

当前项目已移除管理员功能，后端逻辑目录按 `merchant / order / rider / user` 拆分。

## API 列表

| HTTP | 路径 | 后端 API 文件 | 请求类型 | 响应类型 |
|------|------|---------------|----------|----------|
| `POST` | `/api/user/login` | `backend/src/user/api/LoginApi.scala` | `LoginRequest` | `LoginResponse` |
| `POST` | `/api/user/register` | `backend/src/user/api/RegisterApi.scala` | `RegisterRequest` | `OkResponse` |
| `GET` | `/api/user/me` | `backend/src/user/api/CustomerMeApi.scala` | 无请求体 | `CustomerMeResponse` |
| `PATCH` | `/api/user/me/profile` | `backend/src/user/api/CustomerProfilePatchApi.scala` | `CustomerProfilePatch` | `OkResponse` |
| `GET` | `/api/merchant/catalog` | `backend/src/merchant/api/CatalogApi.scala` | 无请求体 | `CatalogResponse` |
| `GET` | `/api/merchant/me` | `backend/src/merchant/api/MerchantMeApi.scala` | 无请求体 | `MerchantMeResponse` |
| `PUT` | `/api/merchant/me/profile` | `backend/src/merchant/api/MerchantProfileApi.scala` | `MerchantProfileBody` | `OkResponse` |
| `POST` | `/api/merchant/me/stores` | `backend/src/merchant/api/MerchantStoreApi.scala` | `CreateStoreRequest` | `CreateStoreResponse` |
| `PUT` | `/api/merchant/me/stores/:merchantId/image` | `backend/src/merchant/api/MerchantStoreImageApi.scala` | `UpdateStoreImageRequest` | `OkResponse` |
| `POST` | `/api/merchant/me/stores/:merchantId/image-file` | `backend/src/merchant/api/MerchantStoreImageFileApi.scala` | `multipart/form-data` 字段 `file` | `StoreImageUploadResponse` |
| `GET` | `/api/merchant/store-images/:fileName` | `backend/src/merchant/routes/MerchantRoutes.scala` | 无 | 图片字节流 |
| `GET` | `/api/merchant/me/orders` | `backend/src/merchant/api/MerchantOrderApi.scala` | 无请求体 | `List[Order]` |
| `POST` | `/api/merchant/me/orders/:orderId/ready` | `backend/src/merchant/api/MerchantOrderReadyApi.scala` | 无请求体 | `OkResponse` |
| `GET` | `/api/order/customer/orders` | `backend/src/order/api/CustomerOrdersApi.scala` | 无请求体 | `CustomerOrdersResponse` |
| `GET` | `/api/order/customer/orders/:orderId` | `backend/src/order/api/OrderDetailApi.scala` | 无请求体 | `OrderDetailResponse` |
| `POST` | `/api/order/customer/orders/:orderId/cancel` | `backend/src/order/api/OrderCancelApi.scala` | 无请求体 | `OrderCancelResponse` |
| `POST` | `/api/order/checkout` | `backend/src/order/api/CheckoutApi.scala` | `CheckoutRequest` | `CheckoutResponse` |
| `GET` | `/api/rider/me` | `backend/src/rider/api/RiderMeApi.scala` | 无请求体 | `RiderMeResponse` |
| `POST` | `/api/rider/orders/:orderId/grab` | `backend/src/rider/api/RiderGrabOrderApi.scala` | 无请求体 | `OkResponse` |
| `POST` | `/api/rider/orders/:orderId/status` | `backend/src/rider/api/RiderUpdateOrderStatusApi.scala` | 状态更新请求 | `RiderUpdateOrderStatusResponse` |

## Contract 目录

| 领域 | 后端 | 前端 |
|------|------|------|
| User | `backend/src/user/objects/` | `frontend/src/objects/user/` |
| Order | `backend/src/order/objects/` | `frontend/src/objects/order/` |
| Merchant | `backend/src/merchant/objects/` | `frontend/src/objects/merchant/` |
| Rider | `backend/src/rider/objects/` | `frontend/src/objects/rider/` |
| Shared | `backend/src/shared/objects/` | `frontend/src/objects/shared/` |

## 前端 API 目录

| 领域 | 目录 |
|------|------|
| User | `frontend/src/api/user/` |
| Order | `frontend/src/api/order/` |
| Merchant | `frontend/src/api/merchant/` |
| Rider | `frontend/src/api/rider/` |
