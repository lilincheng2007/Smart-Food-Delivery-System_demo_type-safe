# Delivery Frontend

`frontend/` 是外卖平台 Web 前端，基于 Vite + React + TypeScript 构建。前端使用 `APIMessage` 封装访问后端 `POST /api/{apiName}` 网关；页面状态由 Zustand 管理；UI 使用 shadcn/ui、Radix UI、Tailwind CSS 和 lucide-react。

前端只负责页面展示、交互缓存和乐观前校验；订单、钱包、优惠、库存、通知已读、评价等真实业务结果必须由后端 API 返回。

## 技术栈

- Vite 8
- React 19
- TypeScript 5.9
- React Router 7
- Zustand 5
- shadcn/ui + Radix UI
- Tailwind CSS v4
- lucide-react
- date-fns

## 启动

```bash
cd frontend
npm install
npm run dev
```

默认 Vite dev server 会将 `/api` 代理到 `http://localhost:8787`。也可以在仓库根目录一键启动全栈：

```bash
npm install
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
├── apis/                    # APIMessage 封装，一 API 一文件
│   ├── admin/               # 管理员审核、退款仲裁、订单监控、平台优惠 API
│   ├── ai/                  # AI 搜索、周报、文案、经营建议 API
│   ├── merchant/            # 商家、目录、店铺、商品、图片、订单、营业时间、优惠 API
│   ├── order/               # 顾客订单、结算、聊天、退款、通知已读 API
│   ├── review/              # 评价、回复、投票、图片 API
│   ├── rider/               # 骑手工作台、抢单、送达、免责卡 API
│   ├── user/                # 登录注册、顾客资料、充值、优惠券舍弃 API
│   └── shared/              # APIMessage、sendAPI、client、TaskIO
├── components/              # 通用组件
│   ├── notifications/       # 全局通知中心展示、合并、已读辅助函数
│   └── ui/                  # shadcn/ui 组件
├── hooks/                   # useAppChrome、auth session 等 hooks
├── lib/                     # auth、计价、库存、套餐、时间线、媒体 URL 等工具
├── objects/                 # 与后端 case class 对应的 TS 契约对象
│   └── */apiTypes/          # API Request / Response 包装类型
├── pages/                   # 页面模块
├── stores/                  # Zustand store 与拆分 helper
├── router.tsx               # 路由定义
├── main.tsx                 # React 入口
└── index.css                # 全局样式与 Tailwind
```

## 页面路由

路由定义位于 `src/router.tsx`。

| 路由 | 页面 | 角色 |
|---|---|---|
| `/` | 重定向到 `/auth/login` | 游客 |
| `/auth/login` | 登录页 | 游客 |
| `/auth/register` | 注册页 | 游客 |
| `/delivery/customer` | 顾客门户 | 顾客 |
| `/delivery/customer/m/:merchantId` | 店内点餐页 | 顾客 |
| `/delivery/customer/checkout` | 顾客结算页 | 顾客 |
| `/delivery/merchant` | 商家控制台 | 商家 |
| `/delivery/rider` | 骑手工作台 | 骑手 |
| `/delivery/chat/:orderId` | 订单聊天 | 顾客/商家/骑手 |
| `/delivery/admin` | 管理员控制台 | 管理员 |
| `/delivery/admin/promotions` | 平台优惠管理 | 管理员 |

路由守卫位于 `src/components/RoleRouteGuards.tsx`，根据 `src/lib/auth-session.ts` 中保存的 JWT 与角色控制访问。

全局壳层由 `src/components/AppChromeProvider.tsx` 挂载，统一承载 toast、全局通知中心、反馈弹窗和预览检查桥接；`src/components/GlobalNotificationCenter.tsx` 通过 `notificationfeedapi` 拉取后端聚合通知并同步通知已读状态。

## 页面功能结构

### 登录与注册：`src/pages/Login/`、`src/pages/Register/`

- 登录页和注册页均通过 `components/` 拆分表单 UI，通过 `objects/` 保存页面局部常量和类型。
- 表单提交统一调用 `src/apis/user/` 下的一文件 API，成功后写入 `src/lib/auth-session.ts` 的 JWT、账号和角色信息。

### 顾客端：`src/pages/CustomerPortal/`

