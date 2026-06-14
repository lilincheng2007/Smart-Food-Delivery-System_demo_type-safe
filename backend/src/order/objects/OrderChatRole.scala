package delivery.order.objects

enum OrderChatRole derives CanEqual:
  case customer, merchant, rider
end OrderChatRole

object OrderChatRole:
  def fromString(value: String): Option[OrderChatRole] =
    values.find(_.toString == value.trim)

end OrderChatRole
