package delivery.admin.api

import cats.effect.IO
import delivery.admin.objects.apiTypes.AdminOrderMonitorResponse
import delivery.admin.services.AdminOrderMonitorService
import delivery.order.tables.order.OrderTable
import delivery.rider.tables.riderassignment.RiderAssignmentTable
import delivery.rider.utils.RiderTimeoutPolicy
import delivery.platform.api.APIWithRoleMessage
import delivery.domain.OrderStatus

import java.sql.Connection
import java.time.{Instant, LocalDate, ZoneId}

final case class AdminOrderMonitorAPIMessage() extends APIWithRoleMessage[AdminOrderMonitorResponse]:
  override def plan(connection: Connection, username: String): IO[AdminOrderMonitorResponse] =
    for
      orders <- OrderTable.list(connection)
      assignments <- RiderAssignmentTable.listAll(connection)
      now <- IO.realTime.map(duration => Instant.ofEpochMilli(duration.toMillis))
      zoneId <- IO.delay(ZoneId.systemDefault())
    yield
      val today = LocalDate.now(zoneId)
      val todayOrders = orders.filter(order => AdminOrderMonitorService.placedDate(order.placedAt, zoneId).contains(today))
      val todayTurnover = AdminOrderMonitorService.roundMoney(todayOrders.filterNot(order => Set(OrderStatus.已取消, OrderStatus.已退款).contains(order.status)).map(_.payableAmount).sum)
      val ordersById = orders.map(order => order.id -> order).toMap
      val pendingRefunds = orders
        .filter(order => AdminOrderMonitorService.isPendingRefund(order))
        .map(order => AdminOrderMonitorService.item(order, AdminOrderMonitorService.refundReason(order), AdminOrderMonitorService.elapsedMinutes(order.refundRequestedAt.getOrElse(order.placedAt), now, zoneId)))
      val abnormalOrders = orders
        .filter(AdminOrderMonitorService.isAbnormal)
        .map(order => AdminOrderMonitorService.item(order, AdminOrderMonitorService.abnormalReason(order), AdminOrderMonitorService.elapsedMinutes(order.placedAt, now, zoneId)))
      val merchantTimeoutOrders = orders
        .filter(AdminOrderMonitorService.isMerchantTimeout(_, now, zoneId))
        .map(order => AdminOrderMonitorService.item(order, AdminOrderMonitorService.merchantTimeoutReason(order), AdminOrderMonitorService.elapsedMinutes(order.placedAt, now, zoneId)))
      val riderTimeoutOrders = assignments.flatMap { assignment =>
        val activeTimeout = assignment.completedAt.isEmpty && now.isAfter(assignment.deadlineAt.getOrElse(RiderTimeoutPolicy.deadlineAt(assignment.assignedAt)))
        val finishedTimeout = assignment.wasTimeout && !assignment.timeoutExempted
        Option.when(activeTimeout || finishedTimeout) {
          ordersById.get(assignment.orderId).map { order =>
            val deadline = assignment.deadlineAt.getOrElse(RiderTimeoutPolicy.deadlineAt(assignment.assignedAt))
            val overtimeMinutes = math.max(1, ((assignment.completedAt.getOrElse(now).getEpochSecond - deadline.getEpochSecond) / 60).toInt)
            AdminOrderMonitorService.item(order, if activeTimeout then "骑手配送已超过预计送达时间" else "骑手历史配送超时", overtimeMinutes)
          }
        }.flatten
      }
      AdminOrderMonitorResponse(
        todayOrderCount = todayOrders.size,
        todayTurnover = todayTurnover,
        pendingRefunds = pendingRefunds,
        abnormalOrders = abnormalOrders,
        merchantTimeoutOrders = merchantTimeoutOrders,
        riderTimeoutOrders = riderTimeoutOrders
      )
