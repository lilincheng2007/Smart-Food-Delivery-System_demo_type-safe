# Cursor 规则原文（复制为 `.cursor/rules/*.mdc`）

以下每个代码块保存为独立文件，路径：`类型安全项目/.cursor/rules/<文件名>`。

---

## `delivery-project-context.mdc`

```markdown
---
description: 外卖 Web 项目总览（方案三、目录约定、如何选用子代理规则）
alwaysApply: true
---

# 外卖 Web（方案三）

- **架构取向**：少量后端服务 + 域事件解耦（订单相关事件）；首版仅 **Web**。
- **前端**：[frontend/](frontend/)（Vite + React）；**只调 API**，不写浏览器内「伪后端」持久化。
- **后端（Scala）**：[backend/](backend/)（http4s + Circe + JWT + 内存种子）；开发端口默认 **8787**（`PORT` 可改），Vite 代理 `/api`。
- **建议目录（前端新功能）**：
  - `frontend/src/features/cart/`、`…/checkout/`：购物车与结算（**SliceCheckout**）
  - `frontend/src/features/order-tracking/`：订单详情与时间轴（**SliceOrderTracking**）
  - `frontend/src/features/merchant/`：商户接单与菜品运营（**SliceMerchantOps**）
  - `backend/src/main/scala/delivery/`：路由、store、后续 worker/outbox（**EventReliability**）
  - `frontend/src/domain-types/` 与 `backend/src/main/scala/delivery/model/`：契约对齐（**TypeSafetyShared**）

## 如何用「子代理」

在对话中 **@ 引用** 对应规则（或在规则面板启用），并一句话说明当前任务边界；每个子代理只改自己切片相关文件，避免一次对话混写全栈。协作说明见仓库根目录 [AGENTS.md](AGENTS.md)。
```

---

## `agent-slice-checkout.mdc`

```markdown
---
description: 子代理 · 购物车与结算（优惠券入口、与下单 API 对齐）
globs: "**/features/cart/**/*"
alwaysApply: false
---

# SliceCheckout（购物车 / 结算）

## 职责

- 购物车增删改、本地与服务端同步策略、金额与配送费展示。
- 结算页：地址选择、备注、优惠券入口（可先 mock）、提交订单前的校验与禁用态。
- 与订单创建 API 的请求/响应字段 **一一对应**；禁止在前端硬编码与契约不一致的状态名。

## 输出要求

- 列表/表单 **空态、加载、错误** 三种状态齐全；关键操作可撤销或二次确认。
- 与 **TypeSafetyShared** 规则对齐：从共享类型或 Zod schema 推导表单与 API 类型。

## 非职责

- 订单详情时间轴、配送轨迹（交给 SliceOrderTracking）。
- 商户后台菜品管理（交给 SliceMerchantOps）。
```

---

## `agent-slice-checkout-settlement.mdc`

```markdown
---
description: 子代理 · 结算页（与 cart 规则配合；匹配 checkout 目录）
globs: "**/features/checkout/**/*"
alwaysApply: false
---

# SliceCheckout · 结算子目录

与 **agent-slice-checkout** 同属「下单前」切片：仅当编辑 `features/checkout/**` 时启用本规则。

## 额外关注

- 提交订单 **幂等**：重复点击、网络重试时的按钮状态与 request id（若后端提供）。
- 明确区分「校验失败」与「服务端业务错误」（售罄、超配等）的展示文案。
```

---

## `agent-slice-order-tracking.mdc`

```markdown
---
description: 子代理 · 订单详情与时间轴（配送状态、实时更新）
globs: "**/features/order-tracking/**/*"
alwaysApply: false
---

# SliceOrderTracking（订单跟踪）

## 职责

- 订单详情页：状态时间轴、金额明细、配送信息展示。
- 实时更新：优先与后端约定 **SSE / WebSocket / 轮询** 之一；实现超时、断线重连与降级为手动刷新。
- 状态文案与 **共享枚举** 一致，禁止魔法字符串。

