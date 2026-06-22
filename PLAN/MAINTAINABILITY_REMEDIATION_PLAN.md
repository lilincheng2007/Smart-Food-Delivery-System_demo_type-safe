# Type-safe_project 可维护性整改计划

> 本文档用于记录当前静态审计后形成的结构、分层与类型契约整改计划。计划只描述后续修改步骤，不代表已修改程序；每批完成后再补充进展、验证结果与遗留问题。

## 1. 总目标

1. 继续保持前后端类型安全主线：业务 API 默认通过后端 `APIMessage` 与前端 `sendAPI` 对齐，避免新增绕过统一网关的业务调用。
2. 让后端 `api/` 层回到入口编排职责：复杂流程进入 `services/`，业务规则进入 `validators/`，数据库访问留在 `tables/`，媒体能力保持在 `media/`。
3. 提升接口契约可发现性：所有 `*Request` / `*Response` 统一落到前后端 `objects/*/apiTypes/`，API 文件只声明消息与调用函数。
4. 降低重复实现与隐式兜底：前端通用图片读取逻辑集中复用，后端 JSON codec 逐步减少自动派生兜底。
5. 继续坚持“后端为业务事实源”：结算、通知、订单、退款、评价、钱包、库存和促销等关键状态不回退到前端本地推导。
6. 让文档、审计脚本和代码结构同步演进，避免计划完成后规则缺口继续扩散。

## 2. 修改原则

- 小步迁移，避免一次性大重构。
- 优先处理会扩散坏模式的问题，再处理局部大文件和低风险重复代码。
- 每批只处理一组边界清晰的问题，并保持 HTTP 路径、API 名称、响应字段和用户行为兼容。
- 新增文件优先按现有目录职责落位，不新增 `*Support`、宽泛 helper 或跨层依赖。
- API 层保留鉴权上下文、请求解析、服务调用和响应返回；不要继续承载状态机、计价、AI prompt/parse 等重业务逻辑。
- 每批完成后按影响范围执行验证，并同步更新 `API_INVENTORY.md`、`DIRECTORY_LAYERING_GUIDE.md`、`README.full.md` 等必要文档。

## 3. 批次计划

### 第一批：修复前端 DTO 落位与小型重复实现

目标：先处理低风险、可快速收敛且会影响后续可发现性的结构问题。

优先文件：

- `frontend/src/apis/admin/AdminPlatformPromotionsAPI.ts`
- `frontend/src/objects/admin/apiTypes/PlatformPromotionsResponse.ts`
- `frontend/src/lib/local-image-file.ts`
- `frontend/src/apis/order/CustomerOrderImageFileAPI.ts`
- `frontend/src/apis/order/CustomerRefundImageFileAPI.ts`
- `frontend/src/apis/order/MerchantOrderImageFileAPI.ts`
- `frontend/src/apis/order/RiderOrderImageFileAPI.ts`
- `frontend/src/apis/review/CustomerReviewImageFileAPI.ts`

建议方向：

1. 将 `PlatformPromotionsResponse` 从 `AdminPlatformPromotionsAPI.ts` 迁入 `frontend/src/objects/admin/apiTypes/PlatformPromotionsResponse.ts`。
2. `AdminPlatformPromotionsAPI.ts` 只保留 `APIMessage` 类和 `fetchAdminPlatformPromotionsIO`，通过 `import type` 引入响应类型。
3. 所有图片上传 API 统一复用 `frontend/src/lib/local-image-file.ts` 中的 `fileToBase64` 与图片错误转换能力。
4. 保持现有 API 名称、请求字段和返回类型不变。

完成标准：

- 前端 `*Response` 类型不再内联定义在 API 文件中。
- 图片上传 API 不再复制 `FileReader` 到 base64 的实现。
- 前端类型检查通过，相关 API 调用路径保持兼容。

### 第二批：补齐退款状态机服务入口

目标：让退款接受、驳回、仲裁等状态变化集中在同一 workflow 服务中，避免后续通知、钱包、审计日志逻辑散落。

