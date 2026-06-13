---
name: maintainability-remediation-plan
overview: 基于当前可维护性审计的 6 类 warning，制定渐进整改计划：先收敛媒体与 shared 边界，再拆 API 支撑文件、补齐 service/validator 分层，最后迁移前端页面 store 与模块化 JSON codec。
todos:
  - id: baseline-audit
    content: 使用 [skill:maintainability-audit] 和 [subagent:code-explorer] 建立整改基线
    status: pending
  - id: migrate-media-module
    content: 使用 [skill:layered-feature-development] 迁移 StoredImage 到 media 模块
    status: pending
    dependencies:
      - baseline-audit
  - id: split-backend-support
    content: 拆分 APIMessageSupport 到 services、validators、utils
    status: pending
    dependencies:
      - migrate-media-module
  - id: modularize-codecs-shared
    content: 拆分模块 JSON codec 并收敛 shared 业务对象
    status: pending
    dependencies:
      - split-backend-support
  - id: relocate-frontend-stores
    content: 迁移页面私有 Zustand store 到页面目录
    status: pending
    dependencies:
      - baseline-audit
  - id: update-audits-docs
    content: 更新审计脚本、项目文档和迁移说明
    status: pending
    dependencies:
      - modularize-codecs-shared
      - relocate-frontend-stores
  - id: final-verification
    content: 使用 [skill:type-safety-audit] 完成编译、类型和可维护性验证
    status: pending
    dependencies:
      - update-audits-docs
---

## User Requirements

用户要求基于已发现的工程可维护性问题制定整改计划，目标是让当前外卖平台项目具备长期可维护结构。整改范围包括后端分层、共享目录边界、对象归属、序列化组织、媒体资源模块、前端页面状态内聚，以及审计规则持续生效。

## Product Overview

本次整改不改变现有业务功能和页面视觉效果，重点调整代码组织方式。现有顾客、商家、骑手、管理员等业务能力保持稳定，公开接口路径、图片访问路径、业务数据来源、页面交互效果均应保持兼容。

## Core Features

- 清理后端 API 层中过重和命名模糊的支撑文件，让 API 入口只负责请求编排。
- 建立或补齐后端 `services`、`validators`、`utils`、`media` 等职责清晰的分层。
- 收敛 `shared` 目录职责，避免业务对象、业务表、业务路由继续混入基础设施目录。
- 将图片存储和读取能力整理为独立媒体模块，同时保持原有公开图片路径不变。
- 将页面私有状态迁移到对应页面目录下，提升前端页面代码内聚性。
- 拆分集中化序列化配置，提升对象和接口契约的可发现性。
- 更新审计规则和文档，使后续新增代码能持续遵守可维护结构。

## Tech Stack Selection

- 前端继续沿用当前项目技术栈：Vite、React、TypeScript、React Router、Zustand、Tailwind CSS、shadcn/ui、Radix UI。
- 后端继续沿用当前项目技术栈：Scala 3、Cats Effect、http4s、Circe、JWT、HikariCP、PostgreSQL JDBC。
- 数据库结构保持 PostgreSQL 现有表与迁移方式，不改变已有业务表名、字段语义和数据来源。
- 审计工具继续使用项目级 Skills：
- `.codebuddy/skills/maintainability-audit/`
- `.codebuddy/skills/layered-feature-development/`
- `.codebuddy/skills/type-safety-audit/`

## Implementation Approach

本次整改采用“先低风险结构迁移，再逐步拆分复杂业务支撑”的方式推进。优先做不改变行为的包路径迁移、兼容 re-export、模块目录补齐和导入更新；随后将 `APIMessageSupport` 中的职责按实际内容拆到 `services`、`validators`、`utils` 或 `media`，最后更新审计脚本和文档，让历史问题不再回流。

关键决策：

1. **不改变业务行为**

- 不修改 `POST /api/{apiName}` APIMessage 网关。
- 不修改现有公开图片路径：
    - `/api/merchant/store-images/{fileName}`
    - `/api/merchant/product-images/{fileName}`
    - `/api/orders/refund-images/{fileName}`
    - `/api/reviews/images/{fileName}`
- 不修改数据库表名、API 名称、前端响应类型和页面展示逻辑。

2. **先建目标层，再迁移调用**

