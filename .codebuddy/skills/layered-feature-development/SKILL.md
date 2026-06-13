---
name: layered-feature-development
description: "在 Type-safe_project 中新增或重构功能时使用，确保功能按长期可维护结构落位。指导后端 APIMessage、services、validators、tables、objects/apiTypes/records、JSON codec、media 模块，以及前端 apis、objects、pages/components/hooks/functions/stores 的分层与文档同步。"
---

# Type-safe Project 分层功能开发 Skill

## 目的

在 `Type-safe_project` 新增、修改或重构功能时，先确定目录归属和层级边界，再动手编码。保持业务事实源在后端、APIMessage 契约前后端对齐、页面私有代码就近组织、`shared` 不继续膨胀。

## 何时使用

- 新增业务 API、页面、领域对象、数据库表或媒体上传能力。
- 重构现有 `APIMessageSupport`、大型 store、大型 object 文件或 `shared` 中的业务代码。
- 用户要求“按可维护结构实现”“别再堆到 shared/api/store 里”“补分层”。
- 修改会同时影响前端、后端、API 契约和文档。

## 开发流程

1. 阅读 `DIRECTORY_LAYERING_GUIDE.md`，确认目标模块和分层规则。
2. 判断变更属于哪个业务模块：`admin`、`ai`、`merchant`、`order`、`review`、`rider`、`user` 或独立 `media`。
3. 后端按顺序落位：`objects` / `apiTypes` / `records` → `tables` → `validators` → `services` → `api` → `routes` → `json`。
4. 前端按顺序落位：`objects` / `apiTypes` → `apis` → 页面 `hooks` / `stores` / `functions` / `components` → `index.tsx` 装配。
5. 同步文档：新增 API 更新 `API_INVENTORY.md`；改变目录规则更新 `DIRECTORY_LAYERING_GUIDE.md` 和 `AGENTS.md`。
6. 运行类型安全和可维护性检查。

## 后端落位规则

### `api/`

只创建 `XxxAPIMessage.scala`。保持一文件一 APIMessage，只做入口编排：读取请求、拿鉴权上下文、调用 service/validator、返回 response。

不要新增 `XxxAPIMessageSupport.scala`。如果需要支撑逻辑，按职责放入：

- `services/`：业务流程、状态流转、事务编排。
- `validators/`：输入校验、权限前置条件、状态可变更性。
- `utils/`：模块内纯函数。
- `media/`：图片存储、图片读取、图片元数据。

### `objects/`

- 领域对象放 `objects/`。
- `*Request` / `*Response` 放 `objects/apiTypes/`。
- 表行映射需要独立表达时放 `objects/records/`。
- 优先一对象一文件，避免把多个不相关类型堆在同一文件。

### `tables/`

- 只处理 SQL、PreparedStatement、表初始化和迁移。
- 不在 table 中拼页面响应模型。
- 新表必须提供 `TableInitializer`，兼容已有库使用 `ADD COLUMN IF NOT EXISTS`。

### `json/`

当前必须确认 `backend/src/platform/json/ApiJsonCodecs.scala` 注册 codec。新增大量模块类型时，优先新增 `{module}/json/{Module}JsonCodecs.scala`，再由 platform/json 聚合导出。

### `shared/`

只放基础设施。任何带明确业务词的代码先尝试放回业务模块；跨业务图片能力优先抽为 `media`，不要继续扩大 `shared/tables` 或 `shared/routes`。

## 前端落位规则

### `apis/`

每个后端 `XxxAPIMessage.scala` 对应一个前端 `XxxAPI.ts`，通过 `sendAPI` 调用，不直接拼业务 URL。

### `objects/`

只放与后端契约对齐的领域对象和 API DTO。页面私有 tab、筛选条件、展示状态不要放这里。

### `pages/{Page}/`

页面私有代码就近组织：

```text
index.tsx
components/
hooks/
objects/
functions/
stores/
```

`index.tsx` 负责装配，不堆复杂业务逻辑。页面私有 Zustand store 放 `pages/{Page}/stores/`；`frontend/src/stores/` 只保留全局 store 或迁移期兼容 re-export。

## 完成前检查

- API 是否一文件一消息，前后端文件名是否对应？
- 是否没有新增 `*APIMessageSupport`？
- 业务规则是否在后端 service/validator？
- SQL 是否只在 `tables/`？
- `Request` / `Response` 是否位于 `apiTypes/`？
- codec 是否注册且可发现？
- 是否没有把业务对象、业务表、业务路由塞进 `shared`？
- 页面私有状态是否与页面就近组织？
- 是否更新 `API_INVENTORY.md`、`DIRECTORY_LAYERING_GUIDE.md` 或 `AGENTS.md`？

## 参考资源

- `references/layering-decision-tree.md` — 新功能目录落位决策树。
- `DIRECTORY_LAYERING_GUIDE.md` — 项目目录职责与分层规范。
- `.codebuddy/skills/type-safety-audit/` — 类型安全与前后端契约审计。
- `.codebuddy/skills/maintainability-audit/` — 可维护性审计。
