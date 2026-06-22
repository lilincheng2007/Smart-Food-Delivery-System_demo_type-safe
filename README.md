# Type-safe Delivery Platform 项目总结

> 类型安全外卖平台全栈项目。补充说明文档见 [`README.full.md`](README.full.md)。

## 1. 项目概述

本项目实现了覆盖顾客、商家、骑手、管理员四类角色的外卖业务系统，并集成多项 AI 辅助能力。项目采用 Web 前端 + Scala 后端 + PostgreSQL 架构，所有业务交互统一通过类型化 `APIMessage` 网关完成。

核心目标：

- 前后端 API 契约一一对应，降低接口漂移风险。
- 订单、钱包、库存、优惠、通知、评价、退款等真实业务数据以后端数据库为准。
- 下单、接单、配送、退款、仲裁等关键状态由后端集中流转。
- 顾客、商家、骑手、管理员形成完整外卖业务闭环。
- AI 能力嵌入真实业务页面，而不是孤立 Demo。

## 2. 小组分工情况

项目由 **李** 与 **张** 两名成员共同完成，李担任组长。两人均参与前后端开发，按功能模块闭环负责页面、后端逻辑、数据落库与类型契约，避免前后端理解割裂。

### 2.1 架构与基础设施

| 方向 | 负责人 | 主要内容 |
|---|---|---|
| 数据库与核心契约 | **李** | 数据库整体设计、PostgreSQL 实现、前后端类型契约定义与校验 |
| API 网关与类型安全优化 | **张** | 类型化 `APIMessage` 网关、统一通信格式、降低接口漂移风险 |
| API 设计与工程化 | **李、张** | API 方案讨论、类型安全维护、脚手架、构建与部署等按需协作 |

### 2.2 业务功能分工

#### 顾客端

| 负责人 | 模块 |
|---|---|
| **李** | 注册登录、购物车、收货地址、结算、钱包充值与余额校验、吃货积分与等级 |
| **张** | 商家浏览、店内点餐、订单操作与状态时间线、退款申请与凭证、评价与图片评价、订单聊天、全局通知 |

#### 商家端

| 负责人 | 模块 |
|---|---|
| **李** | 多店铺管理、入驻申请、店铺资料维护 |
| **张** | 营业时间、商品/套餐/规格、上下架与库存、优惠券、接单/拒单/备餐/出餐、退款处理、评价回复、订单聊天 |

#### 骑手端

| 负责人 | 模块 |
|---|---|
| **李** | 服务能量值、超时统计、免责卡兑换与使用 |
| **张** | 抢单、配送状态、配送完成、配送结算与薪资、骑手评分与评价、同骑手最多同时配送 5 单约束 |

#### 管理员端

| 负责人 | 模块 |
|---|---|
| **李** | 订单监控面板、通知已读持久化 |
| **张** | 平台优惠管理、管理员全局通知中心、店铺入驻审核、退款仲裁 |

### 2.3 AI 能力分工

全部 AI 能力由 **李** 独立完成，包括 AI 智能搜索、饮食周报、订单进度叙事、评价摘要、商家文案和经营建议；相关前端交互、后端调用、Prompt 设计与结果处理均嵌入真实业务流程。

### 2.4 协作方式

两人以模块为单位并行推进，跨角色流程依托统一 `APIMessage` 网关和类型契约完成端到端串联；API 设计、类型安全维护和代码审查保持共同讨论，复杂问题按需互相支援。

## 3. 类型与系统架构

### 3.1 技术栈

| 层级 | 技术 |
|---|---|
| 前端 | Vite、React、TypeScript、React Router、Zustand、Tailwind CSS、shadcn/ui、Radix UI |
| 后端 | Scala 3、Cats Effect、http4s、Circe、JWT、HikariCP、PostgreSQL JDBC |
| 数据库 | PostgreSQL 16，Docker Compose 本地启动 |
| AI | OpenAI Chat Completions 兼容接口，后端统一代理调用 |

### 3.2 目录结构