## 输出要求

- 终态（完成/取消）与进行中态视觉区分清晰；敏感信息按角色脱敏（若同一 Web 多角色）。

## 非职责

- 购物车与结算流程（SliceCheckout）。
- 商户侧接单后台（SliceMerchantOps）。
```

---

## `agent-slice-merchant-ops.mdc`

```markdown
---
description: 子代理 · 商户运营（接单、备餐、营业、菜品上下架）
globs: "**/features/merchant/**/*"
alwaysApply: false
---

# SliceMerchantOps（商户运营）

## 职责

- 订单列表与接单/拒单/出餐操作；备餐时间、营业状态切换。
- 菜品分类与上下架、库存或售罄标记（按产品定义）。
- 权限：商户会话与路由守卫；操作反馈（toast/inline error）完整。

## 输出要求

- 高频操作路径短；表格或列表支持分页或虚拟滚动（数据量上来时）。
- 与 **TypeSafetyShared** 共用订单状态、菜品状态枚举。

## 非职责

- C 端浏览店铺与加购（可归 SliceCheckout 或独立 discovery 切片，勿与本规则混写）。
- 消息队列与 Outbox（EventReliability）。
```

---

## `agent-event-reliability.mdc`

```markdown
---
description: 子代理 · 事件与可靠性（Outbox、消费者幂等、重试、死信）
globs: "**/backend/**/*"
alwaysApply: false
---

# EventReliability（事件可靠性）

## 职责

- **Transactional Outbox**：与业务事务同事务写入；独立转发进程或 worker 拉取投递。
- 消费者 **幂等键**（event id / business id + type）、至少一次语义下的重复处理安全。
- 重试退避、死信队列或人工介入钩子；关键指标：堆积、失败率、延迟。

## 输出要求

- 事件名、版本、payload 形状与 **TypeSafetyShared** 中契约一致；变更走版本或兼容策略。
- 日志结构化，便于关联 trace id。

## 非职责

- 具体 UI 与路由；仅触达后端（`backend/`）与 worker 代码。

## 目录约定

外卖演示 API 位于 [backend/](backend/)；后续 worker 可放在 `backend/src/worker/` 或并列包，保持与前端契约一致。
```

---

## `agent-type-safety-shared.mdc`

```markdown
---
description: 子代理 · 全链路类型与契约（枚举、DTO、Zod/OpenAPI）
globs: "**/domain-types/**/*"
alwaysApply: false
---

# TypeSafetyShared（类型与契约）

## 职责

- 订单/配送/支付（mock）等领域 **枚举与联合类型** 单一数据源；前端 `domain-types`、后端 DTO、OpenAPI 或 Zod schema 对齐。
- 错误模型：统一 `code` + `message` + 可选 `details`，前后端一致。
- API 变更时同步更新类型与调用方；优先代码生成或单文件导出避免漂移。

## 输出要求

- 禁止在业务组件内复制粘贴与契约重复的 interface；改为 import 共享定义。
-  breaking 变更在注释或 CHANGELOG 中一句话说明迁移方式。

## 非职责

- 不单独实现完整页面逻辑；以类型与契约为准，配合各切片 Agent 使用。
```

---

## `agent-type-safety-fe-be-correspondence.mdc` 补充

默认启用的前后端类型安全规则还包含以下全仓库底线：

- **不可变绑定**：后端 Scala 代码只能使用 `val`，不能新增 `var`；需要表达变化时，用不可变数据拷贝、函数返回值或数据库/API 状态承载。

---

## 可选：扩大 TypeSafety 匹配范围

若契约放在 `contracts/` 或 `packages/shared/`，可将 `agent-type-safety-shared.mdc` 的 `globs` 改为：

`globs: "**/domain-types/**/*"`

并复制多份规则文件分别绑定 `**/contracts/**/*`（Cursor 单文件仅支持一个 `globs` 字符串时）。
