package delivery.order.api

import cats.effect.IO
import delivery.merchant.objects.Product
import delivery.order.objects.{CheckoutLine, Order, OrderItem}
import delivery.shared.objects.{OrderStatus, Voucher, VoucherId}
import delivery.user.objects.CustomerProfile

import java.time.LocalDate
import scala.util.Try

object OrderAPIMessageSupport:

  private val FoodieLevelPoints = 200
  private val DefaultVoucherDiscount = 10.0
  private val DefaultVoucherMinSpend = 30.0
  private val DefaultVoucherExpiresAt = "2026-12-31"

  final case class CheckoutBuild(
      orders: List[Order],
      originalAmount: Double,
      discountAmount: Double,
      payableAmount: Double,
      usedVoucher: Option[Voucher]
  )

  def isHistoryOrderStatus(status: OrderStatus): Boolean =
    OrderStatus.history.contains(status)

  def roundMoney(value: Double): Double =
    BigDecimal(value).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble

  def rewardVoucher(id: VoucherId): Voucher =
    Voucher(id, "满30减10", DefaultVoucherDiscount, DefaultVoucherMinSpend, DefaultVoucherExpiresAt, 1)

  private def isVoucherExpired(voucher: Voucher): Boolean =
    Try(LocalDate.parse(voucher.expiresAt)).toOption.forall(_.isBefore(LocalDate.now()))

  private def validateVoucher(profile: CustomerProfile, voucherId: Option[VoucherId], originalAmount: Double): Either[String, Option[Voucher]] =
    voucherId match
      case None => Right(None)
      case Some(id) =>
        profile.vouchers.find(_.id == id) match
          case None => Left("优惠券不属于当前顾客")
          case Some(voucher) if voucher.remainingCount <= 0 => Left("优惠券已使用完")
          case Some(voucher) if isVoucherExpired(voucher) => Left("优惠券已过期")
          case Some(voucher) if originalAmount < voucher.minSpend => Left(s"未满足优惠券门槛：满${voucher.minSpend}元可用")
          case Some(voucher) => Right(Some(voucher))

  def consumeVoucher(profile: CustomerProfile, voucher: Voucher): List[Voucher] =
    profile.vouchers.map { current =>
      if current.id == voucher.id then current.copy(remainingCount = math.max(0, current.remainingCount - 1))
      else current
    }

  def levelOf(points: Int): Int =
    1 + math.max(0, points) / FoodieLevelPoints

  def buildOrdersForCheckout(products: List[Product], customerProfile: CustomerProfile, lines: List[CheckoutLine], voucherId: Option[VoucherId]): IO[Either[String, CheckoutBuild]] =
    if lines.isEmpty then IO.pure(Left("购物车为空"))
    else
      for
        nowMillis <- IO.realTime.map(_.toMillis)
        zoneId <- IO.delay(java.time.ZoneId.systemDefault())
      yield
        val grouped = lines.groupBy(_.merchantId).toList
        val rawOrders = grouped.flatMap { case (merchantId, groupLines) =>
          val items = groupLines.flatMap { line =>
            products.find(p => p.id == line.productId && p.merchantId == merchantId).map(p => OrderItem(p.id, p.name, p.price, line.quantity))
          }
          if items.isEmpty then None
          else Some((merchantId, items, roundMoney(items.map(i => i.unitPrice * i.quantity).sum)))
        }

        if rawOrders.isEmpty then Left("无法解析购物车商品")
        else
          val originalAmount = roundMoney(rawOrders.map(_._3).sum)
          validateVoucher(customerProfile, voucherId, originalAmount).flatMap { usedVoucher =>
            val discountAmount = usedVoucher.map(voucher => math.min(voucher.discountAmount, originalAmount)).getOrElse(0.0)
            val payableAmount = roundMoney(originalAmount - discountAmount)
            if customerProfile.walletBalance < payableAmount then Left("余额不足")
            else
              val now = java.time.Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDateTime
              val orderTimeText = f"${now.getYear}%04d-${now.getMonthValue}%02d-${now.getDayOfMonth}%02d ${now.getHour}%02d:${now.getMinute}%02d"
              val orders = rawOrders.zipWithIndex.map { case ((merchantId, items, orderOriginalAmount), idx) =>
                val orderDiscount =
                  if idx == rawOrders.size - 1 then roundMoney(discountAmount - rawOrders.take(idx).map { case (_, _, amount) => roundMoney(discountAmount * amount / originalAmount) }.sum)
                  else roundMoney(discountAmount * orderOriginalAmount / originalAmount)
                val orderPayable = roundMoney(orderOriginalAmount - orderDiscount)
                Order(
                  id = s"o-$nowMillis-${idx + 1}",
                  customerId = customerProfile.id,
                  customerName = customerProfile.name,
                  customerPhone = customerProfile.phone,
                  merchantId = merchantId,
                  riderId = None,
                  items = items,
                  totalAmount = orderPayable,
                  deliveryAddress = customerProfile.defaultAddress,
                  status = OrderStatus.待商家接单,
                  placedAt = orderTimeText,
                  originalAmount = orderOriginalAmount,
                  discountAmount = orderDiscount,
                  payableAmount = orderPayable,
                  usedVoucher = usedVoucher,
                  pointsAwarded = 0
                )
              }
              Right(CheckoutBuild(orders.reverse, originalAmount, discountAmount, payableAmount, usedVoucher))
          }

end OrderAPIMessageSupport
