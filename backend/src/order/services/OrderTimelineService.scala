package delivery.order.services

import delivery.order.objects.{Order, OrderTimelineEvent}
import delivery.domain.OrderStatus

object OrderTimelineService:
  def appendTransitionEvents(order: Order, previousStatus: OrderStatus, targetStatus: OrderStatus, actorRole: String, occurredAt: String): Order =
    val events = transitionEvents(previousStatus, targetStatus, actorRole, occurredAt)
    if events.isEmpty then order
    else
      val existingKeys = order.statusTimeline.map(_.key).toSet
      order.copy(statusTimeline = order.statusTimeline ++ events.filterNot(event => existingKeys.contains(event.key)))

  def transitionEvents(previousStatus: OrderStatus, targetStatus: OrderStatus, actorRole: String, occurredAt: String): List[OrderTimelineEvent] =
    (previousStatus, targetStatus, actorRole) match
      case (OrderStatus.待商家接单, OrderStatus.制作中, "merchant") =>
        List(OrderTimelineEvent("merchantAccepted", "商家已接单", occurredAt, Some("商家已确认订单，开始备餐")))
      case (OrderStatus.制作中, OrderStatus.待骑手接单, "merchant") =>
        List(OrderTimelineEvent("merchantReady", "商家出餐", occurredAt, Some("餐品已制作完成，等待骑手接单")))
      case (OrderStatus.待骑手接单, OrderStatus.配送中, "rider") =>
        List(
          OrderTimelineEvent("riderAccepted", "骑手已接单", occurredAt, Some("骑手已接单，准备前往商家取餐")),
          OrderTimelineEvent("riderPickedUp", "骑手取餐", occurredAt, Some("骑手已取餐，正在配送途中")),
          OrderTimelineEvent("delivering", "配送中", occurredAt, Some("餐品正在配送途中"))
        )
      case (OrderStatus.配送中, OrderStatus.已送达, "rider") =>
        List(OrderTimelineEvent("delivered", "已送达", occurredAt, Some("餐品已送达，请及时查收")))
      case (OrderStatus.已送达, OrderStatus.已完成, "customer") =>
        List(OrderTimelineEvent("completed", "已完成/可评价", occurredAt, Some("订单已完成，可以评价本次体验")))
      case (OrderStatus.待商家接单, OrderStatus.已取消, _) =>
        List(OrderTimelineEvent("canceled", "已取消", occurredAt, Some("订单已取消")))
      case (OrderStatus.已完成, OrderStatus.已退款, _) =>
        List(OrderTimelineEvent("refunded", "已退款", occurredAt, Some("订单已退款")))
      case _ => Nil

end OrderTimelineService
