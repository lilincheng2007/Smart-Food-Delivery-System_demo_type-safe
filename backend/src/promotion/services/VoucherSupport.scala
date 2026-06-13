package delivery.promotion.services

import delivery.domain.{Voucher, VoucherId}

object VoucherSupport:
  private val StandardDiscountAmount = 10.0
  private val StandardMinSpend = 30.0
  private val StandardExpiresAt = "2026-12-31"

  val PlatformThirtyTenTitle = "平台满30减10优惠券"

  def standardPlatformVoucherId(customerId: String): VoucherId =
    s"v-platform-30-10-$customerId"

  def standardPlatformVoucher(customerId: String, remainingCount: Int): Voucher =
    Voucher(standardPlatformVoucherId(customerId), PlatformThirtyTenTitle, StandardDiscountAmount, StandardMinSpend, StandardExpiresAt, remainingCount)

  def mergeStandardPlatformVouchers(customerId: String, vouchers: List[Voucher]): List[Voucher] =
    val (mergeable, others) = vouchers.partition(voucher => isStandardPlatformVoucher(customerId, voucher) || isLegacyLevelVoucher(voucher))
    val remainingCount = mergeable.map(voucher => math.max(0, voucher.remainingCount)).sum
    if remainingCount <= 0 then others
    else standardPlatformVoucher(customerId, remainingCount) :: others

  def addStandardPlatformVouchers(customerId: String, vouchers: List[Voucher], count: Int): List[Voucher] =
    if count <= 0 then mergeStandardPlatformVouchers(customerId, vouchers)
    else mergeStandardPlatformVouchers(customerId, standardPlatformVoucher(customerId, count) :: vouchers)

  private def isStandardPlatformVoucher(customerId: String, voucher: Voucher): Boolean =
    voucher.id == standardPlatformVoucherId(customerId) || (voucher.title == PlatformThirtyTenTitle && isThirtyTenVoucher(voucher))

  private def isLegacyLevelVoucher(voucher: Voucher): Boolean =
    voucher.id.startsWith("v-level-") && isThirtyTenVoucher(voucher)

  private def isThirtyTenVoucher(voucher: Voucher): Boolean =
    voucher.discountAmount == StandardDiscountAmount && voucher.minSpend == StandardMinSpend

end VoucherSupport
