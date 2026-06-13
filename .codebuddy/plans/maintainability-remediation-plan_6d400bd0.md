---
name: maintainability-remediation-plan
overview: 更新整改计划：将 `shared` 从泛化目录改为直接拆分到语义清晰的新顶层目录，例如 `platform`、`auth`、`db`、`media`、`bootstrap`、`domain`/业务模块，并以兼容迁移方式逐步消除 `backend/src/shared`。
todos:
  - id: classify-shared
    content: 使用 [subagent:code-explorer] 分类 shared 文件并确定新目录
    status: completed
  - id: split-shared-foundation
    content: 使用 [skill:layered-feature-development] 拆分 shared 基础设施目录
    status: completed
    dependencies:
      - classify-shared
  - id: migrate-media-promotion
    content: 迁移 media、Promotion、Voucher 到明确归属
    status: completed
    dependencies:
      - split-shared-foundation
  - id: split-api-support-codecs
    content: 拆分 APIMessageSupport 和模块 JSON codec
    status: completed
    dependencies:
      - migrate-media-promotion
  - id: relocate-frontend-stores
    content: 迁移页面私有 Zustand store 到页面目录
    status: completed
    dependencies:
      - classify-shared
  - id: update-audit-docs
    content: 使用 [skill:maintainability-audit] 更新审计脚本和文档
    status: completed
    dependencies:
      - split-api-support-codecs
      - relocate-frontend-stores
  - id: final-verification
    content: 使用 [skill:type-safety-audit] 完成编译和审计验证
    status: completed
    dependencies:
      - update-audit-docs
---

## User Requirements

用户希望在既有可维护性整改计划基础上进一步调整：`shared` 目录整理后不再作为长期保留的泛化目录，而是直接拆分为语义更清晰的目录。整改应基于已发现的问题推进，并保持现有业务功能、接口路径、图片访问路径、页面交互和数据来源兼容。

## Product Overview

本次整改聚焦工程结构重整，不改变外卖平台现有顾客、商家、骑手、管理员等业务功能，也不进行页面视觉改版。整改后的项目目录应更容易判断代码归属，减少“无法归类就放 shared”的情况，并让后续新增代码持续遵守清晰分层。

## Core Features

- 将后端 `shared` 拆分为职责明确的目录，如平台基础设施、认证、数据库、启动数据、跨域领域类型、媒体资源等。
- 将 `Promotion`、`Voucher` 等业务语义对象迁移到明确业务归属目录，避免继续留在泛化共享目录。
- 将图片存储与读取能力迁移到独立媒体模块，同时保持原公开图片访问路径不变。
- 拆分后端 `APIMessageSupport`，将业务编排、校验和工具函数迁移到对应分层。
- 将页面私有 Zustand store 迁移到对应页面目录，提升前端页面内聚性。
- 拆分集中化 JSON codec，使序列化配置按模块可发现，并保留统一聚合入口。
- 更新审计脚本和文档，禁止新增实现代码继续落入 `shared`。

## Tech Stack Selection

- 前端沿用当前项目：Vite、React、TypeScript、React Router、Zustand、Tailwind CSS、shadcn/ui、Radix UI。
- 后端沿用当前项目：Scala 3、Cats Effect、http4s、Circe、JWT、HikariCP、PostgreSQL JDBC。
- 数据库保持 PostgreSQL 现有表结构、表名和迁移方式，避免因目录调整影响持久化数据。
- 审计与分层工作继续复用项目级 Skills：`maintainability-audit`、`layered-feature-development`、`type-safety-audit`。

## Implementation Approach

采用“先分类 shared，再迁移基础设施和业务归属，最后清理兼容入口”的渐进整改方式。第一阶段逐个确认 `backend/src/shared/*` 文件职责，并分配到新语义目录；第二阶段批量更新 Scala package 与 import；第三阶段拆分 API 支撑文件、模块化 JSON codec、迁移前端页面 store；最终让审计脚本把新增 `backend/src/shared` 实现视为违规。

关键决策：

1. **shared 不再作为长期目标目录**

- 不再把 `shared/api`、`shared/auth`、`shared/db` 等视为正常终态。
- 目标是拆为 `platform`、`auth`、`db`、`bootstrap`、`domain`、`media`，以及明确业务模块。

2. **行为兼容优先**

- 不改变 `POST /api/{apiName}` 统一网关。
- 不改变图片公开路径：
    - `/api/merchant/store-images/{fileName}`
    - `/api/merchant/product-images/{fileName}`
    - `/api/orders/refund-images/{fileName}`
    - `/api/reviews/images/{fileName}`
- 不改变数据库表 `stored_images`、业务表名、API 名称和前端响应结构。

3. **业务对象回归业务归属**

- `ErrorBody`、`HealthOk` 迁移到平台 HTTP 对象目录。
- `ids.scala`、角色和稳定跨模块枚举迁移到 `domain`。
- `Promotion` 和促销计算/校验工具优先评估迁移到独立 `promotion` 领域；若实际只服务商家目录和平台活动，则按使用关系拆入 `merchant` 或 `admin`。
- `Voucher` 和券相关工具优先评估迁移到 `user` 优惠券领域；若与促销结算强耦合，则纳入 `promotion` 领域。

