---
name: maintainability-audit
description: "审计 Type-safe_project 的长期可维护性与分层边界。检查 APIMessageSupport 等模糊支撑文件、api/services/validators/tables 职责混杂、shared 目录污染、objects 与 apiTypes/records 边界、JSON codec 可发现性、媒体能力归属、前端页面 store 是否就近组织，以及文档是否同步。"
---

# Type-safe Project 可维护性审计 Skill

## 目的

审计 `Type-safe_project` 的工程分层、目录归属和长期维护风险。聚焦类型安全之外的结构问题：API 层过重、`shared` 泛化、对象职责混杂、序列化集中膨胀、页面状态远离页面、媒体能力没有独立模块边界。

## 何时使用

- 评估项目是否适合长期维护。
- 用户指出 `APIMessageSupport`、`shared`、`objects`、`stores`、序列化、目录分层等问题。
- 新增或重构模块后，检查是否引入新的结构债务。
- 代码审查时需要按分层规范给出问题清单和迁移建议。

## 工作流

1. 读取根目录 `DIRECTORY_LAYERING_GUIDE.md`，以其中规则作为分层判定标准。
2. 读取 `AGENTS.md`、`API_INVENTORY.md` 和 `.codebuddy/skills/type-safety-audit/` 中的类型安全规则，避免与现有 APIMessage 约定冲突。
3. 运行 `scripts/check-maintainability.sh /Users/leonli/Desktop/Type-safe_project` 获取结构风险快照；需要把警告升级为失败时使用 `STRICT=1`。
4. 对脚本指出的文件做人工复核，区分“当前遗留债务”和“本次修改新增问题”。
5. 按以下维度输出审计结果：API 层、业务层、对象层、序列化、`shared`、媒体资源、前端页面状态、文档同步。
6. 给出渐进迁移建议；避免建议一次性大规模重构。

## 审计重点

### API 层

- 检查 `backend/src/{module}/api/` 下是否存在 `*APIMessageSupport.scala`。
- 检查 APIMessage 文件是否承担 SQL、图片处理、复杂业务状态流转或大型 DTO 转换。
- 建议将支撑逻辑拆到 `services/`、`validators/`、`utils/` 或 `media/`。

### 业务层

- 检查核心业务规则是否存在于后端 service/validator，而不是前端 Zustand store。
- 检查订单、库存、钱包、优惠、退款、评价等状态是否由后端校验。

### 对象层

- 检查 `objects/` 根目录是否混入 `*Request` / `*Response`。
- 检查一个对象文件是否堆叠过多 case class/type。
- 检查数据库 record、领域对象、API DTO 是否混用。

### 序列化

- 检查新增类型是否能在 `platform/json/ApiJsonCodecs.scala` 找到 codec。
- 当 codec 文件持续膨胀时，建议迁移到 `{module}/json/{Module}JsonCodecs.scala` 并由 platform/json 聚合。

### `shared`

- 允许 `platform`、`auth`、`db`、`bootstrap`、`domain`、`media`、`promotion` 等明确目录。
- 警惕 `backend/src/shared` 中残留 Scala 实现文件。
- 如果名称含 `merchant`、`order`、`review`、`rider`、`refund`、`voucher`，优先判断是否应回到业务模块或独立模块。

### 前端

- 检查 `frontend/src/stores/pages/` 中页面私有 store 是否应迁移到 `frontend/src/pages/{Page}/stores/`。
- 检查页面私有类型是否误放进 `frontend/src/objects/`。
- 检查页面是否绕过 `apis/` 直接拼业务 URL。

## 参考资源

- `scripts/check-maintainability.sh` — 可维护性结构检查脚本。
- `references/maintainability-issues.md` — 常见问题、判定标准和迁移方向。
- `DIRECTORY_LAYERING_GUIDE.md` — 项目目录职责与分层规范。
