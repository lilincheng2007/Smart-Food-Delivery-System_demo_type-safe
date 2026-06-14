# Type-safe Delivery Platform

一个类型安全的外卖平台全栈项目，覆盖顾客、商家、骑手、管理员四类角色。前端使用 Vite + React + TypeScript，后端使用 Scala 3 + http4s + Circe + PostgreSQL。所有业务交互通过统一 `APIMessage` 网关完成，前端只保存 JWT/角色等会话信息，订单、钱包、库存、优惠、通知等真实业务数据以后端数据库为准。

## 功能概览

### 顾客端

- 注册/登录、JWT 会话、角色路由保护。
- 浏览商家目录、店内点餐、普通菜品与套餐选择、购物车和“再来一单”。
- 商家营业状态、库存/售罄/每单限购校验，结算前后端双重约束。
- 收货联系人管理，必须且只能有一组默认联系人。
- 结算页支持优惠券、商家优惠、平台优惠、配送费和结构化价格明细。
- 钱包充值、余额校验、取消订单、退款申请/申诉、图片上传。
- 订单详情、状态时间线、预计出餐/送达、完成确认、商家/骑手评价与评价投票。
- 订单聊天、图片消息、未读数和全局通知中心，通知已读状态后端持久化。
- 吃货积分与等级：确认完成订单后按实付金额累计积分，升级发券。
- AI 智能搜索、AI 饮食周报、AI 订单进度叙事、AI 评价摘要。

### 商家端

- 多店铺管理、店铺入驻申请、店铺资料、公告、描述和图片上传。
- 营业状态、每周营业时间、节假日特殊营业时间和临时暂停接单。
- 商品创建/编辑、普通菜品与套餐、套餐类别/选项、图片上传、上下架。
- 库存模式：无限库存、今日库存、售罄、每单限购。
- 店铺优惠和单菜品优惠管理，支持排期、启停、现价折扣与统一校验。
- 商家订单接单/拒单、备餐时间、主动延迟备餐、出餐完成。
- 退款处理、订单聊天、顾客评价回复和商家评价看板。
- AI 店铺描述、AI 菜品描述、AI 经营建议。

### 骑手端

- 查看可抢订单、抢单、配送中任务。
- 更新配送状态，完成配送后生成结算结果。
- 薪资累计、服务能量值、超时统计。
- 100 能量兑换超时免责卡，超时送达时自动或手动使用免责卡。
- 同一骑手最多同时配送 5 单。
- 订单聊天、图片消息、未读提醒、骑手评分与评价列表。

### 管理员端

- 店铺入驻申请审核。
- 顾客/商家退款仲裁。
- 首页订单监控：今日订单数、今日成交额、待处理退款、异常订单、商家超时、骑手超时。
- 平台优惠管理：新增、编辑、启停、排期校验。
- 全局通知已读状态同样走后端 API。

### 一致性与可靠性

- 订单列表按顾客、商家、骑手精准查询，订单项批量加载，避免全表过滤和 N+1。
- 下单保存订单价格快照与结构化结算明细，历史订单可追溯价格组成。
- 订单状态流转集中在后端状态机，统一追加状态时间线和系统聊天通知。
- 商品库存下单时锁行校验并扣减，前端只做交互约束，后端负责最终一致性。
- 通知已读、聊天消息、图片资源、退款和评价均后端持久化。

## 技术栈

| 层 | 技术 |
|---|---|
| 前端 | Vite 8、React 19、TypeScript 5.9、React Router、Zustand、shadcn/ui、Radix UI、Tailwind CSS v4 |
| 后端 | Scala 3.3、Cats Effect、http4s、Circe、JWT、HikariCP |
| 数据库 | PostgreSQL 16（Docker Compose 可一键启动） |
| AI | OpenAI Chat Completions 兼容接口（后端代理调用） |

## 目录结构

