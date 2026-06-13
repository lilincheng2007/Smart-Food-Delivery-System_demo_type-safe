# 目录职责与分层规范

> 目标：让项目在功能继续扩展时保持长期可维护。本文定义每类代码应该放在哪里、哪些目录不能混用、以及新增功能时的最小分层要求。

## 1. 总原则

1. **后端事实源优先**：订单、库存、钱包、优惠、退款、评价、通知、聊天等真实业务状态以后端 PostgreSQL 为准。
2. **APIMessage 统一入口**：业务 API 默认只走 `POST /api/{apiName}`；公开图片读取路由是明确例外。
3. **就近内聚，语义命名**：页面私有代码靠近页面；领域通用能力放入对应业务模块；跨模块基础设施拆到 `platform`、`auth`、`db`、`bootstrap`、`domain`、`media` 等明确目录，不再新增 `shared` 实现。
4. **一层只做一层的事**：`api/` 接请求，`services/` 编排业务，`validators/` 做校验，`tables/` 访问数据库，`objects/` 定义类型。
5. **渐进迁移**：不为了规范做大爆炸重构；每次修改相关文件时顺手收敛边界，并保留兼容入口。

## 2. 后端目录职责

```text
backend/src/{module}/
  api/             # APIMessage 入口：解析请求、声明角色、调用 service/validator
  routes/          # 注册 APIMessage，不写业务规则
  services/        # 业务流程编排、状态流转、事务边界、跨表协调
  validators/      # 参数校验、业务前置条件、权限上下文校验
  tables/          # JDBC/SQL/PreparedStatement，返回领域对象或 record
  objects/         # 领域对象、枚举、值对象
    apiTypes/      # API Request/Response DTO
    records/       # 可选：数据库行映射对象，不直接暴露给前端
  utils/           # 模块内纯函数、格式化、无状态 helper
```

现有业务模块包括 `admin`、`ai`、`merchant`、`order`、`review`、`rider`、`user`、`promotion`。基础设施目录包括 `platform`、`auth`、`db`、`bootstrap`、`domain`、`media`。新增业务优先进入已有模块；只有形成稳定独立领域时才新增顶层模块。

### 2.1 `api/`

允许：

- 一个文件定义一个 `final case class XxxAPIMessage`。
- 声明请求/响应类型、鉴权角色、必要的轻量参数拆解。
- 调用 `services/`、`validators/`、`tables/` 的公开函数。

禁止：

- 新增 `*APIMessageSupport.scala` 这类模糊支撑文件。
- 在 API 文件中堆放 SQL、长业务流程、图片处理细节、复杂 DTO 转换。
- 把多个 APIMessage 放进同一个文件。

迁移建议：现有 `MerchantAPIMessageSupport`、`OrderAPIMessageSupport` 等应按职责拆到 `services/`、`validators/`、`utils/` 或独立 `media/` 能力中。

### 2.2 `services/`

用于表达业务用例，例如：

- `OrderStatusTransitionService`
- `CheckoutService`
- `MerchantProductService`
- `RefundDecisionService`
- `StoredImageService`

职责：

- 编排多个 table 调用。
- 维护订单状态、库存扣减、钱包余额、优惠结算、退款状态等核心规则。
- 返回领域对象或 API 需要的结果结构。

### 2.3 `validators/`

用于表达校验和前置条件，例如：

- `OrderOwnershipValidator`
- `MerchantProductValidator`
- `ImageUploadValidator`
- `RefundRequestValidator`

职责：

- 校验输入长度、枚举合法性、状态是否允许迁移、角色是否能操作资源。
- 返回 typed error / `Either` / `IO.raiseError`，不要在页面或前端 store 里补业务校验。

### 2.4 `tables/`

职责：

- 集中 SQL、事务内数据库读写、表初始化和迁移。
- 长 SQL 的字段列表、`VALUES` 占位符和绑定索引必须同步维护。
- 新增表必须提供 `TableInitializer`，迁移使用 `ALTER TABLE ... ADD COLUMN IF NOT EXISTS`。

禁止：

- 在 `tables/` 中写面向页面的拼装逻辑。
- 让 API 层直接拼长 SQL。

