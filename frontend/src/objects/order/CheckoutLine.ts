export interface CheckoutBundleSelection {
  groupId: string
  productId: string
  quantity: number
}

export interface CheckoutLine {
  merchantId: string
  productId: string
  quantity: number
  bundleSelections?: CheckoutBundleSelection[]
}