```text
.
├── README.md                    # 项目总览
├── README.full.md               # 完整说明文档
├── package.json                 # 根目录联动启动脚本
├── API_INVENTORY.md             # API 盘点文档
├── AGENTS.md                    # 项目研发约定与子代理说明
├── sample/                      # 结构参照样例，不是主应用
├── .codebuddy/skills/           # 项目辅助技能，例如 type-safety-audit
├── frontend/
│   ├── README.md
│   └── src/
│       ├── apis/                # 前端 APIMessage，一 API 一文件
│       ├── objects/             # 与后端对象对应的 TS 契约
│       ├── pages/               # 登录、注册、顾客、商家、骑手、管理员、聊天页面
│       ├── stores/              # Zustand 页面状态与拆分 helper
│       ├── components/          # 通用组件、通知中心、UI 组件
│       ├── hooks/               # app chrome、鉴权等 hooks
│       └── lib/                 # 会话、计价、库存、时间线等工具
└── backend/
    ├── README.md
    ├── build.sbt
    ├── docker-compose.yml
    ├── docker/init-databases.sql
    └── src/
        ├── Main.scala           # 服务入口，监听 8787
        ├── DeliveryRoutes.scala # API 网关和公开静态资源路由汇总
        ├── user/                # 登录注册、顾客资料、钱包、优惠券
        ├── merchant/            # 商家、店铺、商品、营业时间、优惠、订单处理
        ├── order/               # 下单、订单、聊天、退款、通知已读、状态机
        ├── rider/               # 骑手、抢单、配送状态、免责卡
        ├── admin/               # 入驻审核、退款仲裁、订单监控、平台优惠
        ├── review/              # 评价、回复、投票、图片
        ├── ai/                  # AI 搜索、周报、文案、经营建议
        ├── platform/            # APIMessage、HTTP、JSON 聚合等平台基础设施
        ├── auth/                # JWT 与认证鉴权
        ├── db/                  # 数据库连接、事务与初始化入口
        ├── bootstrap/           # 种子数据与启动导入
        ├── domain/              # 跨模块 ID、角色、稳定枚举
        ├── media/               # 图片存储、读取、校验和迁移
        └── promotion/           # 促销、优惠券和结算辅助领域
```

## 快速启动

### 环境要求

- Node.js + npm
- JDK 21 推荐（JDK 17+ 可用；根目录脚本会优先使用 Homebrew `openjdk@21`）
- sbt
- Docker（用于本地 PostgreSQL）

### 一键启动

```bash
npm install
npm run dev
```

`npm run dev` 会自动：

1. 启动 `backend/docker-compose.yml` 中的 PostgreSQL；
2. 等待数据库可用；
3. 启动后端 `http://localhost:8787`；
4. 等待 `POST /api/catalogapi` 可用；
5. 启动前端 Vite 开发服务器。

### 分开启动

```bash
# 终端 1：数据库
npm run dev:db

# 终端 2：后端
cd backend
sbt run

# 终端 3：前端
cd frontend
npm install
npm run dev
```

前端默认通过 Vite proxy 将 `/api` 转发到 `http://localhost:8787`。

## 演示账号

| 角色 | 账号 | 密码 |
|---|---|---|
| 顾客 | `customer_demo` | `123456` |
| 商家 | `merchant_demo` | `123456` |
| 骑手 | `rider_demo` | `123456` |
| 管理员 | `admin` | `123456` |

## 常用命令

| 命令 | 说明 |
|---|---|
| `npm run dev` | 根目录一键启动数据库、后端、前端 |
| `npm run dev:db` | 仅启动 PostgreSQL |
| `npm run dev:backend` | 启动数据库后运行后端 |
| `npm run dev:frontend` | 仅启动前端 |
| `npm run typecheck --prefix frontend` | 前端类型检查 |
| `npm run lint --prefix frontend` | 前端 lint |
| `npm run build --prefix frontend` | 前端生产构建 |
| `cd backend && sbt -batch compile` | 后端编译检查 |
| `.codebuddy/skills/type-safety-audit/scripts/check-type-safety.sh /Users/leonli/Desktop/Type-safe_project` | 类型安全与结构审计 |

## API 约定

后端统一暴露 APIMessage 网关：

```text
POST /api/{apiName}
```