4. **兼容包装只作为短期过渡**

- 如果 import 改动范围过大，可短期保留 `backend/src/shared` 下薄 re-export 或兼容包装。
- 最终审计应标记任何 `shared` 实现文件为债务，禁止新增业务或基础设施实现继续落入 `shared`。

## Implementation Notes

- 迁移 Scala package 时必须全局更新 import，重点关注 `DeliveryRoutes.scala`、`Main.scala`、`DeliveryStateStore.scala`、所有 APIMessage、routes、tables、json codec。
- `StoredImageRoutes` 迁移到 `media/routes` 后，`DeliveryRoutes.scala` 只切换 import，不改变路由挂载路径。
- `StoredImageTable`、`StoredImageMigration`、`StoredImageTableInitializer` 迁移到 `media/tables`，SQL 与表名保持不变。
- `APIMessageSupport` 拆分时优先保持函数签名和返回类型稳定，减少调用方行为变化。
- JSON codec 拆分为模块 codec 后，保留一个统一聚合入口，例如 `platform/json/ApiJsonCodecs.scala`，避免调用方分散 import。
- 前端 store 迁移先就近放到 `pages/{Page}/stores/`，再更新页面和组件 import；旧 `frontend/src/stores/pages` 可短期保留 re-export，最终删除。
- 每一阶段后运行后端编译、前端类型检查、类型安全审计和可维护性审计，确保结构迁移不破坏契约。

## Architecture Design

整改后的后端顶层目录意图：

```text
backend/src/
  platform/     # APIMessage、HTTP 通用对象、JSON 聚合、内部互操作基础设施
  auth/         # JWT、认证鉴权基础能力
  db/           # 数据库连接、事务、状态初始化入口
  bootstrap/    # 种子数据与启动导入
  domain/       # 跨模块 ID、角色、稳定枚举、通用响应类型
  media/        # 跨业务图片存储、读取、校验和迁移
  promotion/    # 若确认促销跨 admin/merchant/order，则承载促销领域
  admin/
  ai/
  merchant/
  order/
  review/
  rider/
  user/
```

运行链路保持：

```text
frontend/src/pages/{Page}/stores
  -> frontend/src/apis/{module}/XxxAPI.ts
  -> POST /api/{apiName}
  -> backend/src/{module}/api/XxxAPIMessage.scala
  -> validators/services
  -> tables
  -> PostgreSQL
```

图片读取链路调整为：

```text
GET /api/.../images/{fileName}
  -> backend/src/DeliveryRoutes.scala
  -> backend/src/media/routes/StoredImageRoutes.scala
  -> backend/src/media/tables/StoredImageTable.scala
  -> stored_images
```

JSON codec 目标链路：

```text
backend/src/{module}/json/{Module}JsonCodecs.scala
backend/src/platform/json/ApiJsonCodecs.scala  # 统一聚合入口
```

## Directory Structure

