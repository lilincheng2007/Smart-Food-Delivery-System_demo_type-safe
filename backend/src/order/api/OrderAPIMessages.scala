package delivery.order.api

import cats.effect.IO
import delivery.order.objects.{CheckoutLine, CheckoutRequest, CheckoutResponse, CustomerOrdersResponse, Order, OrderCancelResponse}
import delivery.order.utils.OrderApiSupport
import delivery.shared.api.{APIWithRoleMessage, HttpApiError}
import delivery.shared.db.DeliveryStateStore

import javax.sql.DataSource

final case class CustomerOrdersAPIMessage() extends APIWithRoleMessage[CustomerOrdersResponse]:
  override def plan(ds: DataSource, username: String): IO[CustomerOrdersResponse] =
    for
      state <- DeliveryStateStore.load(ds)
      response <- CustomerOrdersApi.plan(CustomerOrdersApi.CustomerOrdersQuery(state, username))
      output <- response match
        case None => IO.raiseError(HttpApiError.NotFound(OrderApiSupport.customerNotFound.error))
        case Some(value) => IO.pure(value)
    yield output

final case class OrderDetailAPIMessage(orderId: String) extends APIWithRoleMessage[Order]:
  override def plan(ds: DataSource, username: String): IO[Order] =
    for
      state <- DeliveryStateStore.load(ds)
      response <- OrderDetailApi.plan(OrderDetailApi.OrderDetailQuery(state, username, orderId))
      output <- response match
        case None => IO.raiseError(HttpApiError.NotFound("未找到订单"))
        case Some(value) => IO.pure(value)
    yield output

final case class OrderCancelAPIMessage(orderId: String) extends APIWithRoleMessage[OrderCancelResponse]:
  override def plan(ds: DataSource, username: String): IO[OrderCancelResponse] =
    for
      state <- DeliveryStateStore.load(ds)
      response <- OrderCancelApi.plan(OrderCancelApi.OrderCancelCommand(state, username, orderId))
      output <- response match
        case Left(msg) => IO.raiseError(HttpApiError.BadRequest(msg))
        case Right(value) => DeliveryStateStore.save(ds)(value.nextState).as(value.response)
    yield output

final case class CheckoutAPIMessage(
    lines: List[CheckoutLine],
    customerName: Option[String],
    customerPhone: Option[String],
    deliveryAddress: Option[String]
) extends APIWithRoleMessage[CheckoutResponse]:
  override def plan(ds: DataSource, username: String): IO[CheckoutResponse] =
    val body = CheckoutRequest(lines, customerName, customerPhone, deliveryAddress)
    for
      state <- DeliveryStateStore.load(ds)
      response <- CheckoutApi.plan(CheckoutApi.CheckoutCommand(state, username, body))
      output <- response match
        case Left(CheckoutApi.CheckoutFailure.CustomerMissing) => IO.raiseError(HttpApiError.NotFound(OrderApiSupport.customerNotFound.error))
        case Left(CheckoutApi.CheckoutFailure.Invalid(msg)) => IO.raiseError(HttpApiError.BadRequest(msg))
        case Right(value) => DeliveryStateStore.save(ds)(value.nextState).as(value.response)
    yield output
