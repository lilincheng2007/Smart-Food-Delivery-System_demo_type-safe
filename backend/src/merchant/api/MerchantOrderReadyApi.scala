package delivery.merchant.api

import cats.effect.IO
import delivery.merchant.tables.MerchantDomainOps
import delivery.order.tables.OrderDomainOps
import delivery.rider.tables.RiderDomainOps
import delivery.shared.api.ApiPlan
import delivery.shared.objects.{DeliveryState, OkResponse}
import delivery.shared.db.DeliveryStateOps
import delivery.user.tables.UserDomainOps
import org.typelevel.log4cats.slf4j.Slf4jLogger

object MerchantOrderReadyApi extends ApiPlan[MerchantOrderReadyApi.MerchantOrderReadyCommand, Either[String, MerchantOrderReadyApi.MerchantOrderReadySuccess]]:

  private def canFinishCooking(status: String): Boolean =
    // Legacy snapshots may still contain 待接单 orders from the old manual-accept flow.
    status == "制作中" || status == "待接单"

  final case class MerchantOrderReadyCommand(
      state: DeliveryState,
      username: String,
      orderId: String
  )

  private val logger = Slf4jLogger.getLogger[IO]

  override val name: String = "MerchantOrderReadyApi"

  override def plan(input: MerchantOrderReadyCommand): IO[Either[String, MerchantOrderReadySuccess]] =
    for
      _ <- logger.info(s"$name started, username=${input.username}, orderId=${input.orderId}")
      response <- input.state.order.orders.find(_.id == input.orderId) match
        case None => IO.pure(Left("未找到订单"))
        case Some(order) if !MerchantDomainOps.hasStoreAccess(input.state.merchant, input.username, order.merchantId) =>
          IO.pure(Left("无权操作该订单"))
        case Some(order) if !canFinishCooking(order.status) =>
          IO.pure(Left(s"当前状态不可执行出餐完成：${order.status}"))
        case Some(_) =>
          IO.pure(
            OrderDomainOps.updateOrderStatus(input.state.order, input.orderId, "待接单").map { case (nextOrder, updatedOrder) =>
              val nextMerchant = MerchantDomainOps.replaceOrderSnapshot(input.state.merchant, updatedOrder)
              val nextUser = UserDomainOps.replaceOrderSnapshot(input.state.user, updatedOrder)
              val nextRider = RiderDomainOps.replaceOrderSnapshot(input.state.rider, updatedOrder)
              val nextState =
                DeliveryStateOps.withOrderAndMerchantAndUserAndRiderState(input.state, nextUser, nextOrder, nextMerchant, nextRider)
              MerchantOrderReadySuccess(nextState, OkResponse(ok = true))
            }
          )
      _ <- logger.info(s"$name finished, success=${response.isRight}")
    yield response

  final case class MerchantOrderReadySuccess(nextState: DeliveryState, response: OkResponse)

end MerchantOrderReadyApi
