import type { Product, ProductBundleGroup } from '@/objects/merchant/Product'
import type { CheckoutBundleSelection } from '@/objects/order/CheckoutLine'

export const isBundleProduct = (product: Pick<Product, 'bundleGroups'>) => (product.bundleGroups ?? []).length > 0

export const bundleGroupMinPrice = (group: ProductBundleGroup, products: Product[]) => {
  const prices = group.options
    .map((option) => products.find((product) => product.id === option.productId)?.price)
    .filter((price): price is number => typeof price === 'number' && Number.isFinite(price))
  return prices.length > 0 ? Math.min(...prices) : 0
}

export const bundleBasePrice = (product: Product) => {
  if (!isBundleProduct(product)) return product.price
  return product.price
}

export const bundleOptionExtraPrice = (group: ProductBundleGroup, optionProduct: Product, products: Product[]) =>
  Math.max(0, optionProduct.price - bundleGroupMinPrice(group, products))

export const bundleLineUnitPrice = (
  product: Product,
  selections: CheckoutBundleSelection[] | undefined,
  products: Product[],
) => {
  if (!isBundleProduct(product)) return product.price
  const basePrice = bundleBasePrice(product)
  const extraPrice = (product.bundleGroups ?? []).reduce((sum, group) => {
    const minPrice = bundleGroupMinPrice(group, products)
    const groupExtra = (selections ?? [])
      .filter((selection) => selection.groupId === group.id)
      .reduce((groupSum, selection) => {
        const selectedProduct = products.find((item) => item.id === selection.productId)
        return groupSum + (selectedProduct ? Math.max(0, selectedProduct.price - minPrice) * selection.quantity : 0)
      }, 0)
    return sum + groupExtra
  }, 0)
  return basePrice + extraPrice
}

export const bundleSelectionSummary = (
  product: Product,
  selections: CheckoutBundleSelection[] | undefined,
  products: Product[],
) => {
  const activeSelections = selections ?? []
  if (activeSelections.length === 0) return ''

  if (isBundleProduct(product)) {
    return (product.bundleGroups ?? [])
      .map((group) => {
        const names = activeSelections
          .filter((selection) => selection.groupId === group.id)
          .flatMap((selection) => {
            const selectedProduct = products.find((item) => item.id === selection.productId)
            return selectedProduct ? [`${selectedProduct.name}x${selection.quantity}`] : []
          })
        return names.length > 0 ? `${group.name}：${names.join('、')}` : null
      })
      .filter(Boolean)
      .join('；')
  }

  const names = activeSelections.flatMap((selection) => {
    const selectedProduct = products.find((item) => item.id === selection.productId)
    return selectedProduct ? [`${selectedProduct.name}x${selection.quantity}`] : []
  })
  return names.length > 0 ? `套餐内容：${names.join('、')}` : ''
}
