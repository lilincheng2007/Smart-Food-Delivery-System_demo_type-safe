# Type-safe Delivery Platform 项目规则

> 本文件是当前仓库的研发规则入口。项目是仅 Web 的类型安全外卖平台，前端负责展示与交互，后端负责真实业务状态、鉴权、校验和持久化。

## 1. 项目定位

- 项目覆盖顾客、商家、骑手、管理员四类角色，并包含评价、退款、通知、聊天、AI 辅助等完整业务链路。
- 所有真实业务数据必须以后端 PostgreSQL 为准：订单、钱包、库存、优惠、通知已读、聊天、评价、退款等都不能只存在前端状态中。
- 前端 Zustand、localStorage 仅用于页面状态、会话信息、展示缓存或 UI 偏好。
- 新增业务能力时优先保持现有类型安全结构，不新增绕过 `APIMessage` 的零散业务路由。

## 2. 技术栈与运行架构

| 层级 | 技术 |
|---|---|
| 前端 | Vite、React、TypeScript、React Router、Zustand、Tailwind CSS、shadcn/ui、Radix UI |
| 后端 | Scala 3、Cats Effect、http4s、Circe、JWT、HikariCP、PostgreSQL JDBC |
| 数据库 | PostgreSQL 16，Docker Compose 本地启动 |
| AI | OpenAI Chat Completions 兼容接口，后端统一代理调用 |

关键入口：

- 后端入口：`backend/src/Main.scala`，监听 `8787`，启动时初始化数据库、表结构、迁移和种子数据。
- 后端总路由：`backend/src/DeliveryRoutes.scala`，汇总所有业务模块 API，并注册静态图片路由。
- 前端入口：`frontend/src/main.tsx` 与 `frontend/src/router.tsx`。
- 前端 API 代理：Vite 默认将 `/api` 转发到 `http://localhost:8787`。

## 3. 本地启动

在仓库根目录：

```bash
npm install
npm run dev
```

该命令会启动 PostgreSQL、后端和前端。也可以分开启动：

```bash
npm run dev:db
cd backend && sbt run
cd frontend && npm install && npm run dev
```

演示账号：

| 角色 | 账号 | 密码 |
|---|---|---|
| 顾客 | `customer_demo` | `123456` |
| 商家 | `merchant_demo` | `123456` |
| 骑手 | `rider_demo` | `123456` |
| 管理员 | `admin` | `123456` |

## 4. 仓库结构

```text
frontend/                  # Vite + React Web 前端
  src/apis/                # 前端 APIMessage，一 API 一文件
  src/objects/             # TypeScript 领域对象与 API 类型
  src/pages/               # 页面与页面私有 components/hooks/objects/functions/stores
  src/stores/              # 仅全局 Zustand store
  src/components/          # 通用组件与 UI
  src/hooks/               # 会话、未读数、页面 chrome 等跨页面 hooks
  src/lib/                 # 计价、库存、时间线、媒体 URL、工具函数

backend/                   # Scala 3 + http4s 后端
  src/admin/               # 入驻审核、退款仲裁、平台优惠、订单监控
  src/ai/                  # AI 搜索、周报、文案、经营建议
  src/merchant/            # 商家、店铺、商品、营业、订单处理
  src/order/               # 下单、订单、退款、聊天、通知、状态流转
  src/review/              # 评价、回复、投票、图片
  src/rider/               # 骑手、抢单、配送、结算、免责卡
  src/user/                # 登录注册、顾客资料、钱包、优惠券
  src/platform/            # APIMessage、HTTP、JSON 聚合等平台基础设施
  src/auth/                # JWT 与认证鉴权基础能力
  src/db/                  # 数据库连接、事务与初始化入口
  src/bootstrap/           # 种子数据与启动导入
  src/domain/              # 跨模块 ID、角色、稳定枚举和通用 DTO
  src/media/               # 图片存储、读取、校验和迁移
  src/promotion/           # 促销、优惠券和结算辅助领域

DIRECTORY_LAYERING_GUIDE.md # 目录职责与分层规范
sample/                    # 结构参照样例，不是主应用
.codebuddy/skills/         # 项目辅助技能，例如 type-safety-audit、maintainability-audit
```

不要编辑或提交构建产物、依赖目录和临时缓存，例如 `frontend/dist`、`node_modules`、`backend/target`、`backend/.codex-tmp`。

## 5. APIMessage 类型安全规则

### 5.1 统一网关

所有业务 API 默认走：

```text
POST /api/{apiName}
```

业务图片文件访问是明确例外，例如：

```text
GET /api/merchant/store-images/{fileName}
GET /api/merchant/product-images/{fileName}
GET /api/orders/refund-images/{fileName}   # 订单图片，当前包含退款凭证、订单聊天图片、订单备注图片
GET /api/reviews/images/{fileName}
```

### 5.2 前后端文件对应

新增或修改业务 API 时必须保持命名和职责对应：

| 类型 | 后端 | 前端 |
|---|---|---|
| API 文件 | `backend/src/{module}/api/XxxAPIMessage.scala` | `frontend/src/apis/{module}/XxxAPI.ts` |
| 领域对象 | `backend/src/{module}/objects/` | `frontend/src/objects/{module}/` |
| 请求/响应对象 | `backend/src/{module}/objects/apiTypes/` | `frontend/src/objects/{module}/apiTypes/` |
| API 基础设施 | `backend/src/platform/api/` | `frontend/src/apis/shared/` |

规则：

