# Type-safe_project 长期可维护性优化计划

> 本文档用于记录结构、命名与分层优化计划，并持续沉淀每批优化后的复盘摘要与下一批建议。

## 1. 总目标

1. 保持前后端类型安全主线：业务 API 继续通过 `APIMessage` / `sendAPI` 对齐，避免新增绕过统一网关的零散接口。
2. 强化后端分层边界：`api/` 只保留 API 入口编排，业务流程进入 `services/`，规则校验进入 `validators/`，数据库访问进入 `tables/`，媒体能力进入 `media/`。
3. 降低模糊命名带来的维护成本：逐步消除 `Support`、`ApiSupport`、过宽 API 名称和兼容 alias 对真实归属的遮蔽。
4. 收敛领域类型：将关键状态、角色、触发类型、优惠类型等字符串枚举逐步改成稳定 enum / union，并保持前后端一致。
5. 控制前端复杂度：大页面、大 store 逐步拆到页面私有 `components/`、`hooks/`、`functions/`、`objects/`、`stores/`。
6. 让文档、审计脚本和代码结构同步演进，避免“代码已迁移、规则未更新”或“脚本通过但结构仍退化”。

## 2. 优化原则

- 小步迁移，避免一次性大重构。
- 新代码先遵守规则，旧代码触碰时顺手收敛。
- 每批只处理一组边界清晰的问题，并在批次结束后运行必要验证。
- 保持 HTTP 公开路径、API 名称和前端调用行为兼容；确需重命名时先提供过渡策略。
- 优先处理“会继续扩散坏模式”的结构问题，再处理局部大文件和低优先级死代码。

## 3. 批次计划

### 第一批：迁移错位的后端 service / rules / support

目标：让 `api/` 回到“API 入口层”，不继续承载业务规则和流程服务。

优先文件：

- `backend/src/order/api/OrderStatusTransitionService.scala` → `backend/src/order/services/OrderStatusTransitionService.scala`
- `backend/src/order/api/OrderStatusTransitionRules.scala` → `backend/src/order/validators/OrderStatusTransitionValidator.scala` 或 `backend/src/order/services/OrderStatusTransitionRules.scala`
- `backend/src/order/api/OrderStatusTimelineSupport.scala` → `backend/src/order/services/OrderTimelineService.scala`
- `backend/src/order/api/RefundWorkflowSupport.scala` → `backend/src/order/services/RefundWorkflowService.scala`
- `backend/src/order/api/OrderChatNotificationTemplates.scala` → `backend/src/order/services/OrderChatNotificationTemplateService.scala`
- `backend/src/merchant/api/MerchantBusinessHoursSupport.scala` → `backend/src/merchant/services/MerchantBusinessHoursService.scala`
- `backend/src/admin/api/AdminOrderMonitorSupport.scala` → `backend/src/admin/services/AdminOrderMonitorService.scala`

完成标准：

- 业务服务不再从 `delivery.*.api` 包被其它模块导入。
- 相关 import 更新到 `services/` 或 `validators/`。
- 后端编译通过。

### 第二批：清理泛化 `Support` / `ApiSupport` 命名

目标：让文件名能表达职责、领域和层级，避免形成新的“杂物间”。

候选文件：

- `backend/src/merchant/utils/MerchantApiSupport.scala`
- `backend/src/order/utils/OrderApiSupport.scala`
- `backend/src/rider/utils/RiderApiSupport.scala`
- `backend/src/user/utils/UserApiSupport.scala`
- `backend/src/promotion/services/VoucherSupport.scala`
- `backend/src/auth/JwtSupport.scala`

建议方向：

- 归属校验：迁到 `validators/`，命名为 `*OwnershipValidator`、`*AccessValidator`。
- 响应装配：迁到 `services/` 或 `assemblers/` 风格对象，命名为 `*ResponseAssembler`。
- 标准券生成：`VoucherSupport` 改为 `StandardPlatformVoucherService`。
- JWT 能力：`JwtSupport` 改为 `JwtTokenService` 或 `JwtTokenCodec`。
- 删除无实际价值的 identity helper，例如 `OrderApiSupport.normalizeLine`。