```text
Type-safe_project/
├── README.md                 # 当前项目总览
├── README.full.md            # 完整说明文档
├── API_INVENTORY.md          # API 盘点
├── AGENTS.md                 # 项目规则与研发约定
├── sample/                   # 结构参照样例，不是主应用
├── .codebuddy/skills/        # 项目辅助技能，例如 type-safety-audit
├── frontend/                 # Vite + React Web 前端
│   └── src/
│       ├── apis/             # 前端 APIMessage，一 API 一文件
│       ├── objects/          # TypeScript 领域对象与 API 类型
│       ├── pages/            # 顾客、商家、骑手、管理员等页面
│       ├── stores/           # 全局 Zustand 状态；页面私有 store 就近放入 pages/{Page}/stores
│       ├── components/       # 通用组件与 UI
│       └── lib/              # 会话、计价、库存、时间线等工具
└── backend/                  # Scala 3 + http4s 后端
    └── src/
        ├── Main.scala        # 服务入口，监听 8787
        ├── DeliveryRoutes.scala
        ├── user/             # 用户、顾客资料、钱包、优惠券
        ├── merchant/         # 商家、店铺、商品、营业、订单处理
        ├── order/            # 下单、订单、退款、聊天、通知
        ├── rider/            # 骑手、抢单、配送、结算
        ├── admin/            # 入驻审核、退款仲裁、平台配置
        ├── review/           # 评价、回复、投票
        ├── ai/               # AI 搜索、周报、文案与建议
        ├── platform/         # APIMessage、HTTP、JSON 聚合等平台基础设施
        ├── auth/             # JWT 与认证鉴权
        ├── db/               # 数据库连接、事务与初始化入口
        ├── bootstrap/        # 种子数据与启动导入
        ├── domain/           # 跨模块 ID、角色、稳定枚举
        ├── media/            # 图片存储、读取、校验和迁移
        └── promotion/        # 促销、优惠券和结算辅助领域
```

### 3.3 APIMessage 类型安全约定

项目统一采用 `POST /api/{apiName}` 网关，不在前端散落硬编码业务路由。

- 后端 API：`backend/src/{module}/api/XxxAPIMessage.scala`
- 前端 API：`frontend/src/apis/{module}/XxxAPI.ts`
- 领域对象：`objects/{module}/`
- 请求/响应类型：`objects/{module}/apiTypes/`

| 能力 | 前端 API | 后端 APIMessage | apiName |
|---|---|---|---|
| 顾客结算 | `CheckoutAPI.ts` | `CheckoutAPIMessage.scala` | `checkoutapi` |
| 商家接单 | `MerchantOrderAcceptAPI.ts` | `MerchantOrderAcceptAPIMessage.scala` | `merchantorderacceptapi` |
| 骑手抢单 | `RiderGrabOrderAPI.ts` | `RiderGrabOrderAPIMessage.scala` | `ridergraborderapi` |
| AI 饮食周报 | `AIDietWeeklyReportAPI.ts` | `AIDietWeeklyReportAPIMessage.scala` | `aidietweeklyreportapi` |

后端通过 `APIMessage` / `APIWithRoleMessage` 描述接口执行计划，并由网关完成 JSON 解码、JWT 鉴权、角色校验与数据库上下文执行。前端统一通过 `sendAPI` 发送类型化消息。

### 3.4 数据与状态原则

- 前端仅保存 JWT、角色、临时 UI 状态。
- 钱包余额、订单状态、库存扣减、优惠使用、通知已读、聊天记录、评价与退款均由后端数据库保存。
- 订单状态由后端统一流转，并追加订单时间线与系统通知。
- 下单时保存价格快照和结构化价格明细，历史订单可追溯。

## 4. 已实现的主要功能

### 4.1 顾客端

- 注册、登录、JWT 会话与角色路由保护。
- 商家目录浏览、店内点餐、普通菜品与套餐选择。
- 购物车、再来一单、库存、售罄、每单限购校验。
- 收货联系人管理，支持多组联系人且必须有唯一默认地址。
- 结算页支持商家优惠、平台优惠、优惠券、配送信息和价格明细。
- 钱包充值、余额校验、订单支付、取消订单、完成确认。
- 订单详情、状态时间线、预计出餐和送达、订单卡片滚动展示。
- 退款申请、图片凭证上传、商家驳回后可提交管理员仲裁。
- 评价、图片评价、商家回复、评价投票。
- 订单聊天、图片消息、未读数、全局通知中心。
- 吃货积分与等级体系，升级自动发放平台优惠券。
- AI 搜索、AI 饮食周报、AI 订单进度叙事、AI 评价摘要。

### 4.2 商家端

- 多店铺管理与店铺入驻申请。
- 店铺资料维护：标签、公告、描述、头图、营业状态。
- 每周营业时间、节假日营业时间、暂停接单。
- 商品创建与编辑：普通菜品、套餐、套餐类别和选项、图片上传。
- 商品上下架、库存模式、今日库存、售罄、每单限购。
- 店铺优惠与单菜品优惠，支持启停、排期与校验。
- 订单接单、拒单、设置备餐时间、主动延迟备餐、出餐完成。
- 退款申请处理：同意退款、驳回退款并给出理由。
- 顾客评价查看与商家回复。
- 订单聊天、未读提醒。
- AI 店铺描述、AI 菜品描述、AI 经营建议。

### 4.3 骑手端

- 查看可抢订单、抢单与配送中任务列表。
- 更新配送状态并完成配送。
- 配送结算、薪资累计、服务能量值与超时统计。
- 100 能量兑换超时免责卡，超时配送可自动或手动使用。
- 同一骑手最多同时配送 5 单。
- 订单聊天、未读提醒、骑手评分与评价列表。

### 4.4 管理员端