优先文件：

- `backend/src/admin/api/AdminRefundRejectAPIMessage.scala`
- `backend/src/admin/api/AdminRefundAcceptAPIMessage.scala`
- `backend/src/order/services/RefundWorkflowService.scala`
- 可能关联的商家退款处理 API 与订单表 / 顾客资料表调用点

建议方向：

1. 在 `RefundWorkflowService` 中补齐 `rejectByAdmin` 等明确入口。
2. 将 `AdminRefundRejectAPIMessage` 中的订单状态更新、顾客历史订单同步等流程迁入 service。
3. 若商家驳回退款存在同类散落逻辑，同步收敛为 `rejectByMerchant` 或同类命名。
4. APIMessage 只负责校验角色上下文、传入 `orderId` / `reason` 并返回 `OkResponse`。

完成标准：

- 管理员退款接受与驳回都通过 `RefundWorkflowService` 编排。
- 退款状态变更规则、订单写回、顾客订单快照同步有统一入口。
- 后续新增退款通知或审计日志时不需要修改多个 APIMessage 文件。

### 第三批：拆分评价提交流程

目标：降低 `CustomerSubmitOrderReviewAPIMessage` 厚度，把商家评价、骑手评价、骑手评分与薪资更新拆入服务层。

优先文件：

- `backend/src/review/api/CustomerSubmitOrderReviewAPIMessage.scala`
- `backend/src/review/services/ReviewSubmissionService.scala`
- `backend/src/review/validators/ReviewImageValidator.scala`
- `backend/src/rider/services/RiderRatingService.scala` 或等价服务入口
- `backend/src/review/tables/*`
- `backend/src/rider/tables/*`

建议方向：

1. 新增 `ReviewSubmissionService.submitOrderReview(...)` 承载完整评价提交流程。
2. 保留并扩展 `ReviewImageValidator`，将评分、文字、图片 URL 等输入边界集中校验。
3. 将骑手评价创建、评分重算、薪资更新拆到 `rider` 或 `review` 下语义明确的服务入口。
4. `CustomerSubmitOrderReviewAPIMessage` 最终只调用服务并返回 `OkResponse(ok = true)`。

完成标准：

- APIMessage 中不再直接创建 `MerchantReview` / `RiderReview` 或直接更新骑手账号。
- 评价重复提交、订单归属、订单状态、评分边界有稳定校验入口。
- 评价流程扩展通知、积分或风控时可在 service 层集中处理。

### 第四批：收敛结算提交流程编排

目标：把提交订单时的库存扣减、订单写入、促销使用次数、checkout request、钱包与券消费统一收敛到服务层。

优先文件：

- `backend/src/order/api/CheckoutAPIMessage.scala`
- `backend/src/order/services/OrderCheckoutService.scala`
- `backend/src/order/services/CheckoutCommandService.scala`
- `backend/src/order/services/CheckoutInventoryService.scala`
- `backend/src/order/services/CheckoutPricingService.scala`
- `backend/src/promotion/services/VoucherRedemptionService.scala`

建议方向：

1. 新增 `CheckoutCommandService.submitCheckout(...)`，或扩展 `OrderCheckoutService` 提供完整提交入口。
2. 将库存扣减、订单持久化、促销使用次数更新、请求记录写入、顾客钱包与券更新统一放入提交服务。
3. `CheckoutAPIMessage` 只读取请求、确认顾客账号、调用提交服务并装配响应。
4. 保持 `CheckoutQuoteAPI` 与 `CheckoutAPI` 对同一计价服务的依赖，避免 quote 与真实提交结果分叉。

完成标准：

- `CheckoutAPIMessage` 不再直接调用多个 table 和促销持久化 helper。
- 结算提交的事务边界、失败原因和写入顺序集中可见。
- 前端仍只消费后端 quote 和 checkout 结果，不新增本地业务事实判断。

