package delivery.order.api

import cats.effect.IO
import cats.syntax.all.*
import delivery.merchant.objects.Product
import delivery.merchant.tables.catalogproduct.CatalogProductTable
import delivery.order.objects.{CheckoutLine, CheckoutRequest, CheckoutResponse, CustomerOrdersResponse, Order, OrderCancelResponse}
import delivery.order.tables.checkoutrequest.CheckoutRequestTable
import delivery.order.tables.order.OrderTable
import delivery.order.utils.OrderApiSupport
import delivery.rider.tables.riderassignment.RiderAssignmentTable
import delivery.shared.api.{APIWithRoleMessage, HttpApiError}
import delivery.shared.objects.{OrderId, OrderStatus, Voucher, VoucherId}
import delivery.user.objects.CustomerProfile
import delivery.user.tables.customerprofile.CustomerProfileTable

import java.sql.Connection
import java.time.LocalDate
import scala.util.Try

private val FoodieLevelPoints = 200
private val DefaultVoucherDiscount = 10.0
private val DefaultVoucherMinSpend = 30.0
private val DefaultVoucherExpiresAt = "2026-12-31"

private final case class CheckoutBuild(
    orders: List[Order],
    originalAmount: Double,
    discountAmount: Double,
    payableAmount: Double,
    usedVoucher: Option[Voucher]
)

private def isHistoryOrderStatus(status: OrderStatus): Boolean =
  OrderStatus.history.contains(status)

private def roundMoney(value: Double): Double =
  BigDecimal(value).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble

private def rewardVoucher(id: VoucherId): Voucher =
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

private def consumeVoucher(profile: CustomerProfile, voucher: Voucher): List[Voucher] =
  profile.vouchers.map { current =>
    if current.id == voucher.id then current.copy(remainingCount = math.max(0, current.remainingCount - 1))
    else current
  }

private def levelOf(points: Int): Int =
  1 + math.max(0, points) / FoodieLevelPoints

