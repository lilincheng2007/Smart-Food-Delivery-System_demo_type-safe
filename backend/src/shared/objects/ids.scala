package delivery.shared.objects

type UserId = String
type MerchantId = String
type RiderId = String
type ProductId = String
type OrderId = String
type VoucherId = String

enum UserRole derives CanEqual:
  case customer, merchant, rider
end UserRole

enum MerchantCategory derives CanEqual:
  case 中餐, 西餐, 零售, 饮品甜点, 夜宵
end MerchantCategory

enum RiderStatus derives CanEqual:
  case 空闲, 接单, 配送中
end RiderStatus

enum ServiceChannel derives CanEqual:
  case 在线, 电话
end ServiceChannel

enum OrderStatus derives CanEqual:
  case 待接单, 制作中, 配送中, 已送达, 已完成, 已取消
end OrderStatus