完成标准：

- 新增代码不再使用 `*ApiSupport` 命名。
- 旧支撑函数按职责拆到具体层。
- 无引用的空 helper 被删除。

### 第三批：补齐 validators，并把规则从 API / 大 service 中迁出

目标：让空目录不只是占位，关键业务规则有稳定落点。

重点方向：

- `order/validators/CheckoutLineValidator.scala`：购物车行、套餐选择、数量规则。
- `order/validators/OrderStatusTransitionValidator.scala`：订单状态流转规则。
- `merchant/validators/MerchantStoreOwnershipValidator.scala`：商家店铺归属校验。
- `merchant/validators/MerchantBusinessHoursValidator.scala`：营业状态、营业时间输入校验。
- `promotion/validators/PromotionValidator.scala`：优惠类型、触发条件、金额边界。
- `review/validators/ReviewImageValidator.scala`：评价图片数量、格式、大小边界。

完成标准：

- `api/` 中只保留请求解析、鉴权上下文、调用服务和返回响应。
- 复杂 `if` / 状态规则能在 `validators/` 中找到。
- 错误消息和校验边界集中管理。

### 第四批：拆分 `OrderCheckoutService`

目标：降低结算服务的职责密度，避免价格、库存、优惠、订单构造继续耦合。

建议拆分：

- `order/services/CheckoutInventoryService.scala`：库存校验、库存扣减计划。
- `order/services/CheckoutPricingService.scala`：价格明细、订单价格快照。
- `order/services/CheckoutOrderFactory.scala`：订单对象、时间线初始事件构造。
- `promotion/services/VoucherRedemptionService.scala`：优惠券校验、锁定、消费。
- `merchant/services/MerchantBusinessHoursService.scala`：营业状态判断。

完成标准：

- `OrderCheckoutService` 只负责结算流程编排。
- 单个服务文件职责清晰，避免继续扩大。
- 前后端结算结果仍以后端为唯一事实源。

### 第五批：收敛字符串枚举和前后端类型契约

目标：让关键业务状态成为显式类型，而不是散落字符串。

优先类型：

- `MerchantBusinessStatus`
- `StoreOnboardingStatus`
- `PromotionDiscountType`
- `PromotionTriggerType`
- `OrderChatRole`
- `OrderChatMessageType`
- `InventoryMode`
- `BundleSelectionType`

完成标准：

- 后端对象不再裸用 `String` 表达核心状态。
- 前端 union 与后端 enum 名称和值对齐。
- JSON codec 能稳定处理新增枚举。

### 第六批：模块化 JSON codec

目标：降低 `platform/json/ApiJsonCodecs.scala` 的中心化负担，让 codec 靠近领域对象。

建议顺序：

1. 先拆 `merchant/json/MerchantJsonCodecs.scala`，修正 `Merchant` 字段漂移风险。
2. 再拆 `order/json/OrderJsonCodecs.scala`，覆盖订单、聊天、退款、通知相关对象。
3. 然后拆 `user/json/UserJsonCodecs.scala`、`promotion/json/PromotionJsonCodecs.scala`。
4. `platform/json/ApiJsonCodecs.scala` 只做聚合导出和平台级通用 codec。

完成标准：

- 模块新增对象时优先在模块 `json/` 下维护 codec。
- 平台聚合文件显著变薄。
- 手写 encoder / decoder 与对象字段保持同步。

### 第七批：收敛过宽 API 与兼容 alias

目标：让 API 名称表达真实动作，减少整包写回和隐藏归属。

重点项：

- 评估 `MerchantProfileAPIMessage` 是否仍被前端使用。
  - 若不用：标记废弃并删除前后端 API。
  - 若要保留：改成窄接口，只更新商家账号资料，不写回 `stores`、订单等业务数据。
- 将 `MerchantStoreAPIMessage` 这类宽泛命名改为更明确的入驻申请 API，例如 `MerchantCreateStoreOnboardingRequestAPIMessage`。
- 逐步移除 `delivery.domain.CompatibilityAliases` 中的 `Promotion`、`Voucher`、`ErrorBody`、`HealthOk` alias 使用。

完成标准：

