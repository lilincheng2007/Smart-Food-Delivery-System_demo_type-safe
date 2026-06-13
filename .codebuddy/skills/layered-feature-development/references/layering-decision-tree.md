# 分层落位决策树

## 后端

1. 是否是业务 API 入口？
   - 是：`backend/src/{module}/api/XxxAPIMessage.scala`
   - 否：继续判断。

2. 是否是 API 请求/响应契约？
   - 是：`backend/src/{module}/objects/apiTypes/`
   - 否：继续判断。

3. 是否是稳定业务概念？
   - 是：`backend/src/{module}/objects/`
   - 否：继续判断。

4. 是否是数据库行映射？
   - 是：`backend/src/{module}/objects/records/`
   - 否：继续判断。

5. 是否直接写 SQL 或迁移？
   - 是：`backend/src/{module}/tables/`
   - 否：继续判断。

6. 是否编排业务流程、事务或状态流转？
   - 是：`backend/src/{module}/services/`
   - 否：继续判断。

7. 是否校验参数、权限、状态可变更性？
   - 是：`backend/src/{module}/validators/`
   - 否：继续判断。

8. 是否是模块内无状态纯函数？
   - 是：`backend/src/{module}/utils/`
   - 否：继续判断。

9. 是否是跨业务图片/文件能力？
   - 是：`backend/src/media/`
   - 否：继续判断。

10. 是否是无业务归属的基础设施？
    - 是：`backend/src/platform/`、`backend/src/domain/` 或对应业务模块
    - 否：不要放进 `shared`，重新确认模块归属。

## 前端

1. 是否是 APIMessage 客户端？
   - 是：`frontend/src/apis/{module}/XxxAPI.ts`
   - 否：继续判断。

2. 是否与后端契约对齐？
   - 领域对象：`frontend/src/objects/{module}/`
   - API DTO：`frontend/src/objects/{module}/apiTypes/`
   - 否：继续判断。

3. 是否只服务单个页面？
   - UI：`frontend/src/pages/{Page}/components/`
   - hook：`frontend/src/pages/{Page}/hooks/`
   - 类型/常量：`frontend/src/pages/{Page}/objects/`
   - 纯函数：`frontend/src/pages/{Page}/functions/`
   - Zustand store：`frontend/src/pages/{Page}/stores/`

4. 是否跨多个页面复用？
   - UI：`frontend/src/components/`
   - hook：`frontend/src/hooks/`
   - 纯函数：`frontend/src/lib/`
   - 全局状态：`frontend/src/stores/`

5. 是否代表真实业务事实源？
   - 是：不能只放前端，必须有后端 API 和持久化支持。