- 后端先补齐 `services/`、`validators/`、`utils/`、`media/` 目标路径。
- 前端先在 `pages/{Page}/stores/` 建立目标 store 路径，再让旧 `frontend/src/stores/pages/*` 暂时 re-export，降低一次性改动风险。

3. **兼容优先，逐步删除旧入口**

- 第一轮迁移允许保留兼容导出，确保页面和构建不受影响。
- 等所有导入切换到新路径并通过验证后，再删除旧路径或让审计脚本检查其不存在。

4. **审计规则同步进化**

- 整改不是一次性代码搬运，需要同步更新 `.codebuddy/skills/maintainability-audit/scripts/check-maintainability.sh`，让它能识别新的目标结构，避免后续新增代码重新落回旧目录。

## Implementation Notes

- 后端 package 移动时要同步更新所有 Scala import，特别注意 `DeliveryRoutes.scala`、`DeliveryStateStore.scala`、各图片上传 API、`ApiJsonCodecs.scala`。
- 移动 `StoredImage*` 时只改变实现包路径，不改变数据库表 `stored_images`、scope 值和 HTTP 路由。
- 拆分 `APIMessageSupport` 时优先保持函数签名和返回类型稳定，减少调用方改动。
- `ErrorBody.scala`、`HealthOk.scala` 属于 HTTP/健康检查通用 DTO，可保留在 shared；`Promotion.scala`、`Voucher.scala` 具有业务语义，应在整改中明确归属或建立受控共享领域目录。
- 前端 store 迁移优先使用兼容 re-export，避免一次性修改大量页面组件导致回归。
- JSON codec 拆分应以“模块 codec 文件 + shared 聚合导出”为目标，不在 API 文件里临时写 codec。
- 每个阶段至少运行：
- `cd backend && sbt -batch compile`
- `npm run typecheck --prefix frontend`
- `.codebuddy/skills/type-safety-audit/scripts/check-type-safety.sh /Users/leonli/Desktop/Type-safe_project`
- `.codebuddy/skills/maintainability-audit/scripts/check-maintainability.sh /Users/leonli/Desktop/Type-safe_project`

## Architecture Design

整改后的结构保持现有前后端分离架构，但强化层级边界：

```text
前端页面交互
  -> frontend/src/pages/{Page}/hooks|stores
  -> frontend/src/apis/{module}/XxxAPI.ts
  -> POST /api/{apiName}
  -> backend/src/{module}/api/XxxAPIMessage.scala
  -> validators/services
  -> tables
  -> PostgreSQL
```

媒体资源读取保持运行时路径不变，但实现归属调整为：

```text
GET /api/.../images/{fileName}
  -> backend/src/DeliveryRoutes.scala
  -> backend/src/media/routes/StoredImageRoutes.scala
  -> backend/src/media/tables/StoredImageTable.scala
  -> stored_images
```

JSON codec 目标结构：

```text
backend/src/{module}/json/{Module}JsonCodecs.scala
backend/src/shared/json/ApiJsonCodecs.scala  # 聚合并继续作为对外统一 import 入口
```

## Directory Structure

本次计划涉及以下路径。

