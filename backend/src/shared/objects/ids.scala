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

object UserRole:
  def fromString(value: String): Option[UserRole] = values.find(_.toString == value)
end UserRole

enum MerchantCategory derives CanEqual:
  case 中餐, 西餐, 零售, 饮品甜点, 夜宵
end MerchantCategory

object MerchantCategory:
  def fromString(value: String): Option[MerchantCategory] = values.find(_.toString == value)
end MerchantCategory

enum RiderStatus derives CanEqual:
  case 空闲, 接单, 配送中
end RiderStatus

object RiderStatus:
  def fromString(value: String): Option[RiderStatus] = values.find(_.toString == value)
end RiderStatus

enum ServiceChannel derives CanEqual:
  case 在线, 电话
end ServiceChannel

object ServiceChannel:
  def fromString(value: String): Option[ServiceChannel] = values.find(_.toString == value)
end ServiceChannel

enum OrderStatus derives CanEqual:
  case 待商家接单, 制作中, 待骑手接单, 配送中, 已送达, 已完成, 已取消
end OrderStatus

object OrderStatus:
  def fromString(value: String): Option[OrderStatus] =
    value match
      case "待接单" => Some(待骑手接单)
      case _        => values.find(_.toString == value)

  val history: Set[OrderStatus] = Set(已送达, 已完成, 已取消)
end OrderStatus

enum ListingStatus derives CanEqual:
  case 上架, 下架
end ListingStatus

object ListingStatus:
  def fromString(value: String): Option[ListingStatus] = values.find(_.toString == value)
end ListingStatus

enum InventoryStatus derives CanEqual:
  case 充足, 紧张, 售罄
end InventoryStatus

object InventoryStatus:
  def fromString(value: String): Option[InventoryStatus] = values.find(_.toString == value)
end InventoryStatus