- `index.tsx`：顾客门户入口、数据启动和自动刷新。
- `components/HomeTab.tsx`：商家目录、AI 搜索入口。
- `components/CustomerMerchantOrderPage.tsx`：店内点餐、套餐选择、店铺购物车、商家评价列表/筛选/投票、AI 评价摘要、跳转结算。
- `components/CustomerCheckoutPage.tsx`：收货信息、优惠券、价格明细、钱包校验、提交结算。
- `components/CartTab.tsx`：购物车汇总和数量修改。
- `components/ProfileTab.tsx`：钱包、吃货等级、优惠券、待收货订单、历史订单、AI 周报。
- `components/OrderDetailDialog.tsx`：订单详情、状态时间线、价格明细、退款/评价入口。
- `components/OrderRefundDialog.tsx`：退款申请与凭证上传。
- `components/OrderReviewDialog.tsx`：订单评价、图片上传。
- `functions/`：价格展示、优惠券过滤、退款反馈、订单状态文案等纯函数。
- `objects/`：页面局部面板、视图常量和类型。

顾客端支持浏览商家与商品、AI 推荐、AI 评价摘要、普通菜品/套餐加购、库存限购约束、结算、订单时间线、聊天、通知、退款、评价、钱包、积分和优惠券。

### 商家端：`src/pages/MerchantConsole/`

- `index.tsx`：商家控制台入口、店铺选择和标签页装配。
- `components/ProductsTab.tsx`：商品/套餐创建编辑、上下架、库存、优惠、图片上传。
- `components/products/BundleGroupsEditor.tsx`：套餐类别和选项编辑器。
- `components/OrdersTab.tsx`：订单接单/拒单、备餐时间、延迟备餐、出餐完成。
- `components/orders/MerchantOrderCard.tsx`：商家订单卡片。
- `components/ProfileTab.tsx`：店铺资料、公告、营业状态、营业时间、图片。
- `components/BusinessDataTab.tsx`：经营数据和 AI 经营建议。
- `components/MerchantReviewsTab.tsx`：退款申请处理、顾客评价、商家回复与评价摘要。
- `components/MerchantAICopywritingCard.tsx`：AI 店铺描述和菜品描述生成。
- `functions/`：商品表单映射、订单展示文案、通用 helper。
- `objects/`：商品草稿、商家订单视图类型。

商家端支持多店铺、入驻申请、营业时间、商品/套餐、库存、优惠、订单处理、退款、评价回复、订单聊天和 AI 文案/经营建议。

### 骑手端：`src/pages/RiderApp/`

- `index.tsx`：骑手工作台入口。
- `components/RiderOverview.tsx`：骑手状态概览。
- `components/DispatchCard.tsx`：可抢订单。
- `components/TaskListCard.tsx`：配送中任务和状态更新。
- `components/EnergyTimeoutCard.tsx`：服务能量、超时免责卡。
- `components/SalaryCard.tsx`：薪资、配送统计、骑手评分和顾客评价列表。

骑手端支持可抢订单、抢单、配送状态更新、完成配送、薪资结算、能量值、超时、免责卡、骑手评分和评价展示。

### 管理员端：`src/pages/AdminConsole/`

- `index.tsx`：入驻审核、退款仲裁、订单监控和平台优惠摘要。
- `PlatformPromotionsPage.tsx`：平台优惠新增/编辑/启停。
- `components/MonitorOrderList.tsx`：异常订单、超时订单、待退款列表。
- `functions/`：状态 badge、日期和折叠展示格式化。

### 订单聊天：`src/pages/OrderChatPage/`

顾客、商家、骑手共用订单聊天页；不同角色通过 `OrderChatClient.ts` 分发到各自的一文件 API，支持文字、图片、系统消息和未读数。

## API 调用方式

前端 API 使用统一模式：

```text
APIMessage class -> sendAPI -> POST /api/{apiName}
```

关键文件：

- `src/apis/shared/APIMessage.ts`：前端 APIMessage 基类。
- `src/apis/shared/sendAPI.ts`：根据 `apiName` 发起请求。
- `src/apis/shared/client.ts`：注入 `Authorization: Bearer <token>`，统一处理错误和 401/403 会话清理。
- `src/apis/shared/TaskIO.ts`：任务式异步封装。

### API 模块摘要

#### `src/apis/user/`

| apiName | 用途 |
|---|---|
| `loginapi` | 登录 |
| `registerapi` | 注册 |
| `customermeapi` | 获取顾客档案 |
| `customerprofilepatchapi` | 保存顾客资料/收货联系人 |
| `customerrechargeapi` | 顾客钱包充值 |
| `customervoucherdiscardapi` | 舍弃过期优惠券 |