前端通过 `frontend/src/apis/shared/sendAPI.ts` 发送 `APIMessage`；后端通过 `backend/src/platform/api/APIMessage.scala` 注册、解码、鉴权并执行。需要鉴权的 API 使用 `Authorization: Bearer <token>`，网关根据注册角色校验权限。

示例：

| 能力 | apiName |
|---|---|
| 目录 | `catalogapi` |
| 登录 | `loginapi` |
| 顾客结算 | `checkoutapi` |
| 商家接单 | `merchantorderacceptapi` |
| 商家出餐完成 | `merchantorderreadyapi` |
| 骑手抢单 | `ridergraborderapi` |
| 订单聊天 | `customersendorderchatmessageapi` / `merchantsendorderchatmessageapi` / `ridersendorderchatmessageapi` |
| 通知已读 | `notificationmarkreadapi` / `notificationmarkallreadapi` |
| 管理员订单监控 | `adminordermonitorapi` |
| 平台优惠管理 | `adminplatformpromotionsapi` / `adminplatformpromotionsupdateapi` |
| 商家评价 | `merchantreviewsapi` |
| AI 搜索 | `aisearchapi` |

除 APIMessage 网关外，图片通过公开静态路由访问：

```text
GET /api/merchant/store-images/{fileName}
GET /api/merchant/product-images/{fileName}
GET /api/orders/refund-images/{fileName}   # 订单图片，当前包含退款凭证、订单聊天图片、订单备注图片
GET /api/reviews/images/{fileName}
```

## 环境变量

### 后端

| 变量 | 默认值 | 说明 |
|---|---|---|
| `DB_HOST` | `127.0.0.1` | PostgreSQL 主机 |
| `DB_PORT` | `5432` | PostgreSQL 端口 |
| `DB_NAME` | `delivery_backend` | 数据库名 |
| `DB_USER` | `postgres` | 数据库用户 |
| `DB_PASSWORD` | `postgres` | 数据库密码 |
| `DB_MAX_POOL_SIZE` | `10` | Hikari 连接池大小 |
| `DB_CONNECTION_TIMEOUT_MS` | `3000` | 连接超时 |
| `DB_POOL_NAME` | `delivery-backend-pool` | Hikari 连接池名称 |
| `JWT_SECRET` | 开发默认值 | JWT 签名密钥，生产环境必须覆盖 |
| `SERVICE_INTERNAL_TOKEN` | `dev-internal-token` | 内部服务调用令牌，对应 `X-Internal-Token` |
| `OPENAI_API_KEY` | 无 | AI 功能所需 Key |
| `OPENAI_BASE_URL` | `https://api.openai.com/v1` | OpenAI 兼容 API 地址 |
| `OPENAI_MODEL` | `gpt-4o-mini` | AI 使用模型 |

未配置 `OPENAI_API_KEY` 时，普通业务功能可运行；AI 搜索、AI 周报、AI 文案等能力会提示服务未配置或返回兜底结果。

### 前端

| 变量 | 默认值 | 说明 |
|---|---|---|
| `VITE_BACKEND_URL` | `http://localhost:8787` | Vite dev proxy 后端目标 |
| `VITE_API_BASE` | `/api` | 前端 API base path |

## 开发约定

- 前端只通过 HTTP API 获取和提交业务数据，不把浏览器本地状态作为真实数据源。
- 新增业务 API 时，后端 `XxxAPIMessage.scala` 与前端 `src/apis/**/XxxAPI.ts` 必须一一对应。
- 领域对象放 `objects/<module>/`，请求/响应 wrapper 放 `objects/<module>/apiTypes/`。
- 页面按 sample 风格拆分：`index.tsx` 做装配，局部 UI 放 `components/`，函数放 `functions/`，局部常量/类型放 `objects/`，复杂协调优先进入 `hooks/`。
- 后端 Scala 代码使用不可变绑定 `val`，避免新增 `var`。
- 新增业务路由优先使用 `APIMessage` / `APIWithRoleMessage`，不要新增零散字符串路由。
- AI 能力优先复用 `backend/src/ai/utils/OpenAIClient.scala`。

更多说明见：

- [`frontend/README.md`](frontend/README.md)
- [`backend/README.md`](backend/README.md)
