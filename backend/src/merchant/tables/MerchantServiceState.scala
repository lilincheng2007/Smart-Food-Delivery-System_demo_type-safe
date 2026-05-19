package delivery.merchant.tables

import delivery.merchant.objects.{Merchant, Product}

final case class MerchantServiceState(
    merchantAccounts: List[MerchantAccount],
    catalogMerchants: List[Merchant],
    catalogProducts: List[Product]
)