- API inventory 中的名称能直接表达动作。
- 新代码不再从 compatibility alias 导入业务对象。
- 旧 alias 只作为短期迁移过渡存在。

### 第八批：前端大页面和大 store 拆分

目标：降低页面入口和 Zustand store 的维护成本。

优先拆分：

- `frontend/src/pages/OrderChatPage/index.tsx`
  - `hooks/useOrderChatMessages.ts`
  - `hooks/useOrderChatPeerContext.ts`
  - `hooks/usePendingChatImage.ts`
  - `components/ChatHeader.tsx`
  - `components/MessageList.tsx`
  - `components/ChatComposer.tsx`
- `frontend/src/pages/AdminConsole/index.tsx`
  - `hooks/useAdminConsoleData.ts`
  - `hooks/useRefundReviewDialog.ts`
  - `components/OnboardingRequestList.tsx`
  - `components/RefundReviewDialog.tsx`
  - `components/AdminMetricCards.tsx`
- `frontend/src/pages/CustomerPortal/stores/use-customer-portal-store.ts`
  - 拆分购物车、结算、评价、资料、AI 相关 action。

完成标准：

- `index.tsx` 主要负责页面装配。
- 业务副作用集中在 hooks / store actions。
- 展示组件不直接复制后端业务规则。

### 第九批：通知和结算预估后端化

目标：避免前端复制业务事实源。

结算方向：

- 新增 `CheckoutQuoteAPI`，由后端返回价格预估、优惠明细、余额检查和失败原因。
- 前端只展示 quote，不自行复刻完整计价规则。

通知方向：

- 新增 `NotificationFeedService`。
- 评估使用 `NotificationEventTable`，或由订单、退款、聊天等事实源聚合生成 feed。
- 前端 `GlobalNotificationCenter` 只负责展示、轮询和 mark read。

完成标准：

- 价格、通知事件不再主要由前端本地推导。
- 后端读状态、通知 feed、订单事实源形成闭环。

### 第十批：低优先级清理与文档同步

目标：清理遗留噪音，防止文档和脚本落后。

候选项：

- 确认并删除无引用的 `storeImageExtension` 等媒体迁移遗留函数。
- 统一订单图片上传路径，避免一部分直接调 `StoredImageService`、一部分走 `OrderImageFileService`。
- 更新 `API_INVENTORY.md`、`DIRECTORY_LAYERING_GUIDE.md`、`AGENTS.md`、`README.full.md`。
- 扩展可维护性审计脚本：检查错位 `api/` service、空 `validators/`、新增 `Support` 命名、过大 codec 等。

完成标准：

- 清理不改变业务行为。
- 文档、脚本、目录结构一致。
- 后端编译、前端类型检查、审计脚本通过。

## 4. 建议验证命令

每批按影响范围选择验证，不要求每次都全量执行。

```bash
npm run typecheck --prefix frontend
cd backend && sbt -batch compile
.codebuddy/skills/type-safety-audit/scripts/check-type-safety.sh /Users/leonli/Desktop/Type-safe_project
.codebuddy/skills/maintainability-audit/scripts/check-maintainability.sh /Users/leonli/Desktop/Type-safe_project
```

涉及前端局部文件时可补充：

```bash
cd frontend && npx eslint <changed-files>
```

## 5. 当前进展记录

