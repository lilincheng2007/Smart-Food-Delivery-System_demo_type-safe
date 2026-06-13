package delivery.order.validators

import delivery.domain.OrderStatus

object OrderStatusTransitionValidator:
  val CustomerActor = "customer"
  val MerchantActor = "merchant"
  val RiderActor = "rider"
  val AdminActor = "admin"

  private val allowedTransitions: Map[(OrderStatus, OrderStatus), Set[String]] = Map(
    (OrderStatus.待商家接单, OrderStatus.制作中) -> Set(MerchantActor),
    (OrderStatus.待商家接单, OrderStatus.已取消) -> Set(CustomerActor, MerchantActor),
    (OrderStatus.制作中, OrderStatus.待骑手接单) -> Set(MerchantActor),
    (OrderStatus.待骑手接单, OrderStatus.配送中) -> Set(RiderActor),
    (OrderStatus.配送中, OrderStatus.已送达) -> Set(RiderActor),
    (OrderStatus.已送达, OrderStatus.已完成) -> Set(CustomerActor),
    (OrderStatus.已完成, OrderStatus.已退款) -> Set(MerchantActor, AdminActor)
  )

  def canTransition(from: OrderStatus, to: OrderStatus, actorRole: String): Boolean =
    allowedTransitions.get((from, to)).exists(_.contains(actorRole))

  def actorLabel(actorRole: String): String =
    actorRole match
      case CustomerActor => "顾客"
      case MerchantActor => "商家"
      case RiderActor    => "骑手"
      case AdminActor    => "管理员"
      case other         => other

  def invalidTransitionMessage(from: OrderStatus, to: OrderStatus, actorRole: String): String =
    s"当前状态不可由${actorLabel(actorRole)}从${from}变更为${to}"

end OrderStatusTransitionValidator
