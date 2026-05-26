package delivery.rider.api

import delivery.merchant.tables.MerchantDomainOps
import delivery.order.tables.OrderDomainOps
import delivery.rider.tables.RiderDomainOps
import delivery.shared.objects.OkResponse
import delivery.shared.objects.DeliveryState
import delivery.shared.db.DeliveryStateOps
import delivery.user.tables.UserDomainOps

object RiderUpdateOrderStatusApi:

  final case class RiderUpdateOrderStatusCommand(
      state: DeliveryState,
      username: String,
      orderId: String
  )

  final case class RiderUpdateOrderStatusSuccess(
      nextState: DeliveryState,
      response: OkResponse
  )

  def plan(input: RiderUpdateOrderStatusCommand): Either[String, RiderUpdateOrderStatusSuccess] =
    input.state.rider.riderAccounts.find(_.username == input.username).toRight("未找到骑手账号").flatMap { account =>
      val riderId = account.profile.rider.id
      input.state.order.orders.find(_.id == input.orderId).toRight("未找到订单").flatMap { order =>
        if order.riderId != Some(riderId) then Left("无权操作该订单")
        else if order.status != "配送中" then Left(s"当前状态不可执行更新状态：${order.status}")
        else
          val completedOrder = order.copy(status = "已送达")
          RiderDomainOps.completeOrder(input.state.rider, input.username, completedOrder).flatMap { riderState =>
            OrderDomainOps.replaceOrder(input.state.order, completedOrder).map { case (nextOrder, updatedOrder) =>
              val nextMerchant = MerchantDomainOps.replaceOrderSnapshot(input.state.merchant, updatedOrder)
              val nextUser = UserDomainOps.replaceOrderSnapshot(input.state.user, updatedOrder)
              val nextState =
                DeliveryStateOps.withOrderAndMerchantAndUserAndRiderState(input.state, nextUser, nextOrder, nextMerchant, riderState)
              RiderUpdateOrderStatusSuccess(nextState, OkResponse(ok = true))
            }
          }
      }
    }

end RiderUpdateOrderStatusApi
