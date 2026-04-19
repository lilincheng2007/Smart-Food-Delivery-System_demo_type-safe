import { Bike, BriefcaseBusiness, ClipboardList, Store, UserRound } from 'lucide-react'

import type { PageToolEvent } from '@/lib/mock-types'

export const dashboardEntries = [
  {
    title: '顾客端',
    description: '浏览商品、下单与支付、查看配送轨迹',
    path: '/delivery/customer',
    icon: UserRound,
  },
  {
    title: '商家端',
    description: '管理商品、处理订单、查看营业概况',
    path: '/delivery/merchant',
    icon: Store,
  },
  {
    title: '骑手端',
    description: '抢单/派单、查看导航、更新配送状态',
    path: '/delivery/rider',
    icon: Bike,
  },
  {
    title: '订单中心',
    description: '统一处理订单状态流转与派单策略',
    path: '/delivery/orders',
    icon: ClipboardList,
  },
  {
    title: '运营与客服后台',
    description: '商家审核、活动发放、投诉处理与平台治理',
    path: '/delivery/admin',
    icon: BriefcaseBusiness,
  },
] as const

export const pageEvents: PageToolEvent[] = [
  {
    id: 'push-status',
    label: '发送状态推送',
    description: '订单状态变更后的通知能力由后端消息服务与 API 提供。',
  },
  {
    id: 'daily-settlement',
    label: '日结对账',
    description: '财务结算任务由后端批处理与 API 提供。',
  },
]