#### `src/apis/merchant/`

| apiName | 用途 |
|---|---|
| `catalogapi` | 获取商家目录 |
| `merchantmeapi` | 获取商家账号与店铺资料 |
| `merchantcreatestoreonboardingrequestapi` | 提交店铺入驻申请 |
| `merchantstoreapi` | 兼容旧店铺创建调用（建议迁移） |
| `merchantstoreonboardingrequestsapi` | 商家查看入驻申请 |
| `merchantstoredescriptionapi` / `merchantstoreannouncementapi` | 保存店铺描述/公告 |
| `merchantbusinesshoursapi` | 保存营业状态和营业时间 |
| `merchantstorepromotionsapi` | 保存店铺优惠 |
| `merchantstoreimageapi` / `merchantstoreimagefileapi` | 保存店铺图片 URL / 上传文件 |
| `merchantcreateproductapi` / `merchantproductapi` | 创建/更新商品或套餐 |
| `merchantproductimagefileapi` | 上传商品图片 |
| `merchantproductdescriptionsapi` | 保存 AI 菜品描述 |
| `merchantorderacceptapi` / `merchantorderrejectapi` | 接单/拒单 |
| `merchantorderprepdelayapi` | 主动延迟备餐 |
| `merchantorderreadyapi` | 商家完成出餐 |
| `merchantrefundrequestsapi` / `merchantrefundacceptapi` / `merchantrefundrejectapi` | 商家退款处理 |

#### `src/apis/order/`

| apiName | 用途 |
|---|---|
| `customerordersapi` | 顾客订单列表 |
| `orderdetailapi` | 订单详情 |
| `ordercancelapi` | 取消订单 |
| `ordercompleteapi` | 确认完成订单 |
| `checkoutapi` | 提交结算 |
| `orderrefundrequestapi` / `orderrefundappealapi` | 退款申请/申诉 |
| `customerrefundimagefileapi` | 上传退款凭证 |
| `customerorderimagefileapi` / `merchantorderimagefileapi` / `riderorderimagefileapi` | 聊天图片上传 |
| `*orderchatmessagesapi` / `*sendorderchatmessageapi` / `*orderchatunreadcountsapi` | 分角色订单聊天和未读数 |
| `notificationreadstatesapi` / `notificationmarkreadapi` / `notificationmarkallreadapi` | 通知已读状态 |

#### `src/apis/rider/`

| apiName | 用途 |
|---|---|
| `ridermeapi` | 骑手工作台数据 |
| `rideravailableordersapi` | 可抢订单 |
| `ridergraborderapi` | 抢单 |
| `riderupdateorderstatusapi` | 更新配送状态 |
| `riderredeemtimeoutcardapi` | 兑换超时免责卡 |
| `riderusetimeoutcardapi` | 使用超时免责卡 |

#### `src/apis/admin/`

| apiName | 用途 |
|---|---|
| `adminstoreonboardingrequestsapi` | 入驻申请列表 |
| `adminstoreonboardingacceptapi` / `adminstoreonboardingrejectapi` | 通过/拒绝入驻 |
| `adminrefundrequestsapi` / `adminrefundacceptapi` / `adminrefundrejectapi` | 退款仲裁 |
| `adminordermonitorapi` | 订单监控指标和异常列表 |
| `adminplatformpromotionsapi` / `adminplatformpromotionsupdateapi` | 平台优惠读取/保存 |

#### `src/apis/review/`

| apiName | 用途 |
|---|---|
| `merchantreviewsapi` | 商家评价列表与摘要 |
| `customersubmitorderreviewapi` | 顾客提交评价 |
| `customerreviewvoteapi` | 评价投票 |
| `merchantreviewreplyapi` | 商家回复评价 |
| `customerreviewimagefileapi` | 上传评价图片 |

#### `src/apis/ai/`

| apiName | 用途 |
|---|---|
| `aisearchapi` | 顾客 AI 搜索 |
| `aidietweeklyreportapi` | AI 饮食周报 |
| `aiorderprogressnarrativesapi` | AI 订单进度文案 |
| `aireviewsummaryapi` | AI 评价摘要 |
| `aimerchantbusinesssuggestionsapi` | AI 经营建议 |
| `aimerchantstoredescriptionapi` | AI 店铺描述 |
| `aimerchantproductdescriptionsapi` | AI 菜品描述 |

## 状态管理