### 第五批：AI API 降厚与跨 API 依赖收敛

目标：将 AI prompt 构造、模型调用、响应解析和业务数据查询从 APIMessage 中拆出，并避免 API 层互相调用。

优先文件：

- `backend/src/ai/api/AISearchAPIMessage.scala`
- `backend/src/ai/api/AIDietWeeklyReportAPIMessage.scala`
- `backend/src/ai/api/AIMerchantBusinessSuggestionsAPIMessage.scala`
- `backend/src/ai/api/AIOrderProgressNarrativesAPIMessage.scala`
- `backend/src/ai/api/AIReviewSummaryAPIMessage.scala`
- `backend/src/ai/services/AISearchService.scala`
- `backend/src/ai/prompts/*` 或 `backend/src/ai/utils/*PromptBuilder.scala`
- `backend/src/ai/utils/*ResponseParser.scala`
- `backend/src/merchant/services/CatalogQueryService.scala`

建议方向：

1. 先抽 `AISearchService`，将 catalog 查询、prompt 构造、OpenAI 调用、JSON 解析和结果过滤集中处理。
2. 用 `merchant/services/CatalogQueryService` 或已有领域查询服务替代 `CatalogAPIMessage().plan(connection)` 这类 API 调 API。
3. 后续逐个拆其它 AI API 的 prompt builder 与 response parser。
4. 保持 `OpenAIClient` 仍为统一模型调用入口，不在业务 API 中散落 HTTP 客户端实现。

完成标准：

- AI APIMessage 文件只做输入检查、服务调用和响应返回。
- AI 模块不直接依赖其它模块的 APIMessage。
- prompt 模板、解析容错、fallback 逻辑有清晰归属，便于独立测试和调整。

### 第六批：移除 routes 中的 JSON 自动派生兜底

目标：让模块化 JSON codec 成为真实契约入口，避免 `io.circe.generic.auto.*` 隐式掩盖缺失 codec。

优先文件：

- `backend/src/*/routes/*Routes.scala`
- `backend/src/platform/json/ApiJsonCodecs.scala`
- `backend/src/*/json/*JsonCodecs.scala`

建议方向：

1. 先盘点所有 routes 中的 `io.circe.generic.auto.*` 引入。
2. 按模块确认 `*JsonCodecs.scala` 已覆盖该 routes 会编码 / 解码的请求、响应和领域对象。
3. 逐个移除 routes 中的自动派生 import，缺失 codec 通过编译错误补齐。
4. 保持 `ApiJsonCodecs.given` 为唯一聚合导入入口。

完成标准：

- routes 不再依赖 `io.circe.generic.auto.*` 作为业务 codec 兜底。
- 新增响应类型如果缺 codec，会在编译或局部验证中暴露。
- `platform/json/ApiJsonCodecs.scala` 继续只做聚合导出，不重新变厚。

### 第七批：文档与审计规则同步

目标：把本轮整改后的结构约束固化到文档和脚本中，防止同类问题回流。

优先文件：

- `API_INVENTORY.md`
- `DIRECTORY_LAYERING_GUIDE.md`
- `AGENTS.md`
- `README.full.md`
- `backend/README.md`
- `frontend/README.md`
- `.codebuddy/skills/maintainability-audit/scripts/check-maintainability.sh`
- `.codebuddy/skills/type-safety-audit/scripts/check-type-safety.sh`

建议方向：

1. 更新 API inventory 中新增 / 迁移的 API 契约说明。
2. 在目录分层文档中补充“API 不调用 APIMessage”“routes 不使用 generic auto 兜底”等规则。
3. 扩展可维护性审计脚本：检查前端 API 文件内联 `*Request` / `*Response`、重复 `fileToBase64`、routes 自动派生 import、AI API 调其它 APIMessage 等模式。
4. 更新 README 中的当前维护状态、验证命令和已知边界。

完成标准：

- 文档描述与实际目录结构一致。
- 审计脚本能覆盖本轮发现的主要结构回退风险。
- 后续新增代码按文档即可找到正确落点。

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