### 2.5 `objects/`

分类：

| 类型 | 目录 | 说明 |
|---|---|---|
| 领域对象 | `objects/` | `Order`、`Product`、`Review` 等业务概念 |
| API DTO | `objects/apiTypes/` | `*Request`、`*Response`，只服务接口契约 |
| 数据库记录 | `objects/records/` | 可选，表行映射，不直接作为前端契约 |
| 枚举/值对象 | `objects/` | 状态、ID、金额、时间窗口等 |

规则：

- 优先一对象一文件。
- 不用 `index.scala` barrel 隐藏真实来源。
- `*Request` / `*Response` 不放在 `objects/` 根目录。
- 数据库 record 与 API response 不复用同一个类型，除非字段语义完全一致且不会泄露内部字段。

## 3. JSON 序列化规范

当前项目使用 `backend/src/platform/json/ApiJsonCodecs.scala` 作为 Circe codec 统一聚合入口，并在各模块 `json/` 目录提供 codec 发现入口。

新增或修改类型时：

1. 新增领域对象或 API DTO 后，确认 `ApiJsonCodecs.scala` 中有对应 `Encoder` / `Decoder` / `Codec`。
2. 不在 API 文件中临时手写零散 JSON 编解码。
3. 若类型只属于单个模块，优先让 codec 命名带模块语义，避免同名冲突。
4. 当 `ApiJsonCodecs.scala` 继续膨胀时，逐步迁移为模块化 codec：

```text
backend/src/{module}/json/{Module}JsonCodecs.scala
backend/src/platform/json/ApiJsonCodecs.scala  # 统一聚合导出各模块 codec
```

前端 TypeScript 类型仍需与后端手写对齐；新增 API 后同步更新 `API_INVENTORY.md`，并运行类型安全审计。

## 4. 拆分后的基础目录边界

`backend/src/shared/` 不再作为长期实现目录；新增 Scala 实现必须放入语义明确的目录。

| 目录 | 职责 | 典型内容 |
|---|---|---|
| `platform/` | 平台级基础设施 | `api/` APIMessage 网关、`http/` 通用 HTTP DTO、`json/` codec 聚合、`interop/` 内部互操作 |
| `auth/` | 认证鉴权基础能力 | JWT 签发、校验、认证工具 |
| `db/` | 数据库连接与事务基础设施 | 连接池、事务 session、数据库初始化入口 |
| `bootstrap/` | 启动种子与演示数据 | 初始账号、演示商家、种子订单 |
| `domain/` | 跨模块稳定类型 | ID type alias、角色、稳定枚举、通用响应 DTO；业务对象不要继续扩大到这里 |
| `media/` | 跨业务媒体能力 | 图片上传校验、存储表、读取路由、历史图片导入 |
| `promotion/` | 促销/优惠券领域 | `Promotion`、`Voucher`、优惠计算、使用次数、券生成与校验 |

规则：

- 禁止新增 `backend/src/shared/**/*.scala` 实现文件。
- `ErrorBody`、`HealthOk` 属于 `platform/http/objects/`。
- `OkResponse` 等极简通用 API DTO 属于 `domain/apiTypes/`。
- `Promotion`、`Voucher` 和相关计算/校验属于 `promotion/`。
- 如果某段代码出现明确业务词，优先回到对应业务模块或独立领域模块，不放入基础设施目录。

## 5. 媒体/静态图片能力

图片上传、图片元数据表、图片公开读取路由已经形成跨业务能力，长期应视为独立 `media` 模块，而不是继续扩大 `shared`。

建议目标结构：

```text
backend/src/media/
  routes/StoredImageRoutes.scala
  services/StoredImageService.scala
  validators/ImageUploadValidator.scala
  tables/StoredImageTable.scala
  tables/StoredImageMigration.scala
  objects/StoredImage.scala
```

当前公开读取路由仍保持：

```text
GET /api/merchant/store-images/{fileName}
GET /api/merchant/product-images/{fileName}
GET /api/orders/refund-images/{fileName}
GET /api/reviews/images/{fileName}
```

迁移时只移动实现归属，不改变 HTTP 路径和前端返回值。

