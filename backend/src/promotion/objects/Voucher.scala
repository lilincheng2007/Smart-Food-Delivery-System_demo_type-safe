package delivery.promotion.objects

import delivery.domain.VoucherId

final case class Voucher(
    id: VoucherId,
    title: String,
    discountAmount: Double,
    minSpend: Double,
    expiresAt: String,
    remainingCount: Int
)