| 日期 | 进展 | 备注 |
|---|---|---|
| 2026-06-13 | 已创建可维护性与分层规则基础设施 | 新增 `.codebuddy/skills/maintainability-audit/`、`.codebuddy/skills/layered-feature-development/`，并沉淀 `DIRECTORY_LAYERING_GUIDE.md`。 |
| 2026-06-13 | 已完成第一轮结构整改 | 后端 `shared` 实现已拆分到更明确模块，`Promotion` / `Voucher` 迁入 `promotion`，`StoredImage` 迁入 `media`，页面私有 store 已迁入页面目录。 |
| 2026-06-13 | 已通过既有审计 | 后端编译、前端类型检查、类型安全审计、可维护性审计曾通过；后续批次仍需重新验证。 |
| 2026-06-13 | 已完成第二轮静态分析 | 识别出 `api/` 错位 service、泛化 `Support` 命名、中心化 JSON codec、字符串枚举、大页面 / 大 store、前端结算与通知推导等后续优化点。 |
| 2026-06-13 | 已完成第一批迁移 | 将错放在 `api/` 层的订单状态流转、退款流程、时间线、聊天通知模板、商家营业时间和管理员订单监控支撑逻辑迁入 `services/` / `validators/`；同步更新引用和 `backend/README.md`。 |
| 2026-06-13 | 已完成第二批清理 | 清理泛化 `Support` / `ApiSupport` 命名，将响应装配、归属校验、自有商品列表、标准平台券和 JWT 能力迁到语义明确的 service / validator。 |
| 2026-06-13 | 已完成第三批 validators 补齐 | 将商家营业时间、结算行 / 库存 / 套餐选择、促销、评价输入等规则迁入对应 validators，并继续收敛订单状态流转 actor role 字符串。 |
| 2026-06-13 | 已完成第四批结算服务拆分 | 将 `OrderCheckoutService` 的库存扣减、价格明细、订单构造、优惠券兑换和积分等级职责拆入独立 service，`OrderCheckoutService` 保留结算编排门面。 |
| 2026-06-14 | 已完成第五批枚举收敛 | 新增并接入商家营业状态、商品库存模式、套餐选择类型、促销类型、聊天角色和聊天消息类型枚举；前端补齐对应类型文件，JSON 值保持兼容字符串。 |
| 2026-06-14 | 已完成第六批 JSON codec 模块化 | 将业务 codec 下沉到各模块 `json/*JsonCodecs.scala`，新增平台 `CommonJsonCodecs`，`ApiJsonCodecs.scala` 收敛为 14 行聚合导出。 |
| 2026-06-14 | 已完成第七批过宽 API 与兼容 alias 收敛 | 删除未使用 `merchantprofileapi`；新增 `merchantcreatestoreonboardingrequestapi` 并保留 `merchantstoreapi` 兼容入口；清理后端 `Promotion`/`Voucher`/`ErrorBody` compatibility alias 导入。 |
| 2026-06-14 | 已完成第八批前端大页面和大 store 拆分 | `OrderChatPage` 拆分 hooks/components，`AdminConsole` 拆分数据 hook 与关键组件，`CustomerPortal` store 按子域 actions 拆分组合。 |
| 2026-06-14 | 已完成第九批通知与结算预估后端化 | 新增 `checkoutquoteapi` 与 `notificationfeedapi`，前端 `checkoutActions` 与 `CustomerCheckoutPage` 改为消费后端 quote，`GlobalNotificationCenter` 改为消费后端 feed 并仅负责展示/轮询/已读。 |

## 6. 当前代码现状快照

### 已改善的结构

