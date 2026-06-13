package delivery.order.api

import delivery.domain.OrderStatus

object OrderStatusTransitionRules:
  private val allowedTransitions: Map[(OrderStatus, OrderStatus), Set[String]] = Map(
    (OrderStatus.待商家接单, OrderStatus.制作中) -> Set("merchant"),
    (OrderStatus.待商家接单, OrderStatus.已取消) -> Set("customer", "merchant"),
    (OrderStatus.制作中, OrderStatus.待骑手接单) -> Set("merchant"),
    (OrderStatus.待骑手接单, OrderStatus.配送中) -> Set("rider"),
    (OrderStatus.配送中, OrderStatus.已送达) -> Set("rider"),
    (OrderStatus.已送达, OrderStatus.已完成) -> Set("customer"),
    (OrderStatus.已完成, OrderStatus.已退款) -> Set("merchant", "admin")
  )

  def canTransition(from: OrderStatus, to: OrderStatus, actorRole: String): Boolean =
    allowedTransitions.get((from, to)).exists(_.contains(actorRole))

  def actorLabel(actorRole: String): String =
    actorRole match
      case "customer" => "顾客"
      case "merchant" => "商家"
      case "rider"    => "骑手"
      case "admin"    => "管理员"
      case other       => other

  def invalidTransitionMessage(from: OrderStatus, to: OrderStatus, actorRole: String): String =
    s"当前状态不可由${actorLabel(actorRole)}从${from}变更为${to}"

end OrderStatusTransitionRules
