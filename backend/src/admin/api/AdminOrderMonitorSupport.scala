package delivery.admin.api

import delivery.admin.objects.apiTypes.AdminOrderMonitorItem
import delivery.order.objects.Order
import delivery.domain.{OrderStatus, RefundStatus}

import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDate, LocalDateTime, ZoneId}
import scala.util.Try

object AdminOrderMonitorSupport:
  private val PlacedAtFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
  private val MerchantAcceptTimeoutMinutes = 10
  private val MerchantReadyTimeoutMinutes = 30
  private val pendingRefundStatuses = Set(RefundStatus.待审核, RefundStatus.待商家审核, RefundStatus.待管理员仲裁)
  private val abnormalRefundStatuses = Set(RefundStatus.商家已驳回, RefundStatus.已驳回)
  private val abnormalOrderStatuses = Set(OrderStatus.已取消, OrderStatus.已退款)

  def item(order: Order, reason: String, elapsedMinutes: Int): AdminOrderMonitorItem =
    AdminOrderMonitorItem(order, reason, math.max(0, elapsedMinutes))

  def placedDate(value: String, zoneId: ZoneId): Option[LocalDate] =
    parseLocalDateTime(value).map(_.toLocalDate).orElse(parseInstant(value).map(_.atZone(zoneId).toLocalDate))

  def elapsedMinutes(value: String, now: Instant, zoneId: ZoneId): Int =
    val startedAt = parseInstant(value).orElse(parseLocalDateTime(value).map(_.atZone(zoneId).toInstant))
    startedAt.map(instant => math.max(0, ((now.getEpochSecond - instant.getEpochSecond) / 60).toInt)).getOrElse(0)

  def isPendingRefund(order: Order): Boolean =
    order.refundStatus.exists(pendingRefundStatuses.contains)

  def isMerchantTimeout(order: Order, now: Instant, zoneId: ZoneId): Boolean =
    val elapsed = elapsedMinutes(order.placedAt, now, zoneId)
    (order.status == OrderStatus.待商家接单 && elapsed >= MerchantAcceptTimeoutMinutes) ||
      (order.status == OrderStatus.制作中 && elapsed >= MerchantReadyTimeoutMinutes)

  def merchantTimeoutReason(order: Order): String =
    if order.status == OrderStatus.待商家接单 then s"商家超过${MerchantAcceptTimeoutMinutes}分钟未接单"
    else s"商家超过${MerchantReadyTimeoutMinutes}分钟未出餐"

  def isAbnormal(order: Order): Boolean =
    abnormalOrderStatuses.contains(order.status) || order.refundStatus.exists(abnormalRefundStatuses.contains)

  def abnormalReason(order: Order): String =
    order.refundStatus match
      case Some(RefundStatus.商家已驳回) => "商家驳回退款，可能需要关注"
      case Some(RefundStatus.已驳回) => "退款仲裁已驳回"
      case _ if order.status == OrderStatus.已退款 => "订单已退款"
      case _ if order.status == OrderStatus.已取消 => "订单已取消"
      case _ => "订单状态异常"

  def refundReason(order: Order): String =
    order.refundStatus match
      case Some(RefundStatus.待管理员仲裁) => "退款待管理员仲裁"
      case Some(RefundStatus.待商家审核) | Some(RefundStatus.待审核) => "退款待商家处理"
      case _ => "退款待处理"

  def roundMoney(value: Double): Double =
    BigDecimal(value).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble

  private def parseLocalDateTime(value: String): Option[LocalDateTime] =
    Try(LocalDateTime.parse(value, PlacedAtFormatter)).toOption

  private def parseInstant(value: String): Option[Instant] =
    Try(Instant.parse(value)).toOption

end AdminOrderMonitorSupport
