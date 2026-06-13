package delivery.order.objects

import delivery.domain.{MerchantId, ProductId}

final case class CheckoutBundleSelection(groupId: String, productId: ProductId, quantity: Int)

final case class CheckoutLine(
    merchantId: MerchantId,
    productId: ProductId,
    quantity: Int,
    bundleSelections: List[CheckoutBundleSelection] = Nil
)
