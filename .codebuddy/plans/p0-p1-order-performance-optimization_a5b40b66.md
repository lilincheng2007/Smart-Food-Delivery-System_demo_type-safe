---
name: p0-p1-order-performance-optimization
overview: 实施订单历史卡顿优化的 P0 与 P1：后端改为按条件精准查询并批量加载订单项，前端降低轮询压力并防止请求重叠。目标是在不改变 API 契约和业务功能的前提下，显著降低 10+ 历史订单用户的刷新延迟。
todos:
  - id: explore-hot-paths
    content: 使用 [subagent:code-explorer] 复核订单查询热点路径
    status: completed
  - id: batch-load-order-items
    content: 为订单项实现按订单集合批量加载
    status: completed
    dependencies:
      - explore-hot-paths
  - id: add-filtered-order-queries
    content: 为订单表新增精准过滤查询与计数
    status: completed
    dependencies:
      - batch-load-order-items
  - id: update-order-api-paths
    content: 改造顾客、商家、骑手订单读取链路
    status: completed
    dependencies:
      - add-filtered-order-queries
  - id: dedupe-customer-refresh
    content: 为顾客门户刷新实现并发去重
    status: completed
    dependencies:
      - update-order-api-paths
  - id: reduce-customer-polling
    content: 将顾客端轮询改为低频可见性刷新
    status: completed
    dependencies:
      - dedupe-customer-refresh
  - id: audit-and-verify
    content: 使用 [skill:type-safety-audit] 验证契约与刷新行为
    status: completed
    dependencies:
      - reduce-customer-polling
---

## Product Overview

实施订单历史卡顿优化的 P0 与 P1，聚焦订单读取链路和顾客端自动刷新策略，在不改变现有接口入参与响应结构、不改变业务功能的前提下，降低历史订单较多用户的刷新延迟与重复请求压力。

## Core Features

- 订单历史、待处理订单、可抢订单等列表改为按顾客、商家、骑手、状态等条件精准读取。
- 订单读取时批量加载订单项，避免每个订单单独读取明细造成的多次查询。
- 顾客端刷新防止请求重叠，已有刷新未结束时不再叠加新刷新。
- 自动刷新降低频率并结合页面可见性，减少无效后台请求。
- 用户主动操作后仍立即刷新最新订单、钱包、状态等真实后端数据。
- 优化过程保持现有页面展示、操作流程、错误提示与接口契约稳定。

## Tech Stack

- 后端：Scala 3、cats-effect IO、http4s、Circe、JDBC、PostgreSQL。
- 前端：React 19、TypeScript、Vite、Zustand、TaskIO/fetch API 封装。
- 约束：后端 Scala 不新增 `var`；不修改 API message 名称、请求字段、响应对象结构；不通过前端本地状态伪造真实业务数据；不主动新增文档文件。

## Architecture Solution

### Current Findings

- `backend/src/order/tables/order/OrderTable.scala` 的 `list` 当前读取全部订单，并在 `readOrder` 中逐订单调用 `OrderItemTable.listByOrderIdSync`。
- `backend/src/order/api/OrderAPIMessages.scala` 的顾客订单接口先全表读取再按 `customerId` 过滤。
- `backend/src/merchant/api/MerchantAPIMessages.scala` 的商家首页先全表读取再按店铺过滤。
- `backend/src/rider/api/RiderAPIMessages.scala` 的骑手首页、可抢单、抢单前校验、配送完成校验均存在全量列表读取。
- `frontend/src/pages/CustomerPortal/index.tsx` 存在 5 秒固定轮询。
- `frontend/src/stores/pages/use-customer-portal-store.ts` 的 `refreshPortal` 无请求去重，且顾客信息、目录、订单串行读取。

### Data Flow

```mermaid
flowchart LR
  A[页面或用户操作触发刷新] --> B[refreshPortal 并发去重]
  B --> C[顾客信息、目录、订单并行请求]
  C --> D[后端 API Message]
  D --> E[OrderTable 精准条件查询]
  E --> F[OrderItemTable 批量加载订单项]
  F --> G[组装原有响应结构]
  G --> H[Zustand 更新展示状态]
```

## Module Division

- **订单表查询模块**
- 文件：`backend/src/order/tables/order/OrderTable.scala`
- 职责：新增按顾客、商家集合、骑手、可抢状态、活跃配送计数等精准查询方法。
- 依赖：`OrderItemTable` 批量加载订单项。
- 对外接口保持表层内部复用，不改变 HTTP API 契约。

- **订单项批量加载模块**
- 文件：`backend/src/order/tables/orderitem/OrderItemTable.scala`
- 职责：新增按订单 ID 集合一次读取订单项，并按 `order_id` 分组返回。
- 依赖：`OrderItem` 对象与现有 `order_items_order_id_idx` 索引。
- 兼容：保留现有单订单读取方法，降低改动风险。

- **订单 API 热点链路**
- 文件：`backend/src/order/api/OrderAPIMessages.scala`
- 职责：顾客订单列表改用 `listByCustomerId`，响应仍为 `CustomerOrdersResponse`。

