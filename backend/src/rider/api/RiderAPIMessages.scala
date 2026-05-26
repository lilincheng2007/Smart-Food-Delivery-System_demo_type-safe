package delivery.rider.api

import cats.effect.IO
import delivery.rider.objects.RiderMeResponse
import delivery.rider.utils.RiderApiSupport
import delivery.shared.api.{APIWithRoleMessage, HttpApiError}
import delivery.shared.db.DeliveryStateStore
import delivery.shared.objects.OkResponse

import javax.sql.DataSource

final case class RiderMeAPIMessage() extends APIWithRoleMessage[RiderMeResponse]:
  override def plan(ds: DataSource, username: String): IO[RiderMeResponse] =
    for
      state <- DeliveryStateStore.load(ds)
      response <- RiderMeApi.plan(RiderMeApi.RiderMeQuery(state, username))
      output <- response match
        case None => IO.raiseError(HttpApiError.NotFound(RiderApiSupport.riderNotFound.error))
        case Some(value) => IO.pure(value)
    yield output

final case class RiderGrabOrderAPIMessage(orderId: String) extends APIWithRoleMessage[OkResponse]:
  override def plan(ds: DataSource, username: String): IO[OkResponse] =
    for
      state <- DeliveryStateStore.load(ds)
      output <- RiderGrabOrderApi.plan(RiderGrabOrderApi.RiderGrabOrderCommand(state, username, orderId)) match
        case Left(msg) => IO.raiseError(HttpApiError.BadRequest(msg))
        case Right(value) => DeliveryStateStore.save(ds)(value.nextState).as(OkResponse(ok = true))
    yield output

final case class RiderUpdateOrderStatusAPIMessage(orderId: String) extends APIWithRoleMessage[OkResponse]:
  override def plan(ds: DataSource, username: String): IO[OkResponse] =
    for
      state <- DeliveryStateStore.load(ds)
      output <- RiderUpdateOrderStatusApi.plan(RiderUpdateOrderStatusApi.RiderUpdateOrderStatusCommand(state, username, orderId)) match
        case Left(msg) => IO.raiseError(HttpApiError.BadRequest(msg))
        case Right(value) => DeliveryStateStore.save(ds)(value.nextState).as(value.response)
    yield output