- 店铺入驻审核。
- 顾客/商家退款仲裁。
- 订单监控面板：今日订单数、成交额、异常订单、待处理退款、商家/骑手超时。
- 平台优惠管理：新增、编辑、启停、排期校验。
- 管理员账号接入全局通知中心，通知已读状态通过后端 API 持久化。

### 4.5 AI 能力

- AI 智能搜索：根据自然语言需求推荐商家和商品。
- AI 饮食周报：基于近 7 天订单输出顾客饮食分析、营养估算、饮食趋势与可执行建议。
- AI 订单进度叙事：为不同状态订单生成更自然的进度说明。
- AI 评价摘要：聚合商家评价，辅助顾客快速理解口碑。
- AI 商家文案：生成店铺描述和菜品描述。
- AI 经营建议：结合商家订单和经营数据生成优化建议。

## 5. 特点与亮点

### 5.1 前后端契约清晰

项目以 `APIMessage` 为核心统一 API 形态，前后端 API 文件、领域对象、请求/响应类型分层明确，便于维护和审计。

### 5.2 业务闭环完整

系统覆盖入驻、上架、下单、支付、接单、备餐、配送、完成、评价、退款、仲裁、通知、聊天和经营分析，不只是简单点餐 Demo。

### 5.3 后端作为可信业务源

库存扣减、钱包余额、订单状态、退款状态、优惠计算和通知已读等关键逻辑均由后端负责，避免前端伪造持久化业务状态。

### 5.4 订单价格可追溯

订单保存价格快照与结构化 `OrderPriceBreakdown`，优惠券、商家优惠、平台优惠、配送费、实付金额等均可在历史订单中还原。

### 5.5 多角色协同一致

顾客、商家、骑手和管理员看到的是同一后端事实的不同视图。退款、配送、评价、聊天等跨角色动作会互相联动。

### 5.6 AI 融入真实业务

AI 功能嵌入搜索、订单进度、饮食分析、商家经营和商品文案等实际流程，为用户决策和商家运营提供辅助。

### 5.7 本地开发友好

根目录提供一键脚本启动数据库、后端和前端；PostgreSQL 通过 Docker Compose 准备，后端启动时自动初始化表结构和演示数据。

## 6. 快速启动

### 环境要求

- Node.js + npm
- JDK 21 推荐，JDK 17+ 可用
- sbt
- Docker

### 一键启动

```bash
npm install
npm run dev
```

该命令会启动 PostgreSQL、后端 `http://localhost:8787`，并在后端可用后启动前端 Vite 服务。

### 演示账号

| 角色 | 账号 | 密码 |
|---|---|---|
| 顾客 | `customer_demo` | `123456` |
| 商家 | `merchant_demo` | `123456` |
| 骑手 | `rider_demo` | `123456` |
| 管理员 | `admin` | `123456` |

### 常用检查

```bash
npm run typecheck --prefix frontend
cd backend && sbt -batch compile
.codebuddy/skills/type-safety-audit/scripts/check-type-safety.sh /Users/leonli/Desktop/Type-safe_project
.codebuddy/skills/maintainability-audit/scripts/check-maintainability.sh /Users/leonli/Desktop/Type-safe_project
```

## 7. 环境变量

| 变量 | 默认值 | 说明 |
|---|---|---|
| `DB_HOST` | `127.0.0.1` | PostgreSQL 主机 |
| `DB_PORT` | `5432` | PostgreSQL 端口 |
| `DB_NAME` | `delivery_backend` | 数据库名 |
| `DB_USER` | `postgres` | 数据库用户 |
| `DB_PASSWORD` | `postgres` | 数据库密码 |
| `DB_MAX_POOL_SIZE` | `10` | Hikari 连接池大小 |
| `DB_CONNECTION_TIMEOUT_MS` | `3000` | 数据库连接超时 |
| `DB_POOL_NAME` | `delivery-backend-pool` | Hikari 连接池名称 |
| `JWT_SECRET` | 开发默认值 | JWT 签名密钥，生产环境需覆盖 |
| `SERVICE_INTERNAL_TOKEN` | `dev-internal-token` | 内部服务调用令牌 |
| `OPENAI_API_KEY` | 无 | AI 功能所需 Key |
| `OPENAI_BASE_URL` | `https://api.openai.com/v1` | OpenAI 兼容接口地址 |
| `OPENAI_MODEL` | `gpt-4o-mini` | AI 模型 |
| `VITE_BACKEND_URL` | `http://localhost:8787` | 前端开发代理后端地址 |
| `VITE_API_BASE` | `/api` | 前端 API base path |

未配置 `OPENAI_API_KEY` 时，普通业务仍可运行；AI 相关能力会提示未配置或返回兜底内容。

## 8. 参考文档

- 补充说明：[`README.full.md`](README.full.md)
- 前端说明：[`frontend/README.md`](frontend/README.md)
- 后端说明：[`backend/README.md`](backend/README.md)
- API 盘点：[`API_INVENTORY.md`](API_INVENTORY.md)
- 项目规则：[`AGENTS.md`](AGENTS.md)