涉及后端局部 Scala 文件时可优先执行：

```bash
cd backend && sbt -batch compile
```

## 5. 当前进展记录

| 日期 | 进展 | 备注 |
|---|---|---|
| 2026-06-22 | 第一批完成：修复前端 DTO 落位与图片上传重复实现 | 新增 `frontend/src/objects/admin/apiTypes/PlatformPromotionsResponse.ts`，`AdminPlatformPromotionsAPI.ts` 改为 `import type` 契约引用；`CustomerOrderImageFileAPI.ts`、`CustomerRefundImageFileAPI.ts`、`MerchantOrderImageFileAPI.ts`、`RiderOrderImageFileAPI.ts`、`CustomerReviewImageFileAPI.ts` 统一复用 `@/lib/local-image-file` 的 `fileToBase64`；执行 `npm run typecheck --prefix frontend` 通过，相关改动文件 lint 无新增诊断。 |
| 2026-06-22 | 第二批完成：退款驳回流程收敛到 workflow 服务 | `RefundWorkflowService` 新增 `rejectByAdmin`、`rejectByMerchant` 与顾客历史订单同步 helper；`AdminRefundRejectAPIMessage`、`MerchantRefundRejectAPIMessage` 收敛为薄 API 入口并复用服务；执行 `cd backend && sbt -batch compile` 通过，相关改动文件 lint 无新增诊断。 |
| 2026-06-22 | 第三批完成：评价提交流程 service 化 | 新增 `review/services/ReviewSubmissionService.scala` 承接商家评价、骑手评价与骑手评分回写；`CustomerSubmitOrderReviewAPIMessage` 收敛为输入装配 + service 调用；执行 `cd backend && sbt -batch compile` 通过，相关改动文件 lint 无新增诊断。 |
| 2026-06-22 | 第四批完成：结算提交流程收敛到命令服务 | 新增 `order/services/CheckoutCommandService.scala`，统一结算提交链路；`CheckoutAPIMessage` 收敛为薄入口；执行 `cd backend && sbt -batch compile` 与 `npm run typecheck --prefix frontend` 通过。 |
| 2026-06-22 | 第五批完成：AI API 降厚与跨 API 依赖收敛 | 新增 `CatalogQueryService` 与 `ai/services/*`（搜索、周报、经营建议、进度叙事、评价摘要）；`AISearchAPIMessage`、`AIDietWeeklyReportAPIMessage`、`AIMerchantBusinessSuggestionsAPIMessage`、`AIOrderProgressNarrativesAPIMessage`、`AIReviewSummaryAPIMessage` 收敛为薄入口，移除 AI 模块内 API 调 API；执行 `cd backend && sbt -batch compile` 通过。 |
| 2026-06-22 | 第六批完成（阶段一）：移除 AI routes 的 JSON 自动派生兜底 | `AIRoutes.scala` 去除 `io.circe.generic.auto.*`；`AIJsonCodecs.scala` 补齐 AI APIMessage 与相关请求/响应 codec；执行 `cd backend && sbt -batch clean compile` 与 `cd backend && sbt -batch compile` 通过。 |
| 2026-06-22 | 第七批完成：文档与审计规则同步 | 更新 `DIRECTORY_LAYERING_GUIDE.md`、`README.full.md`、`API_INVENTORY.md`；扩展 maintainability/type-safety 审计脚本检查 AI API 跨层依赖、AIRoutes 去兜底、前端 API 内联 DTO 与重复 `fileToBase64`；执行两类审计脚本通过（可维护性脚本保留 1 条 routes generic.auto 迁移告警）。 |

### 下一批建议（第六批阶段二）