1. 一 API 一文件。
2. 前端 `XxxAPI.ts` 与后端 `XxxAPIMessage.scala` 职责一致。
3. `*Request`、`*Response` 放入 `objects/*/apiTypes/`。
4. 真正的领域对象放在 `objects/*/` 根目录。
5. 前端通过 `sendAPI` 调用，不在页面中直接拼业务 API URL。
6. 后端需要鉴权的 API 使用 `APIWithRoleMessage` 并在对应 `Routes` 中声明角色。

## 6. 前端开发规则

- 页面按当前拆分风格组织：`index.tsx` 做装配，局部 UI 放 `components/`，函数放 `functions/`，局部常量/类型放 `objects/`，复杂数据协调优先进入 `hooks/`。
- 页面私有 Zustand store 放到对应 `pages/{Page}/stores/`；`frontend/src/stores/` 仅保留全局状态。
- 页面交互产生的业务数据必须通过 API 写入后端，并可由后端再次查询得到。
- 不要用浏览器状态伪造订单、库存、钱包、优惠、退款、评价等持久化业务结果。
- 顾客端、商家端、骑手端、管理员端的跨角色数据必须来自同一后端事实源。
- UI 改动优先使用现有 shadcn/ui、Radix、Tailwind 风格，避免引入新的 UI 框架。
- 已有弹窗和卡片通常需要显式设置 `bg-*`、`border`、`rounded-*`、`p-*`，因为基础 `DialogContent` 不默认提供完整卡片外观。

## 7. 后端开发规则

- Scala 代码优先使用不可变 `val`，不要新增 `var`。
- 新增业务表要提供 `TableInitializer`，并用 `ALTER TABLE ... ADD COLUMN IF NOT EXISTS` 兼容已有本地库。
- 数据库访问集中在 `tables/`，APIMessage 只做入口编排；复杂业务流程进入 `services/`，校验进入 `validators/`。
- 不要新增 `*APIMessageSupport.scala` 这类模糊支撑文件；已有同类文件触碰时按职责迁移到 `services/`、`validators/`、`utils/` 或 `media/`。
- 订单状态、库存扣减、钱包余额、优惠结算、退款状态等关键规则必须在后端校验。
- 长 SQL 的字段列表、`VALUES` 占位符和 `PreparedStatement` 绑定索引必须同步检查。
- JSON 编解码采用模块 `json/{Module}JsonCodecs.scala` 定义、`platform/json/ApiJsonCodecs.scala` 聚合导出；新增响应类型时优先维护对应模块 codec，并确认聚合入口可用。
- 不再新增 `backend/src/shared/**/*.scala`；基础设施拆到 `platform/auth/db/bootstrap/domain/media`，业务对象回到对应业务模块或 `promotion`。
- AI 能力统一复用 `backend/src/ai/utils/OpenAIClient.scala`，不要在业务 API 中散落 HTTP 客户端实现。

## 8. 可持续维护与分层规则

- 详细目录职责以 `DIRECTORY_LAYERING_GUIDE.md` 为准。
- 新增功能时优先使用 `.codebuddy/skills/layered-feature-development/` 的工作流确定落位。
- 结构审计时使用 `.codebuddy/skills/maintainability-audit/`，并区分历史债务与本次新增问题。
- 媒体/静态图片能力归属独立 `media` 模块；迁移实现时不改变既有 HTTP 公开路径。
- 维护策略采用“新代码遵守、触碰即收敛、小步迁移”，避免无验证的大规模重构。

## 9. 主要功能边界

- 顾客端：目录、点餐、购物车、结算、钱包、优惠券、订单、退款、评价、聊天、通知、AI 搜索、饮食周报、订单进度叙事和评价摘要。
- 商家端：多店铺、入驻申请、店铺资料、营业时间、商品/套餐、库存、优惠、接单出餐、退款处理、评价回复、AI 文案和经营建议。
- 骑手端：可抢订单、抢单、配送状态、完成配送、薪资结算、能量值、超时免责卡、骑手评分与评价展示、聊天。
- 管理员端：入驻审核、退款仲裁、订单监控、平台优惠管理。
- AI：搜索推荐、饮食周报、订单进度叙事、评价摘要、商家文案、菜品描述、经营建议。

## 10. 验证命令

常用检查：

```bash
npm run typecheck --prefix frontend
cd backend && sbt -batch compile
```

前端局部 lint：

```bash
cd frontend && npx eslint <changed-files>
```

全量前端 lint 当前可能包含既有问题；除非任务要求，不要顺手修复无关历史 lint。

类型安全与可维护性结构审计：

```bash
.codebuddy/skills/type-safety-audit/scripts/check-type-safety.sh /Users/leonli/Desktop/Type-safe_project
.codebuddy/skills/maintainability-audit/scripts/check-maintainability.sh /Users/leonli/Desktop/Type-safe_project
```

严格模式可将历史可维护性债务警告升级为失败：

```bash
STRICT=1 .codebuddy/skills/maintainability-audit/scripts/check-maintainability.sh /Users/leonli/Desktop/Type-safe_project
```

## 11. 文档维护

- 根目录 `README.md` 是项目总览。
- `README.full.md` 是完整说明文档，应随关键功能和配置变化同步维护。
- `API_INVENTORY.md` 用于记录 API 对齐情况；新增 API 后应同步更新。
- `DIRECTORY_LAYERING_GUIDE.md` 用于记录目录职责、分层边界和渐进迁移策略。
- `frontend/README.md` 和 `backend/README.md` 分别描述子项目细节。
- 本文件 `AGENTS.md` 记录研发规则；项目结构或约定变化后应同步维护。
