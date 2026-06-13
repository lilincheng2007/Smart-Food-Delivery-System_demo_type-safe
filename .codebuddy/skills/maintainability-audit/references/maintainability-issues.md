# 可维护性问题目录

## 1. 模糊支撑文件

症状：`api/` 下出现 `*APIMessageSupport.scala`、`CommonHelper`、`DataManager` 等名字。

判定：文件名无法说明具体层级和职责，通常说明 API 层承载了过多逻辑。

迁移：

- 业务流程 → `services/`
- 参数/权限/状态校验 → `validators/`
- 纯函数 → `utils/`
- 图片存储/读取 → `media/`

## 2. `objects` 变成类型杂物间

症状：领域对象、API response、数据库 record、页面 view model 混放。

判定：同一类型同时服务数据库、API 和页面展示，或一个文件堆多个不相关类型。

迁移：

- `objects/`：领域对象和值对象。
- `objects/apiTypes/`：`*Request`、`*Response`。
- `objects/records/`：数据库行映射。
- `pages/{Page}/objects/`：页面私有展示类型。

## 3. 序列化不可发现

症状：新增类型后运行时报 codec 缺失，或 `ApiJsonCodecs.scala` 引入越来越多业务细节。

判定：开发者无法从对象或模块附近找到 codec 方案。

迁移：先保持 `platform/json/ApiJsonCodecs.scala` 可用；当模块类型变多时拆出 `{module}/json/{Module}JsonCodecs.scala`，再由 platform/json 聚合。

## 4. `shared` 污染

症状：`shared` 下出现业务对象、业务表、业务路由或无法归类的代码。

判定：名称或逻辑包含明确业务领域词，却放在 `shared`。

迁移：业务代码回到对应模块；跨业务媒体能力抽为 `media`；基础设施才保留在 `shared`。

## 5. 页面状态远离页面

症状：页面 UI 在 `pages/`，页面私有 Zustand store 在 `frontend/src/stores/pages/`。

判定：修改一个页面需要跨多个远距离目录，且 store 只服务单个页面。

迁移：页面私有 store 放 `pages/{Page}/stores/`；旧 `frontend/src/stores/pages/*` 可暂时 re-export 兼容。

## 6. 前端越权承担业务规则

症状：前端直接决定订单状态、钱包余额、库存扣减、优惠核销、退款结果。

判定：刷新页面或换角色后无法从后端重新查询到一致结果。

迁移：规则放后端 service/validator；前端只发起意图并展示后端结果。
