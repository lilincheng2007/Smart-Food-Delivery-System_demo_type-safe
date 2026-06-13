package delivery.order.services

import cats.effect.IO
import delivery.merchant.services.MerchantBusinessHoursService
import delivery.merchant.objects.{Merchant, Product}
import delivery.order.objects.{CheckoutLine, Order, OrderItem, OrderPriceBreakdown, OrderPriceBreakdownLine, OrderPriceSnapshot, OrderPriceSnapshotItem, OrderTimelineEvent}
import delivery.order.objects.apiTypes.OrderMerchantNote
import delivery.domain.{InventoryStatus, ListingStatus, MerchantId, OrderStatus, ProductId, Promotion, Voucher, VoucherId}
import delivery.promotion.services.PromotionPricing
import delivery.user.objects.CustomerProfile

import java.time.LocalDate
import scala.util.Try

object OrderCheckoutService:

  private val FoodieLevelPoints = 200
  private val DefaultVoucherDiscount = 10.0
  private val DefaultVoucherMinSpend = 30.0
  private val DefaultVoucherExpiresAt = "2026-12-31"

  final case class CheckoutBuild(
      orders: List[Order],
      originalAmount: Double,
      discountAmount: Double,
      payableAmount: Double,
      usedVoucher: Option[Voucher],
      priceBreakdown: OrderPriceBreakdown
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

  private def priceBreakdown(
      productOriginalAmount: Double,
      merchantDiscountAmount: Double,
      voucherDiscountAmount: Double,
      platformDiscountAmount: Double,
      deliveryFeeAmount: Double,
      payableAmount: Double
  ): OrderPriceBreakdown =
    val discountAmount = roundMoney(merchantDiscountAmount + voucherDiscountAmount + platformDiscountAmount)
    val lines = List(
      Some(OrderPriceBreakdownLine("productOriginalAmount", "商品原价", productOriginalAmount, "charge")),
      Option.when(merchantDiscountAmount > 0)(OrderPriceBreakdownLine("merchantDiscountAmount", "商家优惠", merchantDiscountAmount, "discount")),
      Option.when(voucherDiscountAmount > 0)(OrderPriceBreakdownLine("voucherDiscountAmount", "优惠券抵扣", voucherDiscountAmount, "discount")),
      Option.when(platformDiscountAmount > 0)(OrderPriceBreakdownLine("platformDiscountAmount", "平台优惠", platformDiscountAmount, "discount")),
      Some(OrderPriceBreakdownLine("deliveryFeeAmount", "配送费", deliveryFeeAmount, "charge")),
      Some(OrderPriceBreakdownLine("discountAmount", "优惠抵扣合计", discountAmount, "discount")),
      Some(OrderPriceBreakdownLine("payableAmount", "实付金额", payableAmount, "total"))
    ).flatten
    OrderPriceBreakdown(
      lines = lines,
      productOriginalAmount = productOriginalAmount,
      merchantDiscountAmount = merchantDiscountAmount,
      voucherDiscountAmount = voucherDiscountAmount,
      platformDiscountAmount = platformDiscountAmount,
      deliveryFeeAmount = deliveryFeeAmount,
      discountAmount = discountAmount,
      payableAmount = payableAmount
    )

  private def allocateDiscount(totalDiscount: Double, totalBase: Double, currentBase: Double, previousAllocated: Double, isLast: Boolean): Double =
    if totalDiscount <= 0 || totalBase <= 0 then 0
    else if isLast then roundMoney(totalDiscount - previousAllocated)
    else roundMoney(totalDiscount * currentBase / totalBase)

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
        val lineValidationError = validateCheckoutLines(productsById, lines)
        val inventoryValidationError = validateInventory(productsById, lines)
        val bundleValidationError = lines.flatMap(line => productsById.get(line.productId).flatMap(product => validateBundleLine(product, line, productsById))).headOption
        val notesByMerchant: Map[MerchantId, OrderMerchantNote] =
          merchantNotes
            .map(note => note.copy(text = note.text.map(_.trim).filter(_.nonEmpty), imageUrl = note.imageUrl.map(_.trim).filter(_.nonEmpty)))
            .filter(note => note.text.nonEmpty || note.imageUrl.nonEmpty)
            .map(note => note.merchantId -> note)
            .toMap
        if lineValidationError.nonEmpty then Left(lineValidationError.get)
        else if inventoryValidationError.nonEmpty then Left(inventoryValidationError.get)
        else if bundleValidationError.nonEmpty then Left(bundleValidationError.get)
        else
          val merchantsById = merchants.map(merchant => merchant.id -> merchant).toMap
          val closedMerchantMessage = grouped.flatMap { case (merchantId, _) =>
            merchantsById.get(merchantId).filterNot(MerchantBusinessHoursService.isAcceptingOrders(_)).map(merchant => MerchantBusinessHoursService.unavailableMessage(merchant))
          }.headOption
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

          if closedMerchantMessage.nonEmpty then Left(closedMerchantMessage.get)
          else if rawOrders.isEmpty then Left("无法解析购物车商品")
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
                  val previousVoucherDiscount = rawOrders.take(idx).map(raw => allocateDiscount(voucherDiscount, afterMerchantDiscountAmount, raw.merchantReceivable, 0, isLast = false)).sum
                  val previousPlatformDiscount = rawOrders.take(idx).map(raw => allocateDiscount(platformDiscount, afterMerchantDiscountAmount, raw.merchantReceivable, 0, isLast = false)).sum
                  val isLastOrder = idx == rawOrders.size - 1
                  val voucherOrderDiscount = allocateDiscount(voucherDiscount, afterMerchantDiscountAmount, rawOrder.merchantReceivable, previousVoucherDiscount, isLastOrder)
                  val platformOrderDiscount = allocateDiscount(platformDiscount, afterMerchantDiscountAmount, rawOrder.merchantReceivable, previousPlatformDiscount, isLastOrder)
                  val customerOnlyOrderDiscount = roundMoney(voucherOrderDiscount + platformOrderDiscount)
                  val orderDiscount = roundMoney(rawOrder.merchantDiscount + customerOnlyOrderDiscount)
                  val orderPayable = roundMoney(rawOrder.merchantReceivable - customerOnlyOrderDiscount)
                  val note = notesByMerchant.get(rawOrder.merchantId)
                  val appliedPromotions = List(rawOrder.appliedMerchantPromotion, platformPromotion.map(_.promotion)).flatten
                  val breakdown = priceBreakdown(
                    productOriginalAmount = rawOrder.originalAmount,
                    merchantDiscountAmount = rawOrder.merchantDiscount,
                    voucherDiscountAmount = voucherOrderDiscount,
                    platformDiscountAmount = platformOrderDiscount,
                    deliveryFeeAmount = 0,
                    payableAmount = orderPayable
                  )
                  val snapshot = OrderPriceSnapshot(
                    items = rawOrder.items.map(item => OrderPriceSnapshotItem(item.productId, item.name, item.unitPrice, item.quantity, roundMoney(item.unitPrice * item.quantity))),
                    productOriginalAmount = rawOrder.originalAmount,
                    merchantDiscountAmount = rawOrder.merchantDiscount,
                    voucherDiscountAmount = voucherOrderDiscount,
                    platformDiscountAmount = platformOrderDiscount,
                    deliveryFeeAmount = 0,
                    discountAmount = orderDiscount,
                    payableAmount = orderPayable,
                    merchantReceivableAmount = rawOrder.merchantReceivable,
                    appliedPromotionTitles = appliedPromotions.map(_.title),
                    usedVoucherTitle = usedVoucher.map(_.title)
                  )
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
                    priceSnapshot = Some(snapshot),
                    priceBreakdown = Some(breakdown),
                    pointsAwarded = 0,
                    customerNoteText = note.flatMap(_.text),
                    customerNoteImageUrl = note.flatMap(_.imageUrl),
                    statusTimeline = List(OrderTimelineEvent("placed", "已下单", orderTimeText, Some("订单已提交，等待商家接单")))
                  )
                }
                val checkoutBreakdown = priceBreakdown(
                  productOriginalAmount = originalAmount,
                  merchantDiscountAmount = merchantDiscountAmount,
                  voucherDiscountAmount = voucherDiscount,
                  platformDiscountAmount = platformDiscount,
                  deliveryFeeAmount = 0,
                  payableAmount = payableAmount
                )
                Right(CheckoutBuild(orders.reverse, originalAmount, discountAmount, payableAmount, usedVoucher, checkoutBreakdown))
            }

  def inventoryDeductions(products: List[Product], lines: List[CheckoutLine]): List[Product] =
    val productsById = products.map(product => product.id -> product).toMap
    val consumed = consumedQuantities(productsById, lines)
    products.flatMap { product =>
      val quantity = consumed.getOrElse(product.id, 0)
      if quantity <= 0 || normalizeInventoryMode(product.inventoryMode) != "finite" then None
      else
        val nextStock = math.max(0, product.remainingStock - quantity)
        Some(product.copy(remainingStock = nextStock, inventoryStatus = inventoryStatus(nextStock, product.listingStatus, product.inventoryMode)))
    }

  private def validateCheckoutLines(productsById: Map[ProductId, Product], lines: List[CheckoutLine]): Option[String] =
    lines.flatMap { line =>
      if line.quantity <= 0 then Some("商品数量必须大于 0")
      else
        productsById.get(line.productId) match
          case None => Some("购物车包含不存在的商品")
          case Some(product) if product.merchantId != line.merchantId => Some(s"${product.name}不属于当前商家")
          case Some(product) if line.bundleSelections.exists(_.quantity <= 0) => Some(s"${product.name}的套餐选择数量无效")
          case Some(_) => None
    }.headOption

  private def validateInventory(productsById: Map[ProductId, Product], lines: List[CheckoutLine]): Option[String] =
    consumedQuantities(productsById, lines).toList.flatMap { case (productId, quantity) =>
      productsById.get(productId).flatMap { product =>
        val mode = normalizeInventoryMode(product.inventoryMode)
        if product.listingStatus != ListingStatus.上架 then Some(s"${product.name}暂未上架")
        else if mode == "soldOut" then Some(s"${product.name}已售罄")
        else if product.maxPerOrder.exists(limit => quantity > limit) then Some(s"${product.name}每单限购${product.maxPerOrder.get}份")
        else if mode == "finite" && product.remainingStock <= 0 then Some(s"${product.name}已售罄")
        else if mode == "finite" && quantity > product.remainingStock then Some(s"${product.name}库存不足，当前仅剩${product.remainingStock}份")
        else None
      }
    }.headOption

  private def consumedQuantities(productsById: Map[ProductId, Product], lines: List[CheckoutLine]): Map[ProductId, Int] =
    lines.foldLeft(Map.empty[ProductId, Int]) { (current, line) =>
      productsById.get(line.productId) match
        case None => current
        case Some(product) =>
          val withBase = addQuantity(current, product.id, line.quantity)
          if product.bundleGroups.isEmpty then withBase
          else
            line.bundleSelections.foldLeft(withBase) { (next, selection) =>
              addQuantity(next, selection.productId, selection.quantity * line.quantity)
            }
    }

  private def addQuantity(values: Map[ProductId, Int], productId: ProductId, quantity: Int): Map[ProductId, Int] =
    values.updated(productId, values.getOrElse(productId, 0) + math.max(0, quantity))

  private def normalizeInventoryMode(value: String): String =
    val trimmed = value.trim
    if Set("unlimited", "finite", "soldOut").contains(trimmed) then trimmed else "finite"

  private def inventoryStatus(remainingStock: Int, listingStatus: ListingStatus, inventoryMode: String): InventoryStatus =
    val mode = normalizeInventoryMode(inventoryMode)
    if listingStatus == ListingStatus.下架 || mode == "soldOut" then InventoryStatus.售罄
    else if mode == "unlimited" then InventoryStatus.充足
    else if remainingStock <= 0 then InventoryStatus.售罄
    else if remainingStock <= 20 then InventoryStatus.紧张
    else InventoryStatus.充足

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
        val duplicated = selected.exists(selection => selection.quantity > 1)
        if selectedCount != group.quantity then Some(s"${product.name}的${group.name}需要选择${group.quantity}件")
        else if invalid then Some(s"${product.name}的${group.name}包含不可选菜品")
        else if group.selectionType == "nonRepeatable" && duplicated then Some(s"${product.name}的${group.name}不可重复选择同一菜品")
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

  private def bundleOptionExtraPrice(group: delivery.merchant.objects.ProductBundleGroup, option: delivery.merchant.objects.ProductBundleOption, optionProduct: Product): Double =
    if option.customExtraPrice || option.extraPrice > 0 then math.max(0, option.extraPrice)
    else if group.includedPrice > 0 then math.max(0, optionProduct.price - group.includedPrice)
    else 0

  private def bundleLinePrice(product: Product, line: CheckoutLine, productsById: Map[ProductId, Product]): Double =
    if product.bundleGroups.isEmpty then product.price
    else
      val extra = product.bundleGroups.map { group =>
        line.bundleSelections
          .filter(_.groupId == group.id)
          .flatMap { selection =>
            for
              option <- group.options.find(_.productId == selection.productId)
              optionProduct <- productsById.get(selection.productId)
            yield bundleOptionExtraPrice(group, option, optionProduct) * selection.quantity
          }
          .sum
      }.sum
      roundMoney(product.price + extra)

end OrderCheckoutService
