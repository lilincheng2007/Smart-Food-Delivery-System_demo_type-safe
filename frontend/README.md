# Delivery Frontend

`frontend/` 是外卖平台的 Web 前端，基于 Vite + React + TypeScript 构建。前端只通过 HTTP 调用后端 `APIMessage` 网关，页面状态由 Zustand 管理，UI 使用 shadcn/ui 与 Tailwind CSS。

## 技术栈

- Vite 8
- React 19
- TypeScript 5
- React Router
- Zustand
- shadcn/ui + Radix UI
- Tailwind CSS v4
- lucide-react

## 启动

```bash
cd frontend
npm install
npm run dev
```

默认 Vite dev server 会将 `/api` 代理到 `http://localhost:8787`。请先启动后端，或在仓库根目录直接运行：

```bash
npm run dev
```

## 常用命令

| 命令 | 说明 |
|---|---|
| `npm run dev` | 启动 Vite 开发服务器 |
| `npm run typecheck` | TypeScript 类型检查 |
| `npm run lint` | ESLint 检查 |
| `npm run lint:fix` | 自动修复可修复的 lint 问题 |
| `npm run build` | 类型检查并构建生产产物 |
| `npm run preview` | 预览生产构建 |
| `npm run ui:add -- <component>` | 添加 shadcn/ui 组件 |

## 环境变量

| 变量 | 默认值 | 说明 |
|---|---|---|
| `VITE_BACKEND_URL` | `http://localhost:8787` | Vite dev proxy 的后端目标 |
| `VITE_API_BASE` | `/api` | 前端 API base path |

示例：

```bash
VITE_BACKEND_URL=http://localhost:8787 npm run dev
```

## 目录结构

```text
src/
├── api/                    # APIMessage 封装
│   ├── ai/                 # AI 搜索 API
│   ├── merchant/           # 商家/目录 API
│   ├── order/              # 订单 API
│   ├── rider/              # 骑手 API
│   ├── user/               # 登录、注册、顾客资料 API
│   └── shared/             # sendAPI、client、TaskIO、APIMessage 基类
├── components/             # 通用组件
│   ├── ui/                 # shadcn/ui 组件
│   ├── AISearchBar.tsx
│   └── AISearchResults.tsx
├── lib/                    # auth session、媒体 URL、工具函数
├── objects/                # 与后端对应的契约对象
├── pages/                  # 页面模块
├── stores/                 # Zustand store
├── router.tsx              # 路由定义
├── main.tsx                # React 入口
└── index.css               # 全局样式与 Tailwind
```

## 页面路由

| 路由 | 页面 | 角色 |
|---|---|---|
| `/auth/login` | 登录页 | 游客 |
| `/auth/register` | 注册页 | 游客 |
| `/delivery/customer` | 顾客首页/订单/钱包等 | 顾客 |
| `/delivery/customer/m/:merchantId` | 店内点餐页 | 顾客 |
| `/delivery/customer/checkout` | 顾客结算页 | 顾客 |
| `/delivery/merchant` | 商家控制台 | 商家 |
| `/delivery/rider` | 骑手工作台 | 骑手 |

路由守卫位于 `src/components/RoleRouteGuards.tsx`，会根据 `src/lib/auth-session.ts` 中保存的 JWT 与角色控制访问。

## API 调用方式

前端 API 使用统一模式：

```text
APIMessage class -> sendAPI -> POST /api/{apiName}
```

关键文件：

- `src/api/shared/APIMessage.ts`：前端 APIMessage 基类
- `src/api/shared/sendAPI.ts`：根据 `apiName` 发起请求
- `src/api/shared/client.ts`：注入 `Authorization: Bearer <token>`，统一处理错误
- `src/api/shared/TaskIO.ts`：任务式异步封装

示例：`src/api/ai/AISearchApi.ts` 中的 `AISearchAPI` 会调用：

```text
POST /api/aisearchapi
```

## 状态管理

页面级业务状态使用 Zustand：

```text
src/stores/pages/
├── use-login-page-store.ts
├── use-customer-portal-store.ts
├── use-merchant-console-store.ts
└── use-rider-app-store.ts
```

注意：前端状态只负责页面展示和交互缓存，订单、钱包、商家商品等真实业务数据必须以后端返回为准。

## AI 搜索

顾客端首页集成 AI 搜索：

- `src/components/AISearchBar.tsx`：搜索输入框，支持 500ms 防抖和回车搜索。
- `src/components/AISearchResults.tsx`：按商家逐行展示推荐结果，店铺图在左侧，推荐菜品横向滚动展示。
- `src/pages/CustomerPortal/HomeTab.tsx`：调用 `aiSearchIO({ query })` 并渲染搜索结果、加载骨架屏和错误重试。
- `src/objects/ai/` 与 `src/api/ai/`：与后端 `backend/src/ai/` 保持契约对应。

AI 返回商家与菜品 ID，前端结合当前目录数据补充店铺图、菜品图并跳转到对应店铺点餐页。

## UI 与样式

- `src/components/ui/` 存放 shadcn/ui 组件。
- `src/lib/utils.ts` 提供 `cn()`。
- `src/index.css` 定义 Tailwind CSS v4 与全局样式。

添加 shadcn/ui 组件：

```bash
npm run ui:add -- button
npm run ui:add -- card dialog sheet
```

## 开发约定

- 新增业务接口时，在 `src/api/<module>/` 增加一个对应 API 文件。
- 新增契约对象时，在 `src/objects/<module>/` 增加同名对象文件，并同步后端 `objects/`。
- 不在前端伪造真实业务结果；涉及下单、取消、充值、保存资料等操作必须调用后端 API。
- 会话本地只保存 JWT、账号、角色和登录时间，业务数据通过 API 刷新。

## 构建产物

生产构建输出到：

```text
dist/
```

运行：

```bash
npm run build
npm run preview
```
