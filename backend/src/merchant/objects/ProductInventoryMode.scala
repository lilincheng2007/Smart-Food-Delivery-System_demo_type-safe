package delivery.merchant.objects

enum ProductInventoryMode derives CanEqual:
  case unlimited, finite, soldOut
end ProductInventoryMode

object ProductInventoryMode:
  def fromString(value: String): Option[ProductInventoryMode] =
    values.find(_.toString == value.trim)

  def normalize(value: String): ProductInventoryMode =
    fromString(value).getOrElse(finite)

end ProductInventoryMode