- 项目已有明确的 `api/`、`services/`、`validators/`、`tables/`、`objects/`、`json/`、`media/` 分层约定。
- `backend/src/shared` 已被移除，基础设施和业务对象开始回到真实归属模块。
- `Promotion`、`Voucher` 已迁到 `backend/src/promotion/objects/`。
- 图片存储相关能力已有独立 `backend/src/media/` 模块。
- 页面私有 Zustand store 已迁到对应 `pages/{Page}/stores/`。
- 可维护性审计脚本已经能捕捉部分结构退化问题。
- 第一批已将 `OrderStatusTransitionService`、`RefundWorkflowService`、`OrderTimelineService`、`OrderChatNotificationTemplateService`、`MerchantBusinessHoursService`、`AdminOrderMonitorService` 迁出 `api/` 包。
- 订单状态流转规则已从 `order/api/OrderStatusTransitionRules.scala` 收敛到 `order/validators/OrderStatusTransitionValidator.scala`。
- 第二批已删除后端源码中的 `*Support.scala` 文件；`MerchantApiSupport`、`OrderApiSupport`、`RiderApiSupport`、`UserApiSupport`、`VoucherSupport`、`JwtSupport` 均已由更明确命名替代。
- Me 响应装配已收敛到 `CustomerMeResponseAssembler`、`MerchantMeResponseAssembler`、`RiderMeResponseAssembler`。
- 商家店铺归属校验已落到 `MerchantStoreOwnershipValidator`，商家自有商品列表已落到 `MerchantOwnedProductService`。
- 标准平台券生成已改为 `StandardPlatformVoucherService`，JWT 能力已改为 `JwtTokenService`。
- `MerchantBusinessHoursValidator` 已负责营业状态、每周营业时间和节假日营业时间的校验与归一化。
- `CheckoutLineValidator` 已负责购物车行、库存消耗、每单限购和套餐选择校验。
- `PromotionValidator` 已负责平台 / 商家优惠通用规则与商家菜品优惠目标校验，旧 `PromotionValidation` 已删除。
- `ReviewImageValidator` 已覆盖评价提交中的商家评分、骑手评分、评价文字和评价图片 URL 校验。
- `CheckoutInventoryService` 已负责库存扣减计划和库存状态刷新。
- `CheckoutPricingService` 已负责金额舍入、价格明细和优惠分摊。
- `CheckoutOrderFactory` 已负责订单项、订单价格快照和初始订单构造。
- `VoucherRedemptionService` 已负责优惠券校验、奖励券构造和消费。
- `CustomerLoyaltyService` 已负责吃货积分等级计算。
- `MerchantBusinessStatus`、`ProductInventoryMode`、`ProductBundleSelectionType`、`PromotionDiscountType`、`PromotionTriggerType`、`OrderChatRole`、`OrderChatMessageType` 已接入后端对象、业务逻辑和 JSON codec。
- 前端已补齐同名类型文件，并在现有 `MerchantBusinessHours`、`Product`、`Promotion`、`OrderChatMessage` 类型中复用，保持前后端契约值一致。
- JSON codec 已拆为模块 codec 定义与平台聚合导出：`merchant`、`order`、`promotion`、`user`、`admin`、`review`、`rider`、`ai` 均已有真实 `*JsonCodecs.scala`，`CommonJsonCodecs.scala` 承载平台通用 codec。

### 仍需优化的结构

- `domain/CompatibilityAliases.scala` 仍隐藏部分对象真实归属，应作为短期兼容层继续收敛。
- 部分历史 API 仍保留兼容包装（如 `merchantstoreapi`），后续可结合版本窗口逐步下线。
- 前端仍有少量商品展示级别的本地优惠提示逻辑，需继续约束为“仅展示，不做业务事实判断”。
- 第十批应聚焦低优先级清理与文档/审计脚本同步，避免规则落后于代码结构。

## 7. 后续维护记录空间

后续每完成一批优化，在此追加记录：

