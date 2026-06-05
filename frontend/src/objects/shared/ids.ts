export type UserId = string
export type MerchantId = string
export type RiderId = string
export type ProductId = string
export type OrderId = string
export type VoucherId = string

export const UserRoles = {
  customer: 'customer',
  merchant: 'merchant',
  rider: 'rider',
  admin: 'admin',
} as const
export type UserRole = (typeof UserRoles)[keyof typeof UserRoles]

export const MerchantCategories = {
  chinese: '中餐',
  western: '西餐',
  retail: '零售',
  dessert: '饮品甜点',
  nightSnack: '夜宵',
} as const
export type MerchantCategory = (typeof MerchantCategories)[keyof typeof MerchantCategories]

export const RiderStatuses = {
  idle: '空闲',
  accepting: '接单',
  delivering: '配送中',
} as const
export type RiderStatus = (typeof RiderStatuses)[keyof typeof RiderStatuses]

export const ServiceChannels = {
  online: '在线',
  phone: '电话',
} as const
export type ServiceChannel = (typeof ServiceChannels)[keyof typeof ServiceChannels]

export const OrderStatuses = {
  waitingForMerchantAcceptance: '待商家接单',
  cooking: '制作中',
  waitingForRiderAcceptance: '待骑手接单',
  delivering: '配送中',
  delivered: '已送达',
  completed: '已完成',
  canceled: '已取消',
  refunded: '已退款',
} as const
export type OrderStatus = (typeof OrderStatuses)[keyof typeof OrderStatuses]

export const RefundStatuses = {
  pending: '待商家审核',
  legacyPending: '待审核',
  merchantRejected: '商家已驳回',
  adminPending: '待管理员仲裁',
  accepted: '已通过',
  rejected: '已驳回',
} as const
export type RefundStatus = (typeof RefundStatuses)[keyof typeof RefundStatuses]

export const ListingStatuses = {
  listed: '上架',
  unlisted: '下架',
} as const
export type ListingStatus = (typeof ListingStatuses)[keyof typeof ListingStatuses]

export const InventoryStatuses = {
  sufficient: '充足',
  low: '紧张',
  soldOut: '售罄',
} as const
export type InventoryStatus = (typeof InventoryStatuses)[keyof typeof InventoryStatuses]
