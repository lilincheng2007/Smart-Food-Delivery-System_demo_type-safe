package delivery.order.objects

enum OrderChatMessageType derives CanEqual:
  case text, image
end OrderChatMessageType

object OrderChatMessageType:
  def fromString(value: String): Option[OrderChatMessageType] =
    values.find(_.toString == value.trim)

end OrderChatMessageType
