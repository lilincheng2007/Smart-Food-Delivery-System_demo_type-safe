# Delivery Backend

`backend/` 是外卖平台的 Scala 后端服务。服务以单进程方式启动，监听 `8787` 端口，对外提供统一 `APIMessage` 网关，并连接 PostgreSQL 持久化业务数据。后端覆盖顾客、商家、骑手、管理员、评价和 AI 模块，负责鉴权、订单状态机、结算、优惠、库存、营业时间、钱包、退款、聊天、通知已读、骑手配送和 AI 调用。

## 技术栈

- Scala 3.3.3
- Cats Effect 3
- http4s Ember Server / Client
- Circe JSON
- HikariCP + PostgreSQL JDBC
- JWT 鉴权
- OpenAI Chat Completions 兼容接口

## 启动

### 1. 启动 PostgreSQL

在仓库根目录：

```bash
npm run dev:db
```

或在 `backend/` 目录：

```bash
docker compose up -d postgres
```

数据库默认账号密码均为 `postgres`。初始化脚本位于 `docker/init-databases.sql`，默认后端连接 `delivery_backend`。

### 2. 启动后端

```bash
cd backend
sbt run
```

根目录 `npm run dev:backend` 会在未设置 `JAVA_HOME` 时优先使用 Homebrew `openjdk@21`，并自动启动数据库。

启动成功后监听：

```text
http://localhost:8787
```

## 编译检查

```bash
cd backend
sbt -batch compile
```

也可以在仓库根目录运行类型安全与结构审计：

```bash
.codebuddy/skills/type-safety-audit/scripts/check-type-safety.sh /Users/leonli/Desktop/Type-safe_project
```

## 目录结构

```text
backend/
├── build.sbt
├── docker-compose.yml
├── docker/
│   └── init-databases.sql
└── src/
    ├── Main.scala                 # 服务入口，监听 0.0.0.0:8787
    ├── DeliveryRoutes.scala       # API 网关和公开静态图片路由汇总
    ├── admin/                     # 入驻审核、退款仲裁、订单监控、平台优惠
    ├── ai/                        # AI 搜索、周报、订单文案、评价摘要、商家文案/建议
    ├── merchant/                  # 商家账号、店铺、商品、目录、营业时间、优惠、订单处理
    ├── order/                     # 下单、订单查询、聊天、退款、通知已读、状态机、价格快照
    ├── review/                    # 顾客评价、商家回复、投票、图片
    ├── rider/                     # 骑手资料、可抢单、抢单、配送、能量/免责卡
    ├── platform/                  # API 网关、HTTP DTO、JSON codec 聚合
    ├── auth/                      # JWT 与认证鉴权
    ├── db/                        # 数据库连接、事务与初始化入口
    ├── bootstrap/                 # 种子数据与启动导入
    ├── domain/                    # 跨模块 ID、角色、稳定枚举
    ├── media/                     # 静态图片存储、读取、校验和迁移
    ├── promotion/                 # 促销、优惠券和结算辅助领域
    └── user/                      # 登录注册、顾客资料、钱包、优惠券、收货联系人
```

每个业务模块通常包含：

```text
api/                 # 一 API 一个 XxxAPIMessage.scala，可配 support/rules/helper
objects/             # 领域对象
objects/apiTypes/    # API Request / Response 包装类型
tables/              # 表初始化与数据库访问
routes/              # API 注册或额外公开路由
utils/               # 模块内工具
```

## 服务入口与路由

- `src/Main.scala`
  - 启动 http4s Ember Server。
  - 初始化数据库连接池。
  - 执行表初始化和种子数据。
  - 开启 CORS。
- `src/DeliveryRoutes.scala`
  - 汇总 `UserRoutes`、`MerchantRoutes`、`OrderRoutes`、`RiderRoutes`、`AdminRoutes`、`ReviewRoutes`、`AIRoutes`。
  - 注册 `/api` APIMessage 网关。
  - 注册商家店铺图、商品图、订单退款/聊天图、评价图公开路由。

主 API：

```text
POST /api/{apiName}
```

公开图片路由：

```text
GET /api/merchant/store-images/{fileName}
GET /api/merchant/product-images/{fileName}
GET /api/orders/refund-images/{fileName}   # 订单图片，当前包含退款凭证和订单聊天图片
GET /api/reviews/images/{fileName}
```

## APIMessage 网关

关键实现：

