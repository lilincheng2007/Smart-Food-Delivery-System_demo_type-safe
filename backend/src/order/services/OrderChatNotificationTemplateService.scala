package delivery.order.services

import delivery.order.objects.Order

object OrderChatNotificationTemplateService:
  def merchantOrderAccepted(order: Order): String =
    val prepText = order.estimatedPrepMinutes.map(minutes => s"预计备餐${minutes}分钟").getOrElse("商家正在安排备餐")
    val readyText = order.estimatedReadyAt.map(value => s"，预计${value}出餐").getOrElse("")
    s"【接单通知】商家已接单，$prepText$readyText。"

  def merchantPrepDelayed(order: Order, reason: String): String =
    val readyText = order.estimatedReadyAt.map(value => s"预计出餐时间调整为${value}。").getOrElse("")
    s"【备餐延迟】商家已更新备餐时间，原因：$reason。$readyText"

  def merchantPrepTimeout(order: Order): String =
    s"【备餐提醒】订单 ${order.id} 已超过预计出餐时间，商家正在加急处理。"

  def merchantOrderReady(order: Order): String =
    s"【出餐通知】您的订单 ${order.id} 已出餐，正在等待骑手接单配送，请留意后续配送动态。"

  def riderOrderDelivered(order: Order): String =
    s"【送达通知】您的订单 ${order.id} 已送达，请及时查收餐品。祝您用餐愉快！"
