package delivery.order.api

import delivery.order.services.{OrderCheckoutService, OrderChatNotificationTemplateService}
import cats.effect.IO
import cats.syntax.all.*
import delivery.order.objects.{Order, OrderChatMessage}
import delivery.order.objects.apiTypes.CustomerOrdersResponse
import delivery.order.tables.order.OrderTable
import delivery.order.tables.orderchat.OrderChatMessageTable
import delivery.order.utils.OrderApiSupport
import delivery.platform.api.{APIWithRoleMessage, HttpApiError}
import delivery.domain.OrderStatus
import delivery.user.tables.customerprofile.CustomerProfileTable

import java.sql.Connection
import java.time.{Instant, ZoneId}
import java.time.format.DateTimeFormatter
import java.util.UUID
import scala.util.Try

final case class CustomerOrdersAPIMessage() extends APIWithRoleMessage[CustomerOrdersResponse]:
  override def plan(connection: Connection, username: String): IO[CustomerOrdersResponse] =
    for
      account <- CustomerProfileTable.findByUsername(connection, username)
      output <- account match
        case None => IO.raiseError(HttpApiError.NotFound(OrderApiSupport.customerNotFound.error))
        case Some(value) =>
          for
            customerOrders <- OrderTable.listByCustomerId(connection, value.profile.id)
            refreshedOrders <- notifyPrepTimeouts(connection, customerOrders)
          yield CustomerOrdersResponse(
            pendingOrders = refreshedOrders.filterNot(order => OrderCheckoutService.isHistoryOrderStatus(order.status)),
            historyOrders = refreshedOrders.filter(order => OrderCheckoutService.isHistoryOrderStatus(order.status))
          )
    yield output

  private def notifyPrepTimeouts(connection: Connection, orders: List[Order]): IO[List[Order]] =
    orders.traverse { order =>
      if shouldNotifyPrepTimeout(order) then
        val notified = order.copy(prepTimeoutNotifiedAt = Some(Instant.now().toString))
        OrderTable.upsert(connection, notified) >> createPrepTimeoutMessage(connection, notified).as(notified)
      else IO.pure(order)
    }

  private def shouldNotifyPrepTimeout(order: Order): Boolean =
    order.status == OrderStatus.制作中 && order.prepTimeoutNotifiedAt.isEmpty && order.estimatedReadyAt.exists { value =>
      parseInstant(value).exists(_.isBefore(Instant.now()))
    }

  private def createPrepTimeoutMessage(connection: Connection, order: Order): IO[Unit] =
    OrderChatMessageTable.create(
      connection,
      OrderChatMessage(
        id = UUID.randomUUID().toString,
        orderId = order.id,
        senderRole = "merchant",
        peerRole = "customer",
        messageType = "text",
        content = OrderChatNotificationTemplateService.merchantPrepTimeout(order),
        createdAt = Instant.now().toString
      )
    ).void

  private def parseInstant(value: String): Option[Instant] =
    Try(Instant.parse(value)).toOption.orElse(
      Try(LocalDateTimeParser.parse(value).atZone(ZoneId.systemDefault()).toInstant).toOption
    )

  private object LocalDateTimeParser:
    private val Formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    def parse(value: String) = java.time.LocalDateTime.parse(value, Formatter)