- `src/platform/api/APIMessage.scala`
  - `APIMessage[Response]`
  - `APIWithRoleMessage[Response]`
  - `RegisteredAPIMessage`
  - `APIMessageRouter`
- `src/platform/json/ApiJsonCodecs.scala`：Circe Codec 注册。
- 各模块 `routes/*Routes.scala`：使用 `api`、`apiWithRole` 或 `apiWithRoles` 注册消息。

`apiName` 由后端 `*APIMessage` 类名推导。例如：

```text
AISearchAPIMessage -> POST /api/aisearchapi
CustomerVoucherDiscardAPIMessage -> POST /api/customervoucherdiscardapi
```

需要登录的 API 使用 `APIWithRoleMessage` / `apiWithRoles` 注册，并通过 `Authorization: Bearer <token>` 校验角色。

## API 清单

### User API

| apiName | 角色 | 响应 |
|---|---|---|
| `loginapi` | 公开 | `LoginResponse` |
| `registerapi` | 公开 | `OkResponse` |
| `customermeapi` | customer | `CustomerMeResponse` |
| `customerprofilepatchapi` | customer | `OkResponse` |
| `customervoucherdiscardapi` | customer | `OkResponse` |
| `customerrechargeapi` | customer | `CustomerWalletTopUpResponse` |

### Merchant API

| apiName | 角色 | 响应 |
|---|---|---|
| `catalogapi` | 公开 | `CatalogResponse` |
| `merchantmeapi` | merchant | `MerchantMeResponse` |
| `merchantprofileapi` | merchant | `OkResponse` |
| `merchantstoreapi` | merchant | `String` |
| `merchantstoreonboardingrequestsapi` | merchant | `StoreOnboardingRequestsResponse` |
| `merchantstoredescriptionapi` | merchant | `OkResponse` |
| `merchantstoreannouncementapi` | merchant | `OkResponse` |
| `merchantbusinesshoursapi` | merchant | `OkResponse` |
| `merchantstorepromotionsapi` | merchant | `OkResponse` |
| `merchantstoreimageapi` | merchant | `OkResponse` |
| `merchantstoreimagefileapi` | merchant | `String` |
| `merchantcreateproductapi` | merchant | `Product` |
| `merchantproductapi` | merchant | `Product` |
| `merchantproductimagefileapi` | merchant | `Product` |
| `merchantproductdescriptionsapi` | merchant | `OkResponse` |
| `merchantorderacceptapi` | merchant | `OkResponse` |
| `merchantorderrejectapi` | merchant | `OkResponse` |
| `merchantorderreadyapi` | merchant | `OkResponse` |
| `merchantorderprepdelayapi` | merchant | `OkResponse` |
| `merchantrefundrequestsapi` | merchant | `MerchantRefundRequestsResponse` |
| `merchantrefundacceptapi` | merchant | `OkResponse` |
| `merchantrefundrejectapi` | merchant | `OkResponse` |

### Order API

| apiName | 角色 | 响应 |
|---|---|---|
| `customerordersapi` | customer | `CustomerOrdersResponse` |
| `orderdetailapi` | customer | `Order` |
| `ordercancelapi` | customer | `OrderCancelResponse` |
| `ordercompleteapi` | customer | `Order` |
| `orderrefundrequestapi` | customer | `OrderRefundRequestResponse` |
| `orderrefundappealapi` | customer | `OrderRefundRequestResponse` |
| `customerrefundimagefileapi` | customer | `String` |
| `customerorderimagefileapi` | customer | `String` |
| `merchantorderimagefileapi` | merchant | `String` |
| `riderorderimagefileapi` | rider | `String` |
| `customerorderchatmessagesapi` | customer | `OrderChatMessagesResponse` |
| `customersendorderchatmessageapi` | customer | `OrderChatMessagesResponse` |
| `customerorderchatunreadcountsapi` | customer | `OrderChatUnreadCountsResponse` |
| `merchantorderchatmessagesapi` | merchant | `OrderChatMessagesResponse` |
| `merchantsendorderchatmessageapi` | merchant | `OrderChatMessagesResponse` |
| `merchantorderchatunreadcountsapi` | merchant | `OrderChatUnreadCountsResponse` |
| `riderorderchatmessagesapi` | rider | `OrderChatMessagesResponse` |
| `ridersendorderchatmessageapi` | rider | `OrderChatMessagesResponse` |
| `riderorderchatunreadcountsapi` | rider | `OrderChatUnreadCountsResponse` |
| `notificationreadstatesapi` | customer/merchant/rider/admin | `NotificationReadStatesResponse` |
| `notificationmarkreadapi` | customer/merchant/rider/admin | `OkResponse` |
| `notificationmarkallreadapi` | customer/merchant/rider/admin | `OkResponse` |
| `checkoutapi` | customer | `CheckoutResponse` |

