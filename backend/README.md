# Delivery Backend

`backend/` 是外卖平台的 Scala 后端服务。服务以单进程方式启动，监听 `8787` 端口，对外提供统一 `APIMessage` 网关，并连接 PostgreSQL 持久化业务数据。

## 技术栈

- Scala 3.3.3
- Cats Effect
- http4s Ember Server / Client
- Circe JSON
- HikariCP + PostgreSQL JDBC
- JWT 鉴权
- OpenAI Chat Completions API（AI 搜索）

## 目录结构

```text
backend/
├── build.sbt
├── docker-compose.yml
├── docker/
│   └── init-databases.sql
└── src/
    ├── Main.scala                 # 服务入口，监听 0.0.0.0:8787
    ├── DeliveryRoutes.scala       # 路由汇总
    ├── ai/                        # AI 搜索模块
    ├── user/                      # 用户、注册、登录、顾客资料
    ├── merchant/                  # 商家、商品、目录、图片
    ├── order/                     # 下单、订单查询、取消
    ├── rider/                     # 骑手、抢单、配送状态
    └── shared/                    # API 网关、JWT、数据库、JSON codec、种子数据
```

每个业务模块通常包含：

```text
api/       # APIMessage 定义
objects/   # 请求/响应/领域对象
tables/    # 表初始化与数据库访问
routes/    # API 注册或额外公开路由
utils/     # 模块内工具
```

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

数据库默认账号密码均为 `postgres`。初始化脚本会创建 `delivery_backend` 等数据库，其中后端默认连接 `delivery_backend`。

### 2. 启动后端

```bash
cd backend
sbt run
```

启动成功后监听：

```text
http://localhost:8787
```

## 编译检查

```bash
cd backend
sbt compile
```

## API 网关

后端对外主要使用统一网关：

```text
POST /api/{apiName}
```

关键实现：

- `src/shared/api/APIMessage.scala`：`APIMessage`、`APIWithRoleMessage`、`RegisteredAPIMessage`、`APIMessageRouter`
- `src/DeliveryRoutes.scala`：汇总 `UserRoutes`、`MerchantRoutes`、`OrderRoutes`、`RiderRoutes`、`AIRoutes`
- `src/shared/json/ApiJsonCodecs.scala`：Circe Codec 注册

`apiName` 由后端 `*APIMessage` 类名推导，例如 `AISearchAPIMessage` 对应：

```text
POST /api/aisearchapi
```

需要登录的 API 使用 `APIWithRoleMessage` 注册，并通过 `Authorization: Bearer <token>` 校验角色。

## 公开静态路由

除 API 网关外，商家店铺图片通过公开路由访问：

```text
/api/merchant/store-images/...
```

## 数据库配置

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

- `src/shared/db/DatabaseConfig.scala`
- `src/shared/db/DatabasePool.scala`
- `src/shared/db/DeliveryStateStore.scala`

## 鉴权配置

JWT 实现在 `src/shared/auth/JwtSupport.scala`。开发环境会使用默认密钥，生产或多人协作环境建议显式配置：

```bash
export JWT_SECRET=replace-with-a-strong-secret
```

## AI 搜索配置

AI 搜索模块位于 `src/ai/`：

```text
ai/
├── api/AIAPIMessages.scala        # AISearchAPIMessage
├── objects/AISearchRequest.scala
├── objects/AISearchResponse.scala
├── routes/AIRoutes.scala
└── utils/OpenAIClient.scala       # OpenAI 调用、30s 超时、最多 3 次重试
```

启动后端前配置：

```bash
export OPENAI_API_KEY=your-key-here
# 可选
export OPENAI_BASE_URL=https://api.openai.com/v1
export OPENAI_MODEL=gpt-4o-mini
```

AI 搜索流程：

1. 顾客端调用 `POST /api/aisearchapi`，请求体包含 `query`。
2. 后端复用目录数据，构造商家与菜品上下文。
3. `OpenAIClient` 拼接系统提示词与用户输入，调用 OpenAI。
4. 后端解析 JSON，校验 `merchantId`、`productId` 来自现有数据后返回。

## 演示数据

种子数据位于 `src/shared/bootstrap/SeedData.scala`，包含默认顾客、商家、菜品和骑手。

演示账号：

| 角色 | 账号 | 密码 |
|---|---|---|
| 顾客 | `customer_demo` | `123456` |
| 商家 | `merchant_demo` | `123456` |
| 骑手 | `rider_demo` | `123456` |

## 新增后端 API 的推荐步骤

1. 在对应模块 `objects/` 新增请求/响应或领域对象。
2. 在对应模块 `api/` 新增 `*APIMessage`。
3. 在 `routes/` 中使用 `api` 或 `apiWithRole` 注册。
4. 在 `src/shared/json/ApiJsonCodecs.scala` 注册 Codec。
5. 同步前端 `frontend/src/api/*` 与 `frontend/src/objects/*` 的同名契约。
6. 运行 `sbt compile` 验证。

## 代码约定

- 后端 Scala 新代码使用 `val`，避免新增 `var`。
- 业务 API 优先走 `APIMessage` 网关，不新增零散字符串路由；静态资源等特殊场景除外。
- 需要鉴权的业务操作必须使用 `APIWithRoleMessage` 并声明角色。
- 业务数据以后端数据库为准，不依赖前端本地状态作为真实数据源。
