package delivery.order.api

import cats.effect.IO
import delivery.merchant.objects.{Merchant, Product}
import delivery.order.objects.{CheckoutLine, Order, OrderItem}
import delivery.order.objects.apiTypes.OrderMerchantNote
import delivery.shared.objects.{MerchantId, OrderStatus, ProductId, Promotion, Voucher, VoucherId}
import delivery.shared.utils.PromotionPricing
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

  private final case class RawOrder(
      merchantId: MerchantId,
      items: List[OrderItem],
      originalAmount: Double,
      merchantDiscount: Double,
      merchantReceivable: Double,
      appliedMerchantPromotion: Option[Promotion]
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

  def buildOrdersForCheckout(
      products: List[Product],
      merchants: List[Merchant],
      platformPromotions: List[Promotion],
      customerProfile: CustomerProfile,
      lines: List[CheckoutLine],
      voucherId: Option[VoucherId],
      merchantNotes: List[OrderMerchantNote] = Nil
  ): IO[Either[String, CheckoutBuild]] =
    if lines.isEmpty then IO.pure(Left("购物车为空"))
    else
      for
        nowMillis <- IO.realTime.map(_.toMillis)
        zoneId <- IO.delay(java.time.ZoneId.systemDefault())
      yield
        val grouped = lines.groupBy(_.merchantId).toList
        val productsById = products.map(product => product.id -> product).toMap
        val bundleValidationError = lines.flatMap(line => productsById.get(line.productId).flatMap(product => validateBundleLine(product, line, productsById))).headOption
        val notesByMerchant: Map[MerchantId, OrderMerchantNote] =
          merchantNotes
            .map(note => note.copy(text = note.text.map(_.trim).filter(_.nonEmpty), imageUrl = note.imageUrl.map(_.trim).filter(_.nonEmpty)))
            .filter(note => note.text.nonEmpty || note.imageUrl.nonEmpty)
            .map(note => note.merchantId -> note)
            .toMap
        if bundleValidationError.nonEmpty then Left(bundleValidationError.get)
        else
          val merchantsById = merchants.map(merchant => merchant.id -> merchant).toMap
          val rawOrders = grouped.flatMap { case (merchantId, groupLines) =>
            val items = groupLines.flatMap { line =>
              products.find(p => p.id == line.productId && p.merchantId == merchantId).map(p => OrderItem(p.id, orderItemName(p, line, productsById), bundleLinePrice(p, line, productsById), line.quantity))
            }
            if items.isEmpty then None
            else
              val original = roundMoney(items.map(i => i.unitPrice * i.quantity).sum)
              val itemCount = items.map(_.quantity).sum
              val promotionItems = items.map(item => PromotionPricing.PromotionItem(item.productId, item.unitPrice, item.quantity))
              val merchantPromotion = merchantsById.get(merchantId).flatMap(merchant => PromotionPricing.bestForItems(merchant.promotions, original, itemCount, promotionItems))
              val merchantDiscount = merchantPromotion.map(_.discountAmount).getOrElse(0.0)
              val merchantReceivable = roundMoney(original - merchantDiscount)
              Some(RawOrder(merchantId, items, original, merchantDiscount, merchantReceivable, merchantPromotion.map(_.promotion)))
          }

          if rawOrders.isEmpty then Left("无法解析购物车商品")
          else
            val originalAmount = roundMoney(rawOrders.map(_.originalAmount).sum)
            val merchantDiscountAmount = roundMoney(rawOrders.map(_.merchantDiscount).sum)
            val afterMerchantDiscountAmount = roundMoney(rawOrders.map(_.merchantReceivable).sum)
            val itemCount = rawOrders.flatMap(_.items).map(_.quantity).sum
            validateVoucher(customerProfile, voucherId, afterMerchantDiscountAmount).flatMap { usedVoucher =>
              val voucherDiscount = usedVoucher.map(voucher => math.min(voucher.discountAmount, afterMerchantDiscountAmount)).getOrElse(0.0)
              val promotionItems = rawOrders.flatMap(_.items).map(item => PromotionPricing.PromotionItem(item.productId, item.unitPrice, item.quantity))
              val platformPromotion = PromotionPricing.bestForItems(platformPromotions, afterMerchantDiscountAmount - voucherDiscount, itemCount, promotionItems)
              val platformDiscount = platformPromotion.map(_.discountAmount).getOrElse(0.0)
              val discountAmount = roundMoney(merchantDiscountAmount + voucherDiscount + platformDiscount)
              val customerOnlyDiscount = roundMoney(voucherDiscount + platformDiscount)
              val payableAmount = roundMoney(afterMerchantDiscountAmount - customerOnlyDiscount)
              if customerProfile.walletBalance < payableAmount then Left("余额不足")
              else
                val now = java.time.Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDateTime
                val orderTimeText = f"${now.getYear}%04d-${now.getMonthValue}%02d-${now.getDayOfMonth}%02d ${now.getHour}%02d:${now.getMinute}%02d"
                val orders = rawOrders.zipWithIndex.map { case (rawOrder, idx) =>
                  val customerOnlyOrderDiscount =
                    if afterMerchantDiscountAmount <= 0 then 0
                    else if idx == rawOrders.size - 1 then roundMoney(customerOnlyDiscount - rawOrders.take(idx).map(raw => roundMoney(customerOnlyDiscount * raw.merchantReceivable / afterMerchantDiscountAmount)).sum)
                    else roundMoney(customerOnlyDiscount * rawOrder.merchantReceivable / afterMerchantDiscountAmount)
                  val orderDiscount = roundMoney(rawOrder.merchantDiscount + customerOnlyOrderDiscount)
                  val orderPayable = roundMoney(rawOrder.merchantReceivable - customerOnlyOrderDiscount)
                  val note = notesByMerchant.get(rawOrder.merchantId)
                  val appliedPromotions = List(rawOrder.appliedMerchantPromotion, platformPromotion.map(_.promotion)).flatten
                  Order(
                    id = s"o-$nowMillis-${idx + 1}",
                    customerId = customerProfile.id,
                    customerName = customerProfile.name,
                    customerPhone = customerProfile.phone,
                    merchantId = rawOrder.merchantId,
                    riderId = None,
                    items = rawOrder.items,
                    totalAmount = orderPayable,
                    deliveryAddress = customerProfile.defaultAddress,
                    status = OrderStatus.待商家接单,
                    placedAt = orderTimeText,
                    originalAmount = rawOrder.originalAmount,
                    discountAmount = orderDiscount,
                    payableAmount = orderPayable,
                    usedVoucher = usedVoucher,
                    merchantDiscountAmount = rawOrder.merchantDiscount,
                    platformDiscountAmount = customerOnlyOrderDiscount,
                    merchantReceivableAmount = rawOrder.merchantReceivable,
                    appliedPromotions = appliedPromotions,
                    pointsAwarded = 0,
                    customerNoteText = note.flatMap(_.text),
                    customerNoteImageUrl = note.flatMap(_.imageUrl)
                  )
                }
                Right(CheckoutBuild(orders.reverse, originalAmount, discountAmount, payableAmount, usedVoucher))
            }

  private def validateBundleLine(product: Product, line: CheckoutLine, productsById: Map[ProductId, Product]): Option[String] =
    if product.bundleGroups.isEmpty then
      val invalid = line.bundleSelections.exists(selection => productsById.get(selection.productId).forall(_.merchantId != product.merchantId))
      if invalid then Some(s"${product.name}包含不可选菜品") else None
    else
      product.bundleGroups.flatMap { group =>
        val selected = line.bundleSelections.filter(_.groupId == group.id)
        val selectedCount = selected.map(selection => math.max(0, selection.quantity)).sum
        val allowedIds = group.options.map(_.productId).toSet
        val invalid = selected.exists(selection => !allowedIds.contains(selection.productId) || productsById.get(selection.productId).forall(_.merchantId != product.merchantId))
        if selectedCount != group.quantity then Some(s"${product.name}的${group.name}需要选择${group.quantity}件")
        else if invalid then Some(s"${product.name}的${group.name}包含不可选菜品")
        else None
      }.headOption

  private def orderItemName(product: Product, line: CheckoutLine, productsById: Map[ProductId, Product]): String =
    if product.bundleGroups.isEmpty then
      val names = line.bundleSelections
        .flatMap(selection => productsById.get(selection.productId).map(item => s"${item.name}x${selection.quantity}"))
        .mkString("、")
      if names.isEmpty then product.name else s"${product.name}（套餐内容：$names）"
    else
      val summary = product.bundleGroups.flatMap { group =>
        val selected = line.bundleSelections.filter(_.groupId == group.id)
        if selected.isEmpty then None
        else
          val names = selected
            .flatMap(selection => productsById.get(selection.productId).map(item => s"${item.name}x${selection.quantity}"))
            .mkString("、")
          Some(s"${group.name}：$names")
      }.mkString("；")
      if summary.isEmpty then product.name else s"${product.name}（$summary）"

  private def bundleLinePrice(product: Product, line: CheckoutLine, productsById: Map[ProductId, Product]): Double =
    if product.bundleGroups.isEmpty then product.price
    else
      val extra = product.bundleGroups.map { group =>
        val optionPrices = group.options.flatMap(option => productsById.get(option.productId).map(_.price))
        val minPrice = if optionPrices.isEmpty then 0.0 else optionPrices.min
        line.bundleSelections
          .filter(_.groupId == group.id)
          .flatMap(selection => productsById.get(selection.productId).map(selected => math.max(0, selected.price - minPrice) * selection.quantity))
          .sum
      }.sum
      roundMoney(product.price + extra)

end OrderAPIMessageSupport
