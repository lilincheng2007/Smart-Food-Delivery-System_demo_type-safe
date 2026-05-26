package delivery.order.api

import cats.effect.IO
import delivery.merchant.tables.MerchantDomainOps
import delivery.order.objects.OrderCancelResponse
import delivery.order.tables.OrderDomainOps
import delivery.shared.api.ApiPlan
import delivery.shared.db.DeliveryStateOps
import delivery.shared.objects.DeliveryState
import delivery.user.tables.UserDomainOps
import org.typelevel.log4cats.slf4j.Slf4jLogger

object OrderCancelApi extends ApiPlan[OrderCancelApi.OrderCancelCommand, Either[String, OrderCancelApi.OrderCancelSuccess]]:

  final case class OrderCancelCommand(state: DeliveryState, username: String, orderId: String)

  private val logger = Slf4jLogger.getLogger[IO]

  override val name: String = "OrderCancelApi"

  override def plan(input: OrderCancelApi.OrderCancelCommand): IO[Either[String, OrderCancelSuccess]] =
    for
      _ <- logger.info(s"$name started, username=${input.username}, orderId=${input.orderId}")
      response = input.state.user.customerAccounts.find(_.username == input.username).toRight("未找到顾客账号").flatMap { account =>
        input.state.order.orders.find(_.id == input.orderId).toRight("未找到订单").flatMap { order =>
          if order.customerId != account.profile.id then Left("无权操作该订单")
          else if order.status == "已取消" then Left("订单已取消")
          else if order.status == "已送达" || order.status == "已完成" then Left("已完成订单不可取消")
          else if order.riderId.nonEmpty || order.status == "配送中" then Left("配送中订单不可取消")
          else
            val canceledOrder = order.copy(status = "已取消")
            OrderDomainOps.replaceOrder(input.state.order, canceledOrder).flatMap { case (nextOrder, updatedOrder) =>
              UserDomainOps.cancelCustomerOrder(input.state.user, input.username, updatedOrder).map { nextUser =>
                val nextMerchant = MerchantDomainOps.replaceOrderSnapshot(input.state.merchant, updatedOrder)
                val nextState = DeliveryStateOps.withOrderAndMerchantAndUserState(input.state, nextUser, nextOrder, nextMerchant)
                val wallet = nextUser.customerAccounts.find(_.username == input.username).map(_.profile.walletBalance).getOrElse(0d)
                OrderCancelSuccess(nextState, OrderCancelResponse(updatedOrder, wallet))
              }
            }
        }
      }
      _ <- logger.info(s"$name finished, success=${response.isRight}")
    yield response

  final case class OrderCancelSuccess(nextState: DeliveryState, response: OrderCancelResponse)

end OrderCancelApi