| 日期 | 批次 | 已完成内容 | 验证结果 | 后续遗留 |
|---|---|---|---|---|
| 2026-06-13 | 第一批：迁移错位的后端 service / rules / support | 移动并重命名 `order/api` 下的状态流转、时间线、退款流程、聊天模板文件；移动并重命名 `merchant/api/MerchantBusinessHoursSupport.scala` 与 `admin/api/AdminOrderMonitorSupport.scala`；补齐跨包 import；同步 `backend/README.md`。 | `cd backend && sbt -batch compile` 通过；类型安全审计通过（45 pass / 0 fail）；可维护性审计通过（10 pass / 0 warn / 0 fail）；相关文件无 IDE lint 诊断。 | 第二批继续清理泛化 `Support` / `ApiSupport` 命名；本批未处理仍保留的 `JwtSupport`、`MerchantApiSupport`、`OrderApiSupport`、`RiderApiSupport`、`UserApiSupport`、`VoucherSupport`。 |
| 2026-06-13 | 第二批：清理泛化 `Support` / `ApiSupport` 命名 | 删除 `MerchantApiSupport`、`OrderApiSupport`、`RiderApiSupport`、`UserApiSupport`；新增 `MerchantStoreOwnershipValidator`、`MerchantOwnedProductService`、三类 Me 响应装配器和账号 validator；将 `VoucherSupport` 重命名为 `StandardPlatformVoucherService`，将 `JwtSupport` 重命名为 `JwtTokenService`；同步 `backend/README.md`。 | `cd backend && sbt -batch compile` 通过；类型安全审计通过（45 pass / 0 fail）；可维护性审计通过（10 pass / 0 warn / 0 fail）；后端源码已无 `*Support.scala`。 | 第三批继续补齐 validators：本批只先建立少量 validator / assembler 落点，尚未系统迁出购物车、营业时间、促销、评价图片等复杂规则。 |
| 2026-06-13 | 第三批：补齐 validators，并迁出独立规则 | 新增 `MerchantBusinessHoursValidator`、`CheckoutLineValidator`、`PromotionValidator`；扩展 `ReviewImageValidator` 和 `OrderStatusTransitionValidator`；更新 `MerchantBusinessHoursAPIMessage`、`OrderCheckoutService`、平台 / 商家优惠 API、评价提交 API 调用对应 validator。 | `cd backend && sbt -batch compile` 通过；类型安全审计通过（45 pass / 0 fail）；可维护性审计通过（10 pass / 0 warn / 0 fail）；相关目录无 IDE lint 诊断。 | 第四批继续拆分 `OrderCheckoutService`：本批只迁出校验和消耗数量计算，价格、库存扣减、订单构造、优惠券兑换仍在同一服务内。 |
| 2026-06-13 | 第四批：拆分 `OrderCheckoutService` | 新增 `CheckoutInventoryService`、`CheckoutPricingService`、`CheckoutOrderFactory`、`VoucherRedemptionService`、`CustomerLoyaltyService`；更新结算、取消、拒单、退款、完成订单和顾客订单列表相关调用，移除外部对 `OrderCheckoutService` 的价格 / 库存 / 券 / 等级依赖。 | `cd backend && sbt -batch compile` 通过；类型安全审计通过（45 pass / 0 fail）；可维护性审计通过（10 pass / 0 warn / 0 fail）；相关目录无 IDE lint 诊断。 | 第五批进入字符串枚举收敛；本批仍保留 `OrderCheckoutService.buildOrdersForCheckout` 作为兼容编排门面。 |
| 2026-06-14 | 第五批：收敛字符串枚举和前后端类型契约 | 新增后端枚举 `MerchantBusinessStatus`、`ProductInventoryMode`、`ProductBundleSelectionType`、`PromotionDiscountType`、`PromotionTriggerType`、`OrderChatRole`、`OrderChatMessageType`；更新领域对象、业务服务、表读写和 APIMessage；前端补齐同名类型文件并复用现有 union。 | `cd backend && sbt -batch compile` 通过；`npm run typecheck --prefix frontend` 通过；类型安全审计通过（45 pass / 0 fail）；可维护性审计通过（10 pass / 0 warn / 0 fail）；后端核心字段已无目标裸 `String`。 | 第六批进入 JSON codec 模块化；本批为了控制风险仍将 enum codec 注册在 `platform/json/ApiJsonCodecs.scala` 聚合入口。 |
| 2026-06-14 | 第六批：模块化 JSON codec | 新增 `platform/json/CommonJsonCodecs.scala`；将促销、订单、商家、顾客、管理员、评价、骑手、AI 的 codec 从 `ApiJsonCodecs.scala` 下沉到各模块 `json/*JsonCodecs.scala`；保留 `Order`、`Merchant`、`CustomerProfile`、`CheckoutRequest` 等手写 decoder / encoder 的历史字段兼容；同步 `DIRECTORY_LAYERING_GUIDE.md`、`AGENTS.md`、`backend/README.md`。 | `cd backend && sbt -batch compile` 通过；`npm run typecheck --prefix frontend` 通过；类型安全审计通过（45 pass / 0 fail）；可维护性审计通过（10 pass / 0 warn / 0 fail）；`ApiJsonCodecs.scala` 已收敛为 14 行聚合导出；相关文件无 IDE lint 诊断。 | 第七批进入过宽 API 与 compatibility alias 收敛；优先确认 `MerchantProfileAPIMessage` 是否仍被前端使用，再处理 `MerchantStoreAPIMessage` 命名和 `CompatibilityAliases` 依赖。 |
| 2026-06-14 | 第七批：收敛过宽 API 与兼容 alias | 删除后端 `MerchantProfileAPIMessage.scala` 与前端 `MerchantProfileAPI.ts`；新增 `MerchantCreateStoreOnboardingRequestAPIMessage.scala` 与 `MerchantCreateStoreOnboardingRequestAPI.ts`，并将旧 `MerchantStoreAPIMessage` / `MerchantStoreAPI.ts` 收敛为兼容包装；更新 `MerchantRoutes.scala`、`API_INVENTORY.md`、`backend/README.md`、`frontend/README.md`；将后端 `Promotion` / `Voucher` / `ErrorBody` 导入切换到真实归属模块。 | `cd backend && sbt -batch compile` 通过；`npm run typecheck --prefix frontend` 通过；类型安全审计通过（45 pass / 0 fail）；可维护性审计通过（10 pass / 0 warn / 0 fail）；相关文件无 IDE lint 诊断。 | 第八批进入前端大页面和大 store 拆分，优先 `OrderChatPage`、`AdminConsole`、`CustomerPortal` store。 |
| 2026-06-14 | 第八批：前端大页面和大 store 拆分 | `OrderChatPage/index.tsx` 拆分为 `hooks/useOrderChatMessages.ts`、`hooks/useOrderChatPeerContext.ts`、`hooks/usePendingChatImage.ts` 与 `components/ChatHeader.tsx`、`components/MessageList.tsx`、`components/ChatComposer.tsx`；`AdminConsole/index.tsx` 拆分为 `hooks/useAdminConsoleData.ts`、`hooks/useRefundReviewDialog.ts` 与 `components/AdminMetricCards.tsx`、`components/OnboardingRequestList.tsx`、`components/RefundReviewDialog.tsx`；`use-customer-portal-store.ts` 拆分为 `portalDataActions`、`cartActions`、`orderReviewActions`、`checkoutActions`、`profileAiActions` 子域 action builder 组合。 | `npm run typecheck --prefix frontend` 通过；`cd backend && sbt -batch compile` 通过；类型安全审计通过（45 pass / 0 fail）；可维护性审计通过（10 pass / 0 warn / 0 fail）；相关文件无 IDE lint 诊断。 | 第九批进入通知与结算预估后端化，优先补齐 `CheckoutQuoteAPI` 与 `NotificationFeedService`。 |
| 2026-06-14 | 第九批：通知和结算预估后端化 | 新增后端 `CheckoutQuoteAPIMessage.scala` + 前端 `CheckoutQuoteAPI.ts` 与 `CheckoutQuoteResponse` 契约；新增后端 `NotificationFeedService.scala` + `NotificationFeedAPIMessage.scala` 与 `NotificationFeedResponse` 契约；`CustomerPortal` 的 `checkoutActions.ts`、`CustomerCheckoutPage.tsx` 改为“先读取 quote 再提交 checkout”；`GlobalNotificationCenter.tsx` 改为消费 `notificationfeedapi`，前端仅保留展示、轮询、已读回写。同步更新 `API_INVENTORY.md`、`backend/README.md`、`frontend/README.md`。 | `cd backend && sbt -batch compile` 通过；`npm run typecheck --prefix frontend` 通过；相关文件无 IDE lint 诊断。 | 第十批进入低优先级清理与文档同步，优先处理媒体遗留函数、图片上传路径一致性与审计脚本扩展。 |

## 8. 下一批次建议：第十批低优先级清理与文档同步

十批优化计划已完成，后续建议按“固定审计节奏 + 触碰即收敛”持续维护：

1. 每次新增 API 继续同步维护 `API_INVENTORY.md` 与前后端 API 文件一一对应。
2. 按季度评估 `domain/CompatibilityAliases.scala` 使用情况，逐步下线可替换 alias。
3. 保持订单图片上传链路统一由 `OrderImageFileService` 入口编排，避免 API 层回退为直连 `StoredImageService`。
4. 继续将前端业务事实判断收敛到后端（结算与通知场景已完成，新增场景默认按同一策略实施）。
5. 可维护性审计脚本与目录规则同步演进：新增规则先以 warn 观察，再在稳定后升级为 fail。
6. 例行验证维持不变：后端编译、前端 typecheck、类型安全审计、可维护性审计。