private def buildOrdersForCheckout(products: List[Product], customerProfile: CustomerProfile, lines: List[CheckoutLine], voucherId: Option[VoucherId]): IO[Either[String, CheckoutBuild]] =
  if lines.isEmpty then IO.pure(Left("购物车为空"))
  else
    for
      nowMillis <- IO.realTime.map(_.toMillis)
      zoneId <- IO.delay(java.time.ZoneId.systemDefault())
    yield
      val grouped = lines.groupBy(_.merchantId).toList
      val rawOrders = grouped.flatMap { case (merchantId, groupLines) =>
        val items = groupLines.flatMap { line =>
          products.find(p => p.id == line.productId && p.merchantId == merchantId).map(p => delivery.order.objects.OrderItem(p.id, p.name, p.price, line.quantity))
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
                status = OrderStatus.制作中,
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

final case class CustomerOrdersAPIMessage() extends APIWithRoleMessage[CustomerOrdersResponse]:
  override def plan(connection: Connection, username: String): IO[CustomerOrdersResponse] =
    for
      account <- CustomerProfileTable.findByUsername(connection, username)
      output <- account match
        case None => IO.raiseError(HttpApiError.NotFound(OrderApiSupport.customerNotFound.error))
        case Some(value) =>
          OrderTable.listByCustomerId(connection, value.profile.id).map { customerOrders =>
            CustomerOrdersResponse(
              pendingOrders = customerOrders.filterNot(order => isHistoryOrderStatus(order.status)),
              historyOrders = customerOrders.filter(order => isHistoryOrderStatus(order.status))
            )
          }
    yield output

final case class OrderDetailAPIMessage(orderId: OrderId) extends APIWithRoleMessage[Order]:
  override def plan(connection: Connection, username: String): IO[Order] =
    for
      account <- CustomerProfileTable.findByUsername(connection, username)
      order <- OrderTable.findById(connection, orderId)
      output <- (account, order) match
        case (None, _) => IO.raiseError(HttpApiError.NotFound(OrderApiSupport.customerNotFound.error))
        case (Some(value), Some(found)) if found.customerId == value.profile.id => IO.pure(found)
        case (Some(value), None) =>
          value.profile.pendingOrders
            .find(_.id == orderId)
            .orElse(value.profile.historyOrders.find(_.id == orderId)) match
            case Some(found) => IO.pure(found)
            case None        => IO.raiseError(HttpApiError.NotFound("未找到订单"))
        case (Some(_), Some(_)) => IO.raiseError(HttpApiError.NotFound("未找到订单"))
    yield output

final case class OrderCancelAPIMessage(orderId: OrderId) extends APIWithRoleMessage[OrderCancelResponse]:
  override def plan(connection: Connection, username: String): IO[OrderCancelResponse] =
    for
      account <- CustomerProfileTable.findByUsername(connection, username).flatMap {
        case Some(value) => IO.pure(value)
        case None        => IO.raiseError(HttpApiError.BadRequest("未找到顾客账号"))
      }
      order <- OrderTable.findById(connection, orderId).flatMap {
        case Some(value) => IO.pure(value)
        case None        => IO.raiseError(HttpApiError.BadRequest("未找到订单"))
      }
      _ <-
        if order.customerId != account.profile.id then IO.raiseError(HttpApiError.BadRequest("无权操作该订单"))
        else if order.status == OrderStatus.已取消 then IO.raiseError(HttpApiError.BadRequest("订单已取消"))
        else if order.status == OrderStatus.已送达 || order.status == OrderStatus.已完成 then IO.raiseError(HttpApiError.BadRequest("已完成订单不可取消"))
        else if order.riderId.nonEmpty || order.status == OrderStatus.配送中 then IO.raiseError(HttpApiError.BadRequest("配送中订单不可取消"))
        else IO.unit
      refundAmount = if order.payableAmount > 0 then order.payableAmount else order.totalAmount
      canceledOrder = order.copy(status = OrderStatus.已取消)
      nextAccount = account.copy(profile = account.profile.copy(walletBalance = roundMoney(account.profile.walletBalance + refundAmount)))
      _ <- OrderTable.upsert(connection, canceledOrder)
      _ <- CustomerProfileTable.upsert(connection, nextAccount)
    yield OrderCancelResponse(canceledOrder, nextAccount.profile.walletBalance)

final case class OrderCompleteAPIMessage(orderId: OrderId) extends APIWithRoleMessage[Order]:
  override def plan(connection: Connection, username: String): IO[Order] =
    for
      account <- CustomerProfileTable.findByUsername(connection, username).flatMap {
        case Some(value) => IO.pure(value)
        case None        => IO.raiseError(HttpApiError.BadRequest("未找到顾客账号"))
      }
      order <- OrderTable.findById(connection, orderId).flatMap {
        case Some(value) => IO.pure(value)
        case None        => IO.raiseError(HttpApiError.BadRequest("未找到订单"))
      }
      _ <-
        if order.customerId != account.profile.id then IO.raiseError(HttpApiError.BadRequest("无权操作该订单"))
        else if order.status == OrderStatus.已完成 then IO.raiseError(HttpApiError.BadRequest("订单已完成"))
        else if order.status != OrderStatus.已送达 then IO.raiseError(HttpApiError.BadRequest(s"当前状态不可确认完成：${order.status}"))
        else IO.unit
      earnedPoints = math.floor(if order.payableAmount > 0 then order.payableAmount else order.totalAmount).toInt
      currentPoints = account.profile.foodiePoints
      currentLevel = math.max(account.profile.foodieLevel, levelOf(currentPoints))
      nextPoints = currentPoints + earnedPoints
      nextLevel = levelOf(nextPoints)
      rewardCount = math.max(0, nextLevel - currentLevel)
      levelRewards = List.tabulate(rewardCount)(idx => rewardVoucher(s"v-level-${account.profile.id}-${order.id}-${idx + 1}"))
      completedOrder = order.copy(status = OrderStatus.已完成, pointsAwarded = earnedPoints)
      nextAccount = account.copy(profile = account.profile.copy(
        pendingOrders = account.profile.pendingOrders.filterNot(_.id == orderId),
        historyOrders = completedOrder :: account.profile.historyOrders.filterNot(_.id == orderId),
        foodiePoints = nextPoints,
        foodieLevel = nextLevel,
        vouchers = levelRewards ::: account.profile.vouchers
      ))
      _ <- OrderTable.upsert(connection, completedOrder)
      _ <- order.riderId match
        case Some(riderId) => RiderAssignmentTable.upsert(connection, riderId, completedOrder.id, completedOrder.status)
        case None          => IO.unit
      _ <- CustomerProfileTable.upsert(connection, nextAccount)
    yield completedOrder

final case class CheckoutAPIMessage(
    lines: List[CheckoutLine],
    customerName: Option[String],
    customerPhone: Option[String],
    deliveryAddress: Option[String],
    voucherId: Option[VoucherId]
) extends APIWithRoleMessage[CheckoutResponse]:
  override def plan(connection: Connection, username: String): IO[CheckoutResponse] =
    val body = CheckoutRequest(lines, customerName, customerPhone, deliveryAddress, voucherId)
    for
      account <- CustomerProfileTable.findByUsername(connection, username).flatMap {
        case Some(value) => IO.pure(value)
        case None        => IO.raiseError(HttpApiError.NotFound(OrderApiSupport.customerNotFound.error))
      }
      products <- CatalogProductTable.list(connection)
      profileForOrders =
        (body.customerName, body.customerPhone, body.deliveryAddress) match
          case (Some(n), Some(ph), Some(ad))
              if n.trim.nonEmpty && ph.trim.nonEmpty && ad.trim.nonEmpty =>
            account.profile.copy(name = n.trim, phone = ph.trim, defaultAddress = ad.trim)
          case _ => account.profile
      built <- buildOrdersForCheckout(products, profileForOrders, body.lines.map(OrderApiSupport.normalizeLine), body.voucherId)
      result <- built match
        case Left(msg) => IO.raiseError(HttpApiError.BadRequest(msg))
        case Right(checkout) =>
          val nextVouchers = checkout.usedVoucher.map(voucher => consumeVoucher(account.profile, voucher)).getOrElse(account.profile.vouchers)
          val nextAccount = account.copy(profile =
            account.profile.copy(
              walletBalance = roundMoney(account.profile.walletBalance - checkout.payableAmount),
              pendingOrders = checkout.orders.reverse ::: account.profile.pendingOrders,
              vouchers = nextVouchers
            )
          )
          for
            _ <- checkout.orders.traverse_(OrderTable.upsert(connection, _))
            _ <- CheckoutRequestTable.insert(connection, username, body, checkout.orders.map(_.id))
            _ <- CustomerProfileTable.upsert(connection, nextAccount)
          yield CheckoutResponse(
            orders = checkout.orders,
            walletBalance = nextAccount.profile.walletBalance,
            originalAmount = checkout.originalAmount,
            discountAmount = checkout.discountAmount,
            payableAmount = checkout.payableAmount,
            usedVoucher = checkout.usedVoucher
          )
    yield result