1. 从 `admin/routes/AdminRoutes.scala` 开始，逐模块移除 `io.circe.generic.auto.*` 并补齐模块 `json/*JsonCodecs.scala` 中的 APIMessage/request/response codec。
2. 每移除一个模块都执行 `cd backend && sbt -batch compile`，确保缺失 codec 立即暴露并补齐。
3. 完成全部 routes 去兜底后，升级审计脚本中 routes generic.auto 检查为失败级别。
4. 同步更新计划文档“仍需优化的结构”为清零状态。

## 6. 当前代码现状快照

### 已改善的结构

- 后端已有明确的 `api/`、`services/`、`validators/`、`tables/`、`objects/`、`json/`、`media/` 分层约定。
- `backend/src/shared` 已不再作为业务实现落点，基础设施和业务对象基本回到真实归属模块。
- `*APIMessageSupport.scala` 这类模糊支撑文件当前未发现新增残留。
- 图片存储能力已有独立 `backend/src/media/` 模块，订单图片上传已统一经 `OrderImageFileService` 编排。
- 结算预估已通过 `checkoutquoteapi` 后端化，前端结算页主要展示后端 quote。
- 通知 feed 已通过 `notificationfeedapi` 后端聚合，前端通知中心主要负责展示、轮询和已读回写。
- 页面私有 Zustand store 已就近放入 `pages/{Page}/stores/`，`frontend/src/stores` 只保留全局状态。
- 模块 JSON codec 已拆分到各业务模块，`platform/json/ApiJsonCodecs.scala` 当前保持聚合导出职责。
- `PlatformPromotionsResponse` 已迁入 `frontend/src/objects/admin/apiTypes/`，前端 API 契约可发现性提升。
- 多个前端图片上传 API 已统一复用 `@/lib/local-image-file` 的 `fileToBase64`，重复实现已收敛。
- 评价提交流程已迁入 `ReviewSubmissionService`，`CustomerSubmitOrderReviewAPIMessage` 改为薄入口并复用既有校验器。
- 结算提交链路已迁入 `CheckoutCommandService`，`CheckoutAPIMessage` 改为薄入口。
- AI 搜索/周报/经营建议/进度叙事/评价摘要已下沉到 `ai/services`，AI API 层职责收敛。
- 所有 routes 已移除 `io.circe.generic.auto.*`，并由各模块 JsonCodecs 显式提供 APIMessage 与响应契约编解码。
- 可维护性与类型安全审计脚本已将 routes 自动派生兜底纳入全模块检查。

### 仍需优化的结构

- 本计划内的主要结构问题已完成；后续以新增 API/新增模块时的常规守护为主。

## 7. 后续维护记录空间

后续每完成一批整改，在此追加记录：

| 日期 | 批次 | 已完成内容 | 验证结果 | 后续遗留 |
|---|---|---|---|---|
| 2026-06-22 | 第六批阶段二 | `admin/merchant/order/review/rider/user` routes 去除 `generic.auto`，各模块 JsonCodecs 补齐 APIMessage codec，审计脚本升级为全 routes 检查 | `cd backend && sbt -batch compile` 通过；类型安全审计 `48 PASS / 0 FAIL`；可维护性审计 `23 PASS / 0 WARN / 0 FAIL` | 本计划内主要结构问题已完成，后续仅需新增 API 时同步维护 codec 与文档 |

## 8. 计划完成后的持续维护建议

1. 每次新增或修改 API 时，同步检查后端 `XxxAPIMessage.scala`、前端 `XxxAPI.ts`、前后端 `objects/*/apiTypes/` 和 `API_INVENTORY.md` 是否一致。
2. 每次触碰 APIMessage 时顺手判断是否存在流程过重、table 直连过多、规则判断过多的问题，必要时迁入 service / validator。
3. 每次新增 JSON 响应类型时优先维护模块 `json/*JsonCodecs.scala`，不要依赖 routes 自动派生兜底。
4. 每次新增图片上传能力时复用既有媒体模块与前端图片工具，不在 API 文件内复制 base64 读取或存储逻辑。
5. 每轮整改结束后执行后端编译、前端 typecheck、类型安全审计和可维护性审计，并把结果记录到本计划。
