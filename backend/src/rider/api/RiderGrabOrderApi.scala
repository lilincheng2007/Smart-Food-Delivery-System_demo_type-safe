package delivery.rider.api

import delivery.merchant.tables.MerchantDomainOps
import delivery.order.tables.OrderDomainOps
import delivery.rider.tables.RiderDomainOps
import delivery.shared.objects.DeliveryState
import delivery.shared.db.DeliveryStateOps
import delivery.user.tables.UserDomainOps

object RiderGrabOrderApi:

  final case class RiderGrabOrderCommand(
      state: DeliveryState,
      username: String,
      orderId: String
  )

  final case class RiderGrabOrderSuccess(nextState: DeliveryState)

  def plan(input: RiderGrabOrderCommand): Either[String, RiderGrabOrderSuccess] =
    input.state.order.orders.find(_.id == input.orderId).toRight("未找到订单").flatMap { order =>
      if order.status != "待接单" || order.riderId.nonEmpty then Left("订单已被其他骑手抢走")
      else
        input.state.rider.riderAccounts.find(_.username == input.username).toRight("未找到骑手账号").flatMap { account =>
          val riderId = account.profile.rider.id
          val claimedOrder = order.copy(riderId = Some(riderId), status = "配送中")
          RiderDomainOps.grabOrder(input.state.rider, input.username, claimedOrder).flatMap { riderState =>
            OrderDomainOps.replaceOrder(input.state.order, claimedOrder).map { case (nextOrder, updatedOrder) =>
              val nextMerchant = MerchantDomainOps.replaceOrderSnapshot(input.state.merchant, updatedOrder)
              val nextUser = UserDomainOps.replaceOrderSnapshot(input.state.user, updatedOrder)
              val nextState =
                DeliveryStateOps.withOrderAndMerchantAndUserAndRiderState(input.state, nextUser, nextOrder, nextMerchant, riderState)
              RiderGrabOrderSuccess(nextState)
            }
          }
        }
    }

end RiderGrabOrderApi