### Rider API

| apiName | 角色 | 响应 |
|---|---|---|
| `ridermeapi` | rider | `RiderMeResponse` |
| `rideravailableordersapi` | rider | `RiderAvailableOrdersResponse` |
| `ridergraborderapi` | rider | `OkResponse` |
| `riderupdateorderstatusapi` | rider | `RiderDeliverySettlement` |
| `riderredeemtimeoutcardapi` | rider | `RiderTimeoutCardRedeemResponse` |
| `riderusetimeoutcardapi` | rider | `RiderUseTimeoutCardResponse` |

### Admin API

| apiName | 角色 | 响应 |
|---|---|---|
| `adminstoreonboardingrequestsapi` | admin | `StoreOnboardingRequestsResponse` |
| `adminstoreonboardingacceptapi` | admin | `OkResponse` |
| `adminstoreonboardingrejectapi` | admin | `OkResponse` |
| `adminrefundrequestsapi` | admin | `AdminRefundRequestsResponse` |
| `adminrefundacceptapi` | admin | `OkResponse` |
| `adminrefundrejectapi` | admin | `OkResponse` |
| `adminordermonitorapi` | admin | `AdminOrderMonitorResponse` |
| `adminplatformpromotionsapi` | admin | `PlatformPromotionsResponse` |
| `adminplatformpromotionsupdateapi` | admin | `OkResponse` |

### Review API

| apiName | 角色 | 响应 |
|---|---|---|
| `merchantreviewsapi` | 公开 | `MerchantReviewsResponse` |
| `customersubmitorderreviewapi` | customer | `OkResponse` |
| `customerreviewvoteapi` | customer | `OkResponse` |
| `merchantreviewreplyapi` | merchant | `OkResponse` |
| `customerreviewimagefileapi` | customer | `String` |

### AI API

| apiName | 角色 | 响应 |
|---|---|---|
| `aisearchapi` | customer | `AISearchResponse` |
| `aidietweeklyreportapi` | customer | `AIDietWeeklyReportResponse` |
| `aiorderprogressnarrativesapi` | customer | `AIOrderProgressNarrativesResponse` |
| `aireviewsummaryapi` | customer | `AIReviewSummaryResponse` |
| `aimerchantbusinesssuggestionsapi` | merchant | `AIMerchantBusinessSuggestionsResponse` |
| `aimerchantstoredescriptionapi` | merchant | `AIMerchantStoreDescriptionResponse` |
| `aimerchantproductdescriptionsapi` | merchant | `AIMerchantProductDescriptionsResponse` |

## 数据库结构

表初始化入口：`src/db/DeliveryStateStore.scala`。各模块通过 `*TableInitializer.scala` 创建表、索引和兼容性迁移。

### user 模块

主要表：

- `auth_credentials`：登录凭据。
- `customers`：兼容顾客基础资料。
- `customer_profiles`：顾客公开档案、钱包、优惠券、收货联系人、积分等级。
- `customer_sessions`：顾客会话相关表。

重点字段：

- `customer_profiles.vouchers JSONB`
- `customer_profiles.delivery_contacts JSONB`
- `customer_profiles.wallet_balance`
- `customer_profiles.foodie_points`
- `customer_profiles.foodie_level`

### merchant 模块

主要表：

- `merchant_accounts`：商家账号与 profile JSON。
- `merchant_stores`：店铺资料、描述、公告、图片、营业状态和营业时间。
- `catalog_merchants`：目录商家快照。
- `catalog_products`：目录商品、套餐、库存、上下架、优惠和图片。

重点字段：

- `merchant_stores.description`
- `merchant_stores.announcement`
- `merchant_stores.image_url`
- `merchant_stores.business_status`
- `merchant_stores.weekly_business_hours JSONB`
- `merchant_stores.holiday_business_hours JSONB`
- `catalog_products.listing_status`
- `catalog_products.inventory_mode`
- `catalog_products.max_per_order`
- `catalog_products.bundle_groups JSONB`
- `catalog_products.promotion JSONB`