## 6. 前端目录职责

```text
frontend/src/
  apis/              # APIMessage 客户端，一 API 一文件
  objects/           # 与后端契约对齐的领域对象和 apiTypes
  pages/             # 页面与页面私有代码
  components/        # 跨页面通用 UI 组件
  hooks/             # 跨页面通用 hooks
  lib/               # 通用纯函数和客户端基础工具
  stores/            # 仅保留真正全局 store 或兼容 re-export
```

页面目录建议：

```text
frontend/src/pages/{Page}/
  index.tsx          # 页面装配，不堆业务细节
  components/        # 页面私有 UI
  hooks/             # 页面数据协调、轮询、提交动作
  objects/           # 页面私有类型、常量、tab 定义
  functions/         # 页面私有纯函数、筛选、格式化
  stores/            # 页面私有 Zustand store（如仍需要）
```

规则：

- `frontend/src/stores/` 只放 `use-app-store.ts` 等全局状态，或迁移期兼容入口。
- 页面私有 store 放到对应 `pages/{Page}/stores/`；`frontend/src/stores/` 不再承载页面私有实现。
- 前端 store 不保存真实业务事实源，只缓存展示状态、会话状态、UI 偏好和请求状态。
- 页面业务动作必须调用 `apis/`，不得直接拼业务 URL。
- 页面私有类型不要放进 `frontend/src/objects/`，除非它是前后端契约对象。

## 7. API 与契约变更流程

新增或修改业务 API 时按顺序执行：

1. 后端新增/修改 `backend/src/{module}/api/XxxAPIMessage.scala`。
2. 后端新增/修改 `backend/src/{module}/objects/apiTypes/*Request.scala`、`*Response.scala`。
3. 后端确认领域对象、record、codec、routes 注册。
4. 前端新增/修改 `frontend/src/apis/{module}/XxxAPI.ts`。
5. 前端新增/修改 `frontend/src/objects/{module}/apiTypes/` 与领域对象。
6. 页面通过 hook/store 调用 API，不直接写业务 URL。
7. 更新 `API_INVENTORY.md`。
8. 运行：

```bash
.codebuddy/skills/type-safety-audit/scripts/check-type-safety.sh /Users/leonli/Desktop/Type-safe_project
.codebuddy/skills/maintainability-audit/scripts/check-maintainability.sh /Users/leonli/Desktop/Type-safe_project
npm run typecheck --prefix frontend
cd backend && sbt -batch compile
```

## 8. 命名规范

避免：

- `XxxAPIMessageSupport`
- `CommonHelper`
- `SharedUtils`
- `DataManager`
- `BusinessSupport`

推荐使用职责名：

- `OrderStatusTransitionService`
- `CheckoutPricingService`
- `RefundEligibilityValidator`
- `MerchantImageValidator`
- `StoredImageService`
- `CustomerPortalStore`

命名应回答三个问题：属于哪个领域、做什么动作、处在哪一层。

## 9. 渐进迁移策略

1. **先冻结规则**：新代码必须符合本文，不继续扩大旧问题。
2. **触碰即收敛**：修改某个 API 时，把同文件附近的 support/helper 拆到目标层。
3. **保留兼容入口**：前端 store 迁移时可保留旧路径 re-export，避免一次性改动过大。
4. **先文档后迁移**：跨模块迁移前先记录目标路径、兼容策略和验证命令。
5. **小步验证**：每次迁移只处理一个模块或一个页面，运行对应检查。

## 10. 代码评审清单

- [ ] 是否新增了模糊的 `Support` / `Helper` / `Manager` 文件？
- [ ] API 文件是否只做入口编排？
- [ ] 业务规则是否在后端 service/validator，而不是前端 state？
- [ ] SQL 是否只在 `tables/`？
- [ ] `Request` / `Response` 是否在 `apiTypes/`？
- [ ] codec 是否可发现且已注册？
- [ ] 是否把业务对象、表或路由塞进 `shared`？
- [ ] 页面私有 store/hooks/functions/objects 是否与页面就近组织？
- [ ] 是否同步更新 `API_INVENTORY.md`、`AGENTS.md` 或子项目 README？