- **商家与骑手订单读取链路**
- 文件：`backend/src/merchant/api/MerchantAPIMessages.scala`
- 职责：商家首页按当前商家拥有店铺集合查询订单。
- 文件：`backend/src/rider/api/RiderAPIMessages.scala`
- 职责：骑手首页、可抢单、抢单校验、完成配送校验改用精准查询或计数。

- **顾客端刷新控制模块**
- 文件：`frontend/src/stores/pages/use-customer-portal-store.ts`
- 职责：为 `refreshPortal` 增加 in-flight 去重，并将独立请求改为并行获取后统一落状态。
- 文件：`frontend/src/pages/CustomerPortal/index.tsx`
- 职责：降低自动刷新频率，页面不可见时跳过轮询，用户操作后的即时刷新保持不变。

## Core Directory Structure

```text
Type-safe_project/
├── backend/src/order/tables/order/
│   └── OrderTable.scala                  # 修改：精准查询、批量组装订单项
├── backend/src/order/tables/orderitem/
│   └── OrderItemTable.scala              # 修改：按订单集合批量读取订单项
├── backend/src/order/tables/order/
│   └── OrderTableInitializer.scala       # 可选修改：补充组合索引
├── backend/src/order/api/
│   └── OrderAPIMessages.scala            # 修改：顾客订单改用精准查询
├── backend/src/merchant/api/
│   └── MerchantAPIMessages.scala         # 修改：商家订单改用店铺集合查询
├── backend/src/rider/api/
│   └── RiderAPIMessages.scala            # 修改：骑手订单改用精准查询与计数
└── frontend/src/
    ├── pages/CustomerPortal/index.tsx    # 修改：自动刷新节流与可见性判断
    └── stores/pages/use-customer-portal-store.ts # 修改：刷新请求去重与并行加载
```

## Key Code Structures

### 后端表层接口

```
def listByCustomerId(connection: Connection, customerId: UserId): IO[List[Order]]
def listByMerchantIds(connection: Connection, merchantIds: List[MerchantId]): IO[List[Order]]
def listByRiderId(connection: Connection, riderId: RiderId): IO[List[Order]]
def listAvailableUnassigned(connection: Connection): IO[List[Order]]
def countActiveByRider(connection: Connection, riderId: RiderId, excludingOrderId: Option[OrderId] = None): IO[Int]
```

### 订单项批量接口

```
def listByOrderIdsSync(connection: Connection, orderIds: List[OrderId]): Map[OrderId, List[OrderItem]]
```

### 前端刷新接口

```ts
refreshPortal: () => Promise<void>
```

实现要求：若刷新进行中，返回同一个 Promise；刷新完成后清理 in-flight 标记；失败路径也必须恢复可刷新状态。

## Technical Implementation Plan

### P0：后端精准查询与批量加载

1. 在 `OrderItemTable.scala` 中新增批量查询方法，空 ID 直接返回空 Map。
2. 在 `OrderTable.scala` 中拆分“读取订单行”和“附加订单项”逻辑，列表查询先读取订单行，再批量查询订单项。
3. 新增按顾客、商家集合、骑手、可抢单、活跃配送计数的查询方法，避免 API 层全表过滤。
4. 将顾客、商家、骑手 API 热点路径替换为新表层方法，保持原响应结构不变。
5. 视查询条件补充组合索引，例如顾客/商家/骑手与创建时间、状态与 rider 空值相关索引。

### P1：前端刷新降压

1. 在 store 层为 `refreshPortal` 增加 in-flight 去重，防止轮询、初始化、用户操作刷新叠加。
2. 将顾客信息、目录、订单读取改为并行执行，完成后一次性更新状态。
3. 将页面固定 5 秒轮询改为较低频率，并在 `document.hidden` 时跳过。
4. 页面重新可见或用户操作成功后仍调用 `refreshPortal`，确保用户看到后端最新状态。
5. AI 订单叙事保持已有 loading/日期保护，不再随短周期轮询重复触发无意义请求。

## Testing Strategy

- 后端编译：`cd backend && sbt -batch compile`
- 前端类型检查：`npm run typecheck --prefix frontend`
- 手工验证：
- 10 条以上历史订单顾客进入“我的”页，订单展示结构不变。
- 取消订单、确认完成、结算后仍即时刷新。
- 商家端订单列表、骑手端可抢单和配送状态仍正常。
- 浏览器网络面板中无重叠的顾客门户刷新请求。
- 类型安全验证：确认后端 case class 与前端 interface 未新增或删除契约字段。

## Agent Extensions

### SubAgent

- **code-explorer**
- Purpose: 复核订单读取热点路径、跨文件调用关系与现有实现模式。
- Expected outcome: 确认所有 P0/P1 修改点覆盖完整，避免遗漏商家、骑手、顾客端链路。

### Skill

- **type-safety-audit**
- Purpose: 审计前后端 API 消息与对象契约是否保持一致。
- Expected outcome: 验证无契约变更、无硬编码枚举替代、无前端状态越权替代后端真实数据。