### order 模块

主要表：

- `orders`：订单主表、金额、状态、退款、时间线、价格快照。
- `order_items`：订单项。
- `checkout_requests`：结算请求记录。
- `order_chat_messages`：订单聊天文字/图片/系统消息。
- `notification_read_states`：通知已读状态。

重点字段：

- `orders.original_amount`
- `orders.discount_amount`
- `orders.payable_amount`
- `orders.used_voucher JSONB`
- `orders.price_snapshot JSONB`
- `orders.price_breakdown JSONB`
- `orders.status_timeline JSONB`
- `orders.estimated_prep_minutes`
- `orders.estimated_ready_at`
- `orders.prep_delay_reason`
- `orders.refund_status`

订单查询性能索引：

- `orders_customer_created_idx`
- `orders_merchant_created_idx`
- `orders_rider_created_idx`
- `orders_available_idx`
- `order_items_order_id_idx`

### rider 模块

主要表：

- `rider_profiles`：骑手档案、薪资、能量、免责卡。
- `rider_accounts`：骑手账号。
- `rider_assignments`：骑手订单分配、配送状态、超时信息。

重点字段：

- `rider_profiles.energy_points`
- `rider_profiles.timeout_card_count`
- `rider_profiles.timeout_count`
- `rider_profiles.timeout_exempted_count`
- `rider_assignments.deadline_at`
- `rider_assignments.was_timeout`
- `rider_assignments.timeout_exempted`
- `rider_assignments.timeout_card_used`
- `rider_assignments.overtime_seconds`

### admin / review / shared 模块

- `store_onboarding_requests`：店铺入驻申请和审核状态。
- `platform_promotions`：平台优惠配置和排期。
- `merchant_reviews`：商家评价、商家回复、图片和 AI 摘要来源。
- `merchant_review_votes`：顾客对评价的投票状态。
- `rider_reviews`：骑手评价。
- `stored_images`：店铺图、商品图、订单图、评价图等上传资源元数据。

## 主要对象结构

### shared objects

- `ids.scala`：`UserId`、`MerchantId`、`RiderId`、`ProductId`、`OrderId`、`VoucherId` 以及角色/状态枚举。
- `Voucher.scala`：优惠券。
- `ErrorBody.scala`：通用错误。
- `apiTypes/OkResponse.scala`：通用成功响应。

### user objects

- root：`Customer`、`CustomerProfile`、`CustomerDeliveryContact`、`CustomerProfilePatch`、`CustomerWalletTopUp`
- `apiTypes/`：`CustomerMeResponse`、`CustomerWalletTopUpResponse`、`LoginRequest` / `LoginResponse`、`RegisterRequest`、`MeResponse`

### merchant objects

- root：`Merchant`、`Product`、`ProductBundleGroup`、`MerchantBusinessHours`、`MerchantProfile`、`MerchantStoreProfile`、`MerchantAccountPublic`、`ProductDescriptionPatch`
- `apiTypes/`：`MerchantMeResponse`、`CatalogResponse`、`CreateStoreRequest`、`CreateProductRequest`、`UpdateProductRequest`、`UpdateStoreImageRequest`

### order objects

- root：`Order`、`OrderItem`、`CheckoutLine`、`OrderTimelineEvent`、`OrderPriceSnapshot`、`OrderPriceBreakdown`、`OrderChatMessage`
- `apiTypes/`：`CheckoutRequest`、`CheckoutResponse`、`CustomerOrdersResponse`、`OrderCancelResponse`、`OrderRefundRequestResponse`、`OrderChatMessagesResponse`、`OrderChatUnreadCountsResponse`、`NotificationReadStatesResponse`

### admin objects

- root：`StoreOnboardingRequest`
- `apiTypes/`：`StoreOnboardingRequestsResponse`、`AdminRefundRequestsResponse`、`AdminOrderMonitorResponse`、`PlatformPromotionsResponse`

### review objects

- root：`MerchantReview`、`RiderReview`、`ReviewSummary`
- `apiTypes/`：`MerchantReviewsResponse`、`CustomerSubmitOrderReviewRequest` 等评价请求/响应对象

### rider objects

