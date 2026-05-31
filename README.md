# Type-safe Delivery Platform

一个类型安全的外卖平台全栈项目，包含顾客端、商家端、骑手端和可扩展的 AI 功能。前端通过统一的 `APIMessage` 调用后端网关，后端使用 Scala 3 + http4s + Circe + PostgreSQL 提供业务 API。

## 功能概览

- **账号体系**：顾客、商家、骑手三类角色注册与登录，JWT 鉴权。
- **顾客端**：浏览商家、AI 智能搜索、店内点餐、购物车、结算、钱包充值、订单查看与取消、收货信息维护。
- **商家端**：店铺信息、店铺图片、商品上下架/库存、订单处理、出餐完成。
- **骑手端**：查看可接订单、抢单、更新配送状态。
- **AI 搜索**：顾客输入自然语言需求，后端调用 OpenAI，根据商家与菜品数据返回推荐商家和菜品组合。

## 技术栈

| 层 | 技术 |
|---|---|
| 前端 | Vite、React、TypeScript、Zustand、React Router、shadcn/ui、Tailwind CSS |
| 后端 | Scala 3、Cats Effect、http4s、Circe、JWT、HikariCP |
| 数据库 | PostgreSQL 16（Docker Compose 可一键启动） |
| AI | OpenAI Chat Completions API（后端代理调用） |

## 目录结构

```text
.
├── frontend/              # Web 前端
│   ├── src/api/           # 前端 APIMessage 封装
│   ├── src/objects/       # 前端契约对象
│   ├── src/pages/         # 登录、注册、顾客端、商家端、骑手端页面
│   ├── src/stores/        # Zustand 页面状态
│   └── src/components/    # 通用组件与 AI 搜索组件
├── backend/               # Scala 后端
│   ├── src/ai/            # AI API、对象与 OpenAI 客户端
│   ├── src/user/          # 用户、登录注册、顾客账户
│   ├── src/merchant/      # 商家、商品、目录、店铺图片
│   ├── src/order/         # 下单、订单查询、取消
│   ├── src/rider/         # 骑手、抢单、配送状态
│   └── src/shared/        # API 网关、鉴权、数据库、JSON codec
└── package.json           # 根目录联动启动脚本
```

## 快速启动

### 环境要求

- Node.js + npm
- JDK 17 或更高版本
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
4. 等待后端目录 API 可用；
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

## AI 配置

AI 搜索由后端代理调用 OpenAI。启动后端前设置：

```bash
export OPENAI_API_KEY=your-key-here
# 可选
export OPENAI_BASE_URL=https://api.openai.com/v1
export OPENAI_MODEL=gpt-4o-mini
```

未配置 `OPENAI_API_KEY` 时，普通业务功能可运行，但 AI 搜索会提示服务未配置。

## 演示账号

| 角色 | 账号 | 密码 |
|---|---|---|
| 顾客 | `customer_demo` | `123456` |
| 商家 | `merchant_demo` | `123456` |
| 骑手 | `rider_demo` | `123456` |

## 常用命令

| 命令 | 说明 |
|---|---|
| `npm run dev` | 根目录一键启动数据库、后端、前端 |
| `npm run dev:db` | 仅启动 PostgreSQL |
| `npm run dev:backend` | 启动数据库后运行后端 |
| `npm run dev:frontend` | 仅启动前端 |
| `cd backend && sbt compile` | 后端编译检查 |
| `cd frontend && npm run typecheck` | 前端类型检查 |
| `cd frontend && npm run lint` | 前端 lint |
| `cd frontend && npm run build` | 前端生产构建 |

## API 约定

后端统一暴露网关路由：

```text
POST /api/{apiName}
```

前端通过 `frontend/src/api/shared/sendAPI.ts` 发送 `APIMessage`，后端通过 `backend/src/shared/api/APIMessage.scala` 注册并分发。需要鉴权的 API 使用 `Authorization: Bearer <token>`，网关根据注册角色校验权限。

## 环境变量

### 后端

| 变量 | 默认值 | 说明 |
|---|---|---|
| `DB_HOST` | `127.0.0.1` | PostgreSQL 主机 |
| `DB_PORT` | `5432` | PostgreSQL 端口 |
| `DB_NAME` | `delivery_backend` | 数据库名 |
| `DB_USER` | `postgres` | 数据库用户 |
| `DB_PASSWORD` | `postgres` | 数据库密码 |
| `JWT_SECRET` | 开发默认值 | JWT 签名密钥，生产环境必须覆盖 |
| `OPENAI_API_KEY` | 无 | AI 搜索所需 OpenAI Key |
| `OPENAI_BASE_URL` | `https://api.openai.com/v1` | OpenAI 兼容 API 地址 |
| `OPENAI_MODEL` | `gpt-4o-mini` | AI 搜索使用模型 |

### 前端

| 变量 | 默认值 | 说明 |
|---|---|---|
| `VITE_BACKEND_URL` | `http://localhost:8787` | Vite dev proxy 后端目标 |
| `VITE_API_BASE` | `/api` | 前端 API base path |

## 开发约定

- 前端只通过 HTTP API 获取和提交业务数据，不把浏览器本地状态作为真实数据源。
- 同一业务能力尽量保持前后端 `api/` 与 `objects/` 文件一一对应。
- 后端 Scala 代码使用不可变绑定 `val`，避免新增 `var`。
- 新增 AI 能力优先复用 `backend/src/ai/utils/OpenAIClient.scala`，并在 `backend/src/ai/` 与 `frontend/src/api/ai/` 下扩展同名契约。

更多前端与后端说明见：

- [`frontend/README.md`](frontend/README.md)
- [`backend/README.md`](backend/README.md)