```text
Type-safe_project/
├── DIRECTORY_LAYERING_GUIDE.md
│   # [MODIFY] 根据实际整改结果补充迁移状态、最终目录约定和后续新增代码规则。
├── AGENTS.md
│   # [MODIFY] 同步最终验证命令、可维护性规则和已迁移目录说明。
├── API_INVENTORY.md
│   # [MODIFY] 若 API 契约文件路径或公开静态资源实现说明变化，同步说明；API 名称不变。
├── README.md
│   # [MODIFY] 如项目总览包含目录结构或开发规范摘要，补充可维护性整改入口。
├── README.full.md
│   # [MODIFY] 同步完整架构说明、分层规则和验证流程。
├── backend/src/DeliveryRoutes.scala
│   # [MODIFY] 将 StoredImageRoutes import 从 shared 路径切换到 media 路径；保留原 HTTP 路由。
├── backend/src/media/
│   # [NEW] 独立媒体模块，承载跨业务图片存储、读取、校验和迁移能力。
│   ├── routes/StoredImageRoutes.scala
│   │   # [NEW] 从 shared/routes 迁移；负责公开图片读取路由的通用实现。
│   ├── tables/StoredImageTable.scala
│   │   # [NEW] 从 shared/tables/storedimage 迁移；保持 stored_images 表访问逻辑不变。
│   ├── tables/StoredImageMigration.scala
│   │   # [NEW] 从 shared/tables/storedimage 迁移；保持历史图片导入逻辑不变。
│   ├── tables/StoredImageTableInitializer.scala
│   │   # [NEW] 从 shared/tables/storedimage 迁移；保持表初始化 SQL 不变。
│   ├── services/StoredImageService.scala
│   │   # [NEW] 封装图片保存、publicPath 生成、scope 映射和 contentType 处理。
│   └── validators/ImageUploadValidator.scala
│       # [NEW] 统一校验图片文件名、扩展名、大小和允许类型。
├── backend/src/shared/routes/StoredImageRoutes.scala
│   # [DELETE or COMPAT] 迁移完成后删除，或短期保留兼容转发；最终不应作为实现源。
├── backend/src/shared/tables/storedimage/
│   # [DELETE or COMPAT] 迁移完成后删除，或短期保留兼容转发；最终不应作为实现源。
├── backend/src/shared/db/DeliveryStateStore.scala
│   # [MODIFY] StoredImage 初始化和导入逻辑改用 media 模块路径。
├── backend/src/shared/json/ApiJsonCodecs.scala
│   # [MODIFY] 逐步改为聚合各模块 codec，减少直接 import 全业务模块和 table 对象。
├── backend/src/admin/json/AdminJsonCodecs.scala
│   # [NEW] 管理端对象与 apiTypes 的 Circe codec。
├── backend/src/ai/json/AIJsonCodecs.scala
│   # [NEW] AI 对象与 apiTypes 的 Circe codec。
├── backend/src/merchant/json/MerchantJsonCodecs.scala
│   # [NEW] 商家对象与 apiTypes 的 Circe codec。
├── backend/src/order/json/OrderJsonCodecs.scala
│   # [NEW] 订单对象与 apiTypes 的 Circe codec。
├── backend/src/review/json/ReviewJsonCodecs.scala
│   # [NEW] 评价对象与 apiTypes 的 Circe codec。
├── backend/src/rider/json/RiderJsonCodecs.scala
│   # [NEW] 骑手对象与 apiTypes 的 Circe codec。
├── backend/src/user/json/UserJsonCodecs.scala
│   # [NEW] 用户对象与 apiTypes 的 Circe codec。
├── backend/src/shared/json/SharedJsonCodecs.scala
│   # [NEW] shared 基础 DTO、ID、通用响应对象 codec。
├── backend/src/{admin,ai,merchant,order,review,rider,user}/services/
│   # [NEW/MODIFY] 为各模块建立业务流程层；承接 APIMessageSupport 中的业务编排。
├── backend/src/{admin,ai,merchant,order,review,rider,user}/validators/
│   # [NEW/MODIFY] 为各模块建立输入、权限、状态校验层。
├── backend/src/{merchant,user,rider,review,order}/utils/
│   # [NEW/MODIFY] 承接模块内纯函数 helper，避免继续放在 api/Support 文件。
├── backend/src/merchant/api/MerchantAPIMessageSupport.scala
│   # [DELETE after split] 拆分到 merchant services/validators/utils/media。
├── backend/src/user/api/UserAPIMessageSupport.scala
│   # [DELETE after split] 拆分到 user services/validators/utils。
├── backend/src/rider/api/RiderAPIMessageSupport.scala
│   # [DELETE after split] 拆分到 rider services/validators。
├── backend/src/review/api/ReviewAPIMessageSupport.scala
│   # [DELETE after split] 拆分到 review services/validators/media。
├── backend/src/order/api/OrderAPIMessageSupport.scala
│   # [DELETE after split] 拆分到 order services/validators/utils。
├── backend/src/order/api/OrderChatAPIMessageSupport.scala
│   # [DELETE after split] 拆分到 order services/validators。
├── backend/src/order/api/OrderChatUnreadCountsAPIMessageSupport.scala
│   # [DELETE after split] 拆分到 order services。
├── backend/src/order/api/OrderImageFileAPIMessageSupport.scala
│   # [DELETE after split] 拆分到 media service 或 order validator。
├── backend/src/merchant/api/MerchantStoreImageFileAPIMessage.scala
│   # [MODIFY] 图片保存调用切换到 media service。
├── backend/src/merchant/api/MerchantProductImageFileAPIMessage.scala
│   # [MODIFY] 图片保存调用切换到 media service。
├── backend/src/review/api/CustomerReviewImageFileAPIMessage.scala
│   # [MODIFY] 图片保存调用切换到 media service。
├── backend/src/order/api/*ImageFileAPIMessage.scala
│   # [MODIFY] 订单聊天、退款图片保存调用切换到 media service。
├── backend/src/shared/objects/ErrorBody.scala
│   # [KEEP] 通用 HTTP 错误 DTO，继续保留 shared。
├── backend/src/shared/objects/HealthOk.scala
│   # [KEEP] 健康检查 DTO，继续保留 shared。
├── backend/src/shared/objects/Promotion.scala
│   # [MOVE or DOCUMENT] 明确业务归属；若确为跨模块促销领域，建立受控 shared-domain 说明。
├── backend/src/shared/objects/Voucher.scala
│   # [MOVE or DOCUMENT] 明确业务归属；优先评估迁移到 user 优惠券领域。
├── frontend/src/pages/Login/stores/use-login-page-store.ts
│   # [NEW/MOVE] 登录页私有 store 迁移到页面目录。
├── frontend/src/pages/Register/stores/use-register-page-store.ts
│   # [NEW/MOVE] 注册页私有 store 迁移到页面目录。
├── frontend/src/pages/RiderApp/stores/use-rider-app-store.ts
│   # [NEW/MOVE] 骑手页私有 store 迁移到页面目录。
├── frontend/src/pages/CustomerPortal/stores/
│   # [NEW/MOVE] 顾客页 store、initialState、types、helpers、favoritesStorage 迁移到页面目录。
├── frontend/src/pages/MerchantConsole/stores/
│   # [NEW/MOVE] 商家控制台 store、initialState、types、helpers 迁移到页面目录。
├── frontend/src/stores/pages/
│   # [MODIFY/DELETE] 第一阶段保留 re-export 兼容入口；最终删除页面私有实现。
├── frontend/src/pages/{Login,Register,RiderApp,CustomerPortal,MerchantConsole}/index.tsx
│   # [MODIFY] 导入路径切换到页面就近 stores。
├── frontend/src/pages/MerchantConsole/components/*.tsx
│   # [MODIFY] useMerchantConsoleStore 导入路径切换到 ../stores 或页面别名。
├── frontend/src/pages/CustomerPortal/components/*.tsx
│   # [MODIFY] useCustomerPortalStore 导入路径切换到 ../stores 或页面别名。
└── .codebuddy/skills/maintainability-audit/scripts/check-maintainability.sh
    # [MODIFY] 整改后将对应历史 warning 收敛为 pass；补充对新目标路径的检查。
```

## Key Code Structures

无需在计划阶段固定具体函数实现。实施时应保持现有 APIMessage、Zustand store 和 Circe codec 的外部签名稳定，仅调整目录归属和内部调用路径。

## Agent Extensions

### Skill

- **maintainability-audit**
- Purpose: 基于已创建的可维护性审计规则识别整改范围，并在每个阶段验证 warning 是否减少。
- Expected outcome: 输出可维护性风险快照，最终让新增结构满足 `DIRECTORY_LAYERING_GUIDE.md`。

- **layered-feature-development**
- Purpose: 为后端 service/validator/media/json 和前端页面 stores 的落位提供分层决策。
- Expected outcome: 每个迁移文件都有清晰归属，避免继续把代码塞入 `api/` 或 `shared/`。

- **type-safety-audit**
- Purpose: 确认整改过程中 APIMessage、objects、apiTypes 和前后端契约没有漂移。
- Expected outcome: 结构迁移后仍保持 API 对齐、类型安全和统一网关规则。

### SubAgent

- **code-explorer**
- Purpose: 在拆分 `APIMessageSupport`、迁移 shared/media、迁移前端 store 前做跨文件引用追踪。
- Expected outcome: 找全 import、调用点、路由注册和 codec 依赖，降低遗漏风险。