- root：`Rider`、`RiderProfile`、`RiderAccountPublic`、`RiderDeliveryStatus`、`RiderDeliverySettlement`
- `apiTypes/`：`RiderMeResponse`、`RiderAvailableOrdersResponse`、`RiderTimeoutCardRedeemResponse`、`RiderUseTimeoutCardResponse`

### ai objects

- root：`AIGeneratedProductDescription`
- `apiTypes/`：`AISearchRequest` / `AISearchResponse`、`AIDietWeeklyReportRequest` / `AIDietWeeklyReportResponse`、`AIOrderProgressNarrativesRequest` / `AIOrderProgressNarrativesResponse`、`AIReviewSummaryResponse`、`AIMerchantBusinessSuggestionsResponse`、`AIMerchantStoreDescriptionResponse`、`AIMerchantProductDescriptionsResponse`

## 关键业务规则

### 顾客、优惠券与等级

- 新顾客注册时发放欢迎券。
- 结算时后端校验优惠券归属、剩余次数、是否过期、是否满足满减门槛。
- 使用优惠券后减少 `remainingCount`。
- 确认完成订单后，按实付金额向下取整累计吃货积分。
- 每 200 积分升 1 级，跨级后发放满减券。
- `CustomerVoucherDiscardAPIMessage` 只允许舍弃已过期优惠券；未过期或不存在会返回错误。

### 收货联系人

- 多组联系人保存在 `customer_profiles.delivery_contacts JSONB`。
- 保存时后端校验联系人姓名、电话、地址非空。
- 至少需要一组联系人。
- 必须且只能有一组默认联系人。

### 商家、商品、营业时间与优惠

- 商家只能操作自己拥有的店铺与商品。
- 店铺资料支持描述、公告、图片、营业状态、每周营业时间、节假日特殊营业时间。
- 顾客下单时后端通过 `MerchantBusinessHoursSupport` 校验店铺是否可接单。
- 商品支持普通菜品和套餐；套餐配置由 `ProductBundleGroup` 描述，并在创建/更新时校验选项归属。
- 商品支持上下架、库存模式、每单限购、图片上传。
- 店铺优惠、菜品优惠、平台优惠均有排期和启停状态；结算时以后端计算为准。

### 订单、结算与状态机

- 结算会创建订单、订单项、价格快照和结构化价格明细，并扣减钱包余额。
- 下单时锁定商品行校验上下架、营业状态、售罄、库存不足和每单限购，并扣减有限库存。
- 取消订单和退款通过后会退回实付金额。
- 订单状态流转集中在 `OrderStatusTransitionService.scala`，规则在 `OrderStatusTransitionRules.scala`。
- 状态变化通过 `OrderStatusTimelineSupport.scala` 追加时间线事件。
- 商家接单可设置预计备餐时间；商家可主动延迟备餐并通知顾客。
- 商家出餐、骑手送达等状态变化会自动写入系统聊天消息。

### 订单聊天、通知与图片

- 顾客、商家、骑手使用分角色聊天 API，消息持久化到 `order_chat_messages`。
- 聊天图片、退款图片、评价图片走 `StoredImageRoutes` 和 `stored_images` 元数据表。
- 全局通知中心的已读状态通过 `notification_read_states` 持久化，支持单条已读和全部已读。

### 评价与退款

- 顾客可对完成订单提交商家/骑手评价并上传图片。
- 商家可回复评价，顾客可对评价投票。
- 退款可以由商家处理，也可进入管理员仲裁。
- 管理员监控 API 会聚合待退款、异常订单、商家超时和骑手超时订单。

### 订单查询性能

订单读取使用精准查询和批量加载：

- `OrderTable.listByCustomerId`
- `OrderTable.listByMerchantIds`
- `OrderTable.listByRiderId`
- `OrderTable.listAvailableUnassigned`
- `OrderTable.countActiveByRider`
- `OrderItemTable.listByOrderIds`

这些方法减少全表扫描和订单项 N+1 查询。

### 骑手能力

- 同一骑手最多同时配送 5 单。
- 每单配送成功薪资增加 `5`。
- 非超时送达获得 `10` 能量。
- `100` 能量可兑换 1 张超时免责卡。
- 配送超时阈值由 `RiderTimeoutPolicy` 管理，当前为 45 分钟。
- 超时且有免责卡时自动消耗；超时无卡时可后续手动补用。

### AI 能力

`src/ai/utils/OpenAIClient.scala` 负责 OpenAI 兼容接口调用，支持超时与重试，并避免使用 unsafe URI 构造。

AI 功能包括：

- 顾客 AI 搜索：根据自然语言需求推荐商家与菜品。
- 顾客 AI 饮食周报：基于近 7 天订单生成分析。
- 顾客 AI 订单进度叙事：按订单状态生成展示文案。
- 顾客 AI 评价摘要：聚合评价内容生成摘要。
- 商家 AI 店铺描述：根据店铺与商品生成描述。
- 商家 AI 菜品描述：批量生成菜品卖点文案。
- 商家 AI 经营建议：基于经营数据生成建议。

## 配置

### 数据库

| 环境变量 | 默认值 | 说明 |
|---|---|---|
| `DB_HOST` | `127.0.0.1` | PostgreSQL 主机 |
| `DB_PORT` | `5432` | PostgreSQL 端口 |
| `DB_NAME` | `delivery_backend` | 连接数据库 |
| `DB_USER` | `postgres` | 数据库用户 |
| `DB_PASSWORD` | `postgres` | 数据库密码 |
| `DB_MAX_POOL_SIZE` | `10` | 连接池大小 |
| `DB_CONNECTION_TIMEOUT_MS` | `3000` | 连接超时 |
| `DB_POOL_NAME` | `delivery-backend-pool` | 连接池名称 |

相关文件：

- `src/db/DatabaseConfig.scala`
- `src/db/DatabasePool.scala`
- `src/db/DeliveryStateStore.scala`

### JWT

JWT 实现在 `src/auth/JwtSupport.scala`。开发环境会使用默认密钥，生产或多人协作环境建议显式配置：

```bash
export JWT_SECRET=replace-with-a-strong-secret
```

### 内部服务令牌

`src/platform/interop/InternalToken.scala` 使用 `SERVICE_INTERNAL_TOKEN` 校验内部请求头 `X-Internal-Token`，默认值为 `dev-internal-token`。如接入内部调用方，应在环境中显式覆盖该值。

### AI

启动后端前配置：

```bash
export OPENAI_API_KEY=your-key-here
# 可选
export OPENAI_BASE_URL=https://api.openai.com/v1
export OPENAI_MODEL=gpt-4o-mini
```

未配置 `OPENAI_API_KEY` 时，普通业务功能仍可运行；AI 功能会提示服务未配置或返回兜底内容。

## 演示数据

种子数据位于 `src/bootstrap/SeedData.scala` 和 `src/bootstrap/SeedBootstrap.scala`，包含默认顾客、商家、菜品、骑手、管理员与初始订单。

演示账号：

| 角色 | 账号 | 密码 |
|---|---|---|
| 顾客 | `customer_demo` | `123456` |
| 商家 | `merchant_demo` | `123456` |
| 骑手 | `rider_demo` | `123456` |
| 管理员 | `admin` | `123456` |

## 新增后端 API 的推荐步骤

1. 在对应模块 `objects/` 新增领域对象；请求/响应 wrapper 放入 `objects/apiTypes/`。
2. 在对应模块 `api/` 新增单个 `*APIMessage.scala`，文件名与 case class 一致。
3. 如逻辑较复杂，将可复用逻辑拆到同模块 `Support`、`Rules` 或 helper 文件，保留 APIMessage 入口简洁。
4. 在 `routes/` 中使用 `api`、`apiWithRole` 或 `apiWithRoles` 注册。
5. 在 `src/platform/json/ApiJsonCodecs.scala` 注册 Codec。
6. 同步前端 `frontend/src/apis/*/XxxAPI.ts` 与 `frontend/src/objects/*` 的契约。
7. 运行 `sbt -batch compile`、前端 `npm run typecheck --prefix frontend` 和类型安全审计脚本验证。

## 代码约定

- 后端 Scala 新代码使用 `val`，避免新增 `var`。
- 业务 API 优先走 `APIMessage` 网关，不新增零散字符串路由；静态资源等特殊场景除外。
- 需要鉴权的业务操作必须使用 `APIWithRoleMessage` / `apiWithRoles` 并声明角色。
- 业务数据以后端数据库为准，不依赖前端本地状态作为真实数据源。
- 修改 API 或对象时，保持前后端契约字段语义一致。
- 禁止新增 `unsafeRun`、`unsafeFromString`、`asInstanceOf` 等类型安全逃逸口；需要解析失败时返回 `Either` / `IO.fromEither` 或业务错误。