页面级业务状态使用 Zustand：

```text
src/stores/
├── use-app-store.ts
└── pages/
    ├── use-login-page-store.ts
    ├── use-register-page-store.ts
    ├── use-customer-portal-store.ts      # 兼容入口
    ├── customerPortal/                   # 顾客 store 类型、初始状态、helper、收藏存储
    ├── use-merchant-console-store.ts     # 兼容入口
    ├── merchantConsole/                  # 商家 store 类型、初始状态、helper
    └── use-rider-app-store.ts
```

重点 store：

- `use-customer-portal-store.ts`
  - 保存顾客档案、商家目录、商品、购物车、钱包余额、订单、评价、AI 周报和 AI 订单文案。
  - 提供刷新、加购、改数量、再来一单、结算、取消、确认完成、退款、评价、充值、舍弃过期券、保存收货联系人等动作。
  - `refreshPortal` 做 in-flight 去重，并并行加载顾客档案、目录和订单。
- `use-merchant-console-store.ts`
  - 保存商家账号、店铺列表、当前店铺、标签页、退款/评价/经营数据。
  - 提供刷新、创建店铺、接单/拒单/出餐、创建/更新商品、保存营业时间、保存优惠、上传图片等动作。
- `use-rider-app-store.ts`
  - 保存骑手账号、可抢订单、配送状态、薪资和免责卡信息。
  - 提供刷新、抢单、更新配送状态、兑换免责卡、使用免责卡等动作。

## 关键工具库

- `src/lib/auth-session.ts`：会话存取，只保存 JWT、账号、角色和登录时间。
- `src/lib/bundles.ts`：套餐价格、选项汇总和加价计算。
- `src/lib/cart-inventory.ts`：购物车库存/售罄/限购计算。
- `src/lib/promotions.ts`：商家/平台优惠匹配与金额计算。
- `src/lib/order-price-breakdown.ts`：结构化结算明细解释器。
- `src/lib/order-timeline.ts`：订单状态时间线、预计送达和异常提示。
- `src/lib/api-media-url.ts`：API 返回媒体路径解析，处理 `/api/...` 与 `VITE_API_BASE`。

## 数据刷新策略

- 顾客门户 `src/pages/CustomerPortal/index.tsx`：首次进入执行 `bootstrap`；页面重新可见时立即刷新；自动刷新间隔为 15 秒；页面隐藏时跳过刷新；AI 订单进度文案按天缓存。
- 商家控制台 `src/pages/MerchantConsole/index.tsx`：约 5 秒刷新一次商家账号、店铺、订单等数据。
- 骑手工作台 `src/pages/RiderApp/index.tsx`：约 5 秒刷新一次骑手资料、可抢订单和配送任务。
- 全局通知中心 `src/components/GlobalNotificationCenter.tsx`：约 8 秒轮询不同角色的业务事件，并通过通知已读 API 持久化已读状态。
- 订单聊天未读数 `src/hooks/useOrderChatUnreadCounts.ts`：约 5 秒刷新一次当前角色的订单聊天未读数。

## UI 与样式

- `src/components/ui/` 存放 shadcn/ui 组件。
- `src/lib/utils.ts` 提供 `cn()`。
- `src/index.css` 定义 Tailwind CSS v4、主题变量和全局样式。
- 图标统一使用 `lucide-react`。

添加 shadcn/ui 组件：

```bash
npm run ui:add -- button
npm run ui:add -- card dialog sheet
```

## 开发约定

- 新增业务接口时，在 `src/apis/<module>/` 增加一个对应 `XxxAPI.ts` 文件，并保持后端 `XxxAPIMessage.scala` 一一对应。
- 新增领域对象时，在 `src/objects/<module>/` 增加同名对象文件；新增请求/响应包装类型时放入 `src/objects/<module>/apiTypes/`，并同步后端 `objects` / `objects/apiTypes`。
- 页面按 sample 风格拆分：入口 `index.tsx` 做装配，局部 UI 放 `components/`，纯函数放 `functions/`，局部常量/类型放 `objects/`。
- 不在前端伪造真实业务结果；涉及下单、取消、充值、保存资料、抢单、出餐、退款、评价、通知已读等操作必须调用后端 API。
- 会话本地只保存 JWT、账号、角色和登录时间，业务数据通过 API 刷新。
- 避免 `as any`、`ts-ignore`、`dangerouslySetInnerHTML`、`eval` 等类型安全逃逸口。

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
