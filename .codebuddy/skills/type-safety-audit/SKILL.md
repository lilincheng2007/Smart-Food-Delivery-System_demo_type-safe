---
name: type-safety-audit
description: "审计 Type-safe_project 的前后端类型安全与 sample 风格结构一致性。检查后端 XxxAPIMessage.scala 与前端 src/apis/XxxAPI.ts 是否一一对应，objects 与 objects/apiTypes 是否前后端对齐，页面是否按 components/hooks/objects/functions 拆分，是否存在 Unsafe/Unsfe 组件或类型安全逃逸口、硬编码字符串替代枚举、前端 state 越权替代后端逻辑、路由绕过 APIMessage、README/规则过时等问题。"
---

# Type-Safe Project 类型安全审计 Skill

## 目的

审计 `Type-safe_project` 的前后端契约、目录结构和业务状态来源，确保项目保持 sample 风格：一 API 一文件、请求/响应对象进入 `apiTypes/`、页面按功能子目录拆分、真实业务数据以后端为准。

## 何时使用

- 新增或重构 API、objects、页面目录后验证结构一致性。
- 代码审查时检查前后端契约是否漂移。
- 排查老师反馈的类型安全、目录混乱、前后端不对应问题。
- 修改 `sample` 结构规范后同步审计规则。

## 项目关键路径

| 维度 | 后端路径 | 前端路径 | 规则 |
|------|---------|---------|------|
| API 消息 | `backend/src/{module}/api/{XxxAPIMessage}.scala` | `frontend/src/apis/{module}/{XxxAPI}.ts` | 后端文件名去掉 `Message` 后必须等于前端文件名 |
| API 基础设施 | `backend/src/platform/api/` | `frontend/src/apis/shared/` | 不计入业务 API 对齐 |
| 领域对象 | `backend/src/{module}/objects/{ObjectName}.scala` | `frontend/src/objects/{module}/{ObjectName}.ts` | 真正系统 object，一对象一文件 |
| 请求/响应对象 | `backend/src/{module}/objects/apiTypes/{Name}.scala` | `frontend/src/objects/{module}/apiTypes/{Name}.ts` | `*Request` / `*Response` 只能放这里 |
| 共享枚举/ID | `backend/src/domain/ids.scala` | `frontend/src/objects/shared/ids.ts` | 枚举与 ID 类型必须真实使用 |
| 页面结构 | - | `frontend/src/pages/{Page}/index.tsx` + `components/`、`hooks/`、`objects/`、`functions/` | `index.tsx` 只做页面装配 |
| 路由注册 | `backend/src/{module}/routes/{Module}Routes.scala` | `frontend/src/router.tsx` | 业务 API 走统一 `POST /api/{apiName}` |

模块列表：`ai`、`user`、`merchant`、`order`、`rider`；前端 `objects/shared` 对应后端拆分后的 `domain`、`promotion`、`platform`。

## 审计检查清单

### 检查 1：API 一文件一消息，前后端文件名一一对应

规则：
- 后端业务 API 必须为 `backend/src/{module}/api/XxxAPIMessage.scala`。
- 前端业务 API 必须为 `frontend/src/apis/{module}/XxxAPI.ts`。
- 禁止后端残留 `*APIMessages.scala` 聚合文件。
- 禁止前端残留 `frontend/src/api`、`*APIMessages.ts`、`*Api.ts`。
- 每个后端 API 文件只能定义一个同名 `final case class XxxAPIMessage`。
- 每个前端 API 文件只能定义一个同名 `class XxxAPI extends APIMessage`。

### 检查 2：Objects 与 apiTypes 分层对齐

规则：
- 真正系统对象放在 `objects/{module}/` 根目录，例如 `Order`、`Product`、`CustomerProfile`。
- API 请求/响应包装类型放在 `objects/{module}/apiTypes/`，例如 `CheckoutRequest`、`CustomerMeResponse`、`OkResponse`。
- 前后端同名文件必须在同一层级对应：root 对 root，`apiTypes` 对 `apiTypes`。
- 禁止 `*Request` / `*Response` 出现在 `objects/{module}/` 根目录。
- 禁止 `index.ts` / `index.scala` barrel 文件隐藏真实对应关系。

### 检查 3：页面按 sample 拆分

规则：
- 页面入口保留 `frontend/src/pages/{Page}/index.tsx`。
- 页面局部组件进入 `components/`。
- 页面局部类型/常量进入 `objects/`。
- 纯函数、筛选、格式化进入 `functions/`。
- 数据加载和提交协调优先进入 `hooks/`；若暂时保留 Zustand store，页面目录仍需先完成组件/函数拆分。

### 检查 4：枚举与 ID 类型真实使用

规则：
- `UserRole`、`MerchantCategory`、`RiderStatus`、`OrderStatus`、`ListingStatus`、`InventoryStatus` 必须使用共享枚举/常量，不用裸字符串表达状态分支。
- `UserId`、`MerchantId`、`RiderId`、`ProductId`、`OrderId`、`VoucherId` 应用于对象字段和 API 参数。

### 检查 5：前端 state 不越权替代后端业务逻辑

规则：
- 钱包、订单状态、优惠券、骑手能量/免责卡等真实业务状态必须由后端 API 校验并持久化。
- 前端 Zustand store 只能缓存展示状态，不能本地计算后冒充后端结果写回。
- `localStorage` 只允许保存 JWT/session/UI 偏好，不保存真实业务数据源。

### 检查 6：路由使用纯 APIMessage 类型

规则：
- 所有业务 API 走 `APIMessage` / `APIWithRoleMessage` 和统一网关 `POST /api/{apiName}`。
- 静态资源路由可作为明确例外，例如商户上传图片访问路由。
- 当前前端 `apiNameOf()` 使用显式 `apiName`，不依赖 `constructor.name`，无需强制 `keep_classnames`。

### 检查 7：Unsafe 组件与类型安全逃逸口

规则：
- 禁止前端新增或使用名称包含 `Unsafe` / `Unsfe` 的组件、函数、文件或导出。
- 禁止前端使用 `dangerouslySetInnerHTML`、`eval(...)`、`new Function(...)`、`as any`、`@ts-ignore`、`@ts-expect-error` 绕过类型与渲染安全。
- 禁止后端使用 `unsafeRun*`、`Uri.unsafeFromString`、`asInstanceOf` 等绕过 IO/解析/类型安全的逃逸口。
- 如确有第三方库适配需求，应封装为具名 safe helper，并用显式校验、`Either` / `IO.fromEither` 或 typed wrapper 消除 unsafe 调用。

## 工作流

1. 读取 `references/fe-be-contract-map.md` 获取当前契约快照。
2. 读取 `references/type-safety-issues-catalog.md` 获取常见问题与修复方式。
3. 执行 `scripts/check-type-safety.sh /Users/leonli/Desktop/Type-safe_project`。
4. 若失败，优先修复：API 文件对齐 → objects/apiTypes 分层 → Unsafe/Unsfe 与类型安全逃逸口 → 前端越权状态 → 枚举/ID 使用 → 文档过时。
5. 修复后同时运行前端 `npm run typecheck --prefix frontend` 与后端 `cd backend && sbt -batch compile`。

## 参考文件

- `references/fe-be-contract-map.md` — 当前 API、objects、apiTypes 对照快照。
- `references/type-safety-issues-catalog.md` — 类型安全与结构问题目录。
- `scripts/check-type-safety.sh` — 自动化结构检查脚本。
