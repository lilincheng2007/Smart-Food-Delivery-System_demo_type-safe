package delivery.order.tables

import cats.effect.IO
import delivery.merchant.objects.Product
import delivery.order.objects.*
import delivery.user.objects.CustomerProfile

object OrderDomainOps:

  def buildOrdersForCheckout(products: List[Product], customerProfile: CustomerProfile, lines: List[CheckoutLine]): IO[Either[String, (List[Order], Double)]] =
    if lines.isEmpty then IO.pure(Left("购物车为空"))
    else
      for
        nowMillis <- IO.realTime.map(_.toMillis)
        zoneId <- IO.delay(java.time.ZoneId.systemDefault())
      yield
        val grouped = lines.groupBy(_.merchantId)
        val now = java.time.Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDateTime
        val orderTimeText = f"${now.getYear}%04d-${now.getMonthValue}%02d-${now.getDayOfMonth}%02d ${now.getHour}%02d:${now.getMinute}%02d"

        val createdOrders = grouped.toList.zipWithIndex.flatMap { case ((merchantId, groupLines), idx) =>
          val items = groupLines.flatMap { line =>
            products.find(p => p.id == line.productId && p.merchantId == merchantId).map(p => OrderItem(p.id, p.name, p.price, line.quantity))
          }
          if items.isEmpty then None
          else
            Some(
              Order(
                s"o-$nowMillis-${idx + 1}",
                customerProfile.id,
                customerProfile.name,
                customerProfile.phone,
                merchantId,
                None,
                items,
                items.map(i => i.unitPrice * i.quantity).sum,
                customerProfile.defaultAddress,
                "制作中",
                orderTimeText
              )
            )
        }

        if createdOrders.isEmpty then Left("无法解析购物车商品")
        else
          val total = createdOrders.map(_.totalAmount).sum
          if customerProfile.walletBalance < total then Left("余额不足")
          else Right((createdOrders.reverse, total))

  def appendOrders(state: OrderServiceState, newOnes: List[Order]): OrderServiceState =
    state.copy(orders = newOnes.reverse ::: state.orders)

  def updateOrderStatus(state: OrderServiceState, orderId: String, nextStatus: String): Either[String, (OrderServiceState, Order)] =
    state.orders.find(_.id == orderId).toRight("未找到订单").map { order =>
      val updatedOrder = order.copy(status = nextStatus)
      (state.copy(orders = state.orders.map(item => if item.id == orderId then updatedOrder else item)), updatedOrder)
    }

  def replaceOrder(state: OrderServiceState, updatedOrder: Order): Either[String, (OrderServiceState, Order)] =
    state.orders.find(_.id == updatedOrder.id).toRight("未找到订单").map { _ =>
      (state.copy(orders = state.orders.map(item => if item.id == updatedOrder.id then updatedOrder else item)), updatedOrder)
    }

end OrderDomainOps