```text
Type-safe_project/
├── backend/src/shared/
│   # [DELETE or TEMP COMPAT] 不再作为长期实现目录；仅允许短期兼容包装，最终审计应提示清理。
├── backend/src/platform/
│   # [NEW] 平台基础设施目录，承接原 shared 中无业务归属的 API、HTTP、JSON 聚合和互操作能力。
│   ├── api/
│   │   # [MOVE] 从 shared/api 迁移 APIMessage、TaskIO、sendAPI、apiNameOf 等统一网关基础设施。
│   ├── http/
│   │   # [MOVE] 从 shared/http 迁移 AuthHttp；承接 HTTP 通用辅助能力。
│   ├── http/objects/
│   │   # [NEW/MOVE] 放置 ErrorBody、HealthOk 等 HTTP/健康检查通用 DTO。
│   ├── interop/
│   │   # [MOVE] 从 shared/interop 迁移 InternalToken。
│   └── json/
│       # [NEW/MOVE] 统一聚合各模块 JsonCodecs，替代 shared/json 作为对外导入入口。
├── backend/src/auth/
│   # [NEW/MOVE] 从 shared/auth 迁移 JwtSupport，承接认证鉴权基础能力。
├── backend/src/db/
│   # [NEW/MOVE] 从 shared/db 迁移 DatabaseConfig、DatabasePool、DatabaseSession、DeliveryStateStore。
├── backend/src/bootstrap/
│   # [NEW/MOVE] 从 shared/bootstrap 迁移 SeedBootstrap、SeedData。
├── backend/src/domain/
│   # [NEW/MOVE] 从 shared/objects 迁移 ids.scala、跨模块角色和稳定枚举。
│   └── apiTypes/
│       # [NEW/MOVE] 放置 OkResponse 等真正跨模块通用响应类型。
├── backend/src/media/
│   # [NEW] 独立媒体模块，承接 StoredImage 相关路由、表、服务和校验。
│   ├── routes/StoredImageRoutes.scala
│   │   # [MOVE] 保持图片读取行为不变，仅调整 package 和 import。
│   ├── objects/StoredImage.scala
│   │   # [NEW/MOVE] 从 StoredImageTable.scala 拆出 StoredImage 对象。
│   ├── tables/StoredImageTable.scala
│   │   # [MOVE] 保持 stored_images SQL 与读写逻辑不变。
│   ├── tables/StoredImageMigration.scala
│   │   # [MOVE] 保持历史图片导入逻辑不变。
│   ├── tables/StoredImageTableInitializer.scala
│   │   # [MOVE] 保持表初始化逻辑不变。
│   ├── services/StoredImageService.scala
│   │   # [NEW] 统一封装图片保存、scope、publicPath、contentType 处理。
│   └── validators/ImageUploadValidator.scala
│       # [NEW] 统一校验图片扩展名、文件名、大小和允许类型。
├── backend/src/promotion/
│   # [NEW if needed] 若 Promotion 跨 admin/merchant/order 使用明显，则作为促销领域模块。
│   ├── objects/Promotion.scala
│   │   # [MOVE/EVALUATE] 从 shared/objects 迁移 Promotion。
│   ├── utils/
│   │   # [MOVE/EVALUATE] 从 shared/utils 迁移 PromotionPricing、PromotionUsage、PromotionValidation。
│   └── json/PromotionJsonCodecs.scala
│       # [NEW if needed] 促销领域 codec。
├── backend/src/user/objects/Voucher.scala
│   # [MOVE/EVALUATE] 若 Voucher 主要属于顾客优惠券，则从 shared/objects 迁移到 user。
├── backend/src/user/utils/
│   # [MOVE/EVALUATE] 迁移 VoucherSupport 或拆到 user services/validators。
├── backend/src/DeliveryRoutes.scala
│   # [MODIFY] 切换 platform/api 与 media/routes imports，保留公开 HTTP 路径。
├── backend/src/Main.scala
│   # [MODIFY] 若引用 shared db/auth/platform 能力，切换到新 package。
├── backend/src/{admin,ai,merchant,order,review,rider,user}/api/
│   # [MODIFY] 更新 APIMessage、鉴权、JSON、ID、DTO imports；拆除 APIMessageSupport 依赖。
├── backend/src/{admin,ai,merchant,order,review,rider,user}/services/
│   # [NEW/MODIFY] 承接业务流程和状态流转。
├── backend/src/{admin,ai,merchant,order,review,rider,user}/validators/
│   # [NEW/MODIFY] 承接输入、权限和状态校验。
├── backend/src/{admin,ai,merchant,order,review,rider,user}/json/
│   # [NEW] 各模块 codec，最终由 platform/json/ApiJsonCodecs 聚合。
├── frontend/src/pages/{Login,Register,RiderApp,CustomerPortal,MerchantConsole}/stores/
│   # [NEW/MOVE] 页面私有 Zustand store 迁移到页面目录。
├── frontend/src/stores/pages/
│   # [MODIFY/DELETE] 短期 re-export，最终删除页面私有实现。
├── DIRECTORY_LAYERING_GUIDE.md
│   # [MODIFY] 删除 shared 作为正常目标目录的描述，新增 platform/domain/auth/db/bootstrap/media 边界。
├── AGENTS.md
│   # [MODIFY] 明确禁止新增 backend/src/shared 实现代码。
├── README.md
│   # [MODIFY] 更新项目结构摘要。
├── README.full.md
│   # [MODIFY] 更新完整架构、目录职责和验证流程。
├── API_INVENTORY.md
│   # [MODIFY] 如 API 基础设施或静态资源实现路径说明变化，同步更新。
└── .codebuddy/skills/maintainability-audit/scripts/check-maintainability.sh
    # [MODIFY] 新增 shared 拆分后的目录检查和违规新增检查。
```

## Key Code Structures

计划阶段不固定具体函数签名。实施时应保持外部 APIMessage 契约、Zustand store 导出名称、数据库表结构和图片 URL 格式稳定，仅调整 package、import、目录归属和内部调用关系。

## Agent Extensions

### Skill

- **maintainability-audit**
- Purpose: 审计整改前后的结构风险，特别是 `shared` 残留、APIMessageSupport、媒体归属、前端 store 内聚性。
- Expected outcome: 阶段性输出 warning 收敛情况，最终阻止新增实现代码落入 `backend/src/shared`。

- **layered-feature-development**
- Purpose: 指导 `platform`、`auth`、`db`、`bootstrap`、`domain`、`media`、业务模块的目录落位。
- Expected outcome: 每个迁移文件都有明确职责归属，不再依赖泛化 shared 目录。

- **type-safety-audit**
- Purpose: 验证 APIMessage、objects、apiTypes、前后端契约和页面调用路径没有因迁移漂移。
- Expected outcome: shared 拆分和目录迁移后仍保持统一网关与类型安全。

### SubAgent

- **code-explorer**
- Purpose: 在实施前追踪所有 `delivery.shared.*` import、StoredImage 调用点、codec 使用点和前端 store 引用。
- Expected outcome: 找全迁移影响范围，避免遗漏包路径和运行时依赖。