import { Check, X } from 'lucide-react'
import { useMemo, useState } from 'react'

import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { resolveApiMediaUrl } from '@/lib/api-media-url'
import { bundleLineUnitPrice, bundleOptionExtraPrice } from '@/lib/bundles'
import { cn } from '@/lib/utils'
import type { Product } from '@/objects/merchant/Product'
import type { CheckoutBundleSelection } from '@/objects/order/CheckoutLine'

type BundleSelectionDialogProps = {
  product: Product | null
  products: Product[]
  open: boolean
  onOpenChange: (open: boolean) => void
  onAddToCart: (product: Product, selections: CheckoutBundleSelection[]) => void
  productPromotionForDisplay?: (productId: string, price: number) => { discountAmount: number; currentPrice: number } | null
}

const selectionKey = (groupId: string, productId: string) => `${groupId}::${productId}`

export function BundleSelectionDialog({
  product,
  products,
  open,
  onOpenChange,
  onAddToCart,
  productPromotionForDisplay,
}: BundleSelectionDialogProps) {
  const [selectionCounts, setSelectionCounts] = useState<Record<string, number>>({})

  const selectedCountForGroup = (groupId: string) =>
    Object.entries(selectionCounts).reduce((sum, [key, value]) => (key.startsWith(`${groupId}::`) ? sum + value : sum), 0)

  const selections = useMemo<CheckoutBundleSelection[]>(() => {
    if (!product) return []
    return (product.bundleGroups ?? []).flatMap((group) =>
      group.options
        .map((option) => ({
          groupId: group.id,
          productId: option.productId,
          quantity: selectionCounts[selectionKey(group.id, option.productId)] ?? 0,
        }))
        .filter((selection) => selection.quantity > 0),
    )
  }, [product, selectionCounts])

  const selectedOptions = useMemo(
    () =>
      selections.flatMap((selection) => {
        const optionProduct = products.find((item) => item.id === selection.productId)
        return optionProduct ? [{ ...selection, product: optionProduct }] : []
      }),
    [products, selections],
  )
  const selectedOptionItems = useMemo(
    () =>
      selectedOptions.flatMap((selection) =>
        Array.from({ length: selection.quantity }, (_, index) => ({
          ...selection,
          itemKey: `${selection.groupId}-${selection.productId}-${index}`,
        })),
      ),
    [selectedOptions],
  )

  const totalIncluded = product ? (product.bundleGroups ?? []).reduce((sum, group) => sum + group.quantity, 0) : 0
  const includedSummary = product ? (product.bundleGroups ?? []).map((group) => `${group.name}:${group.quantity}份`).join('、') : ''
  const ready = product ? (product.bundleGroups ?? []).every((group) => selectedCountForGroup(group.id) === group.quantity) : false
  const unitPrice = product ? bundleLineUnitPrice(product, selections, products) : 0
  const productPromotion = product && productPromotionForDisplay ? productPromotionForDisplay(product.id, unitPrice) : null

  const close = (nextOpen: boolean) => {
    if (!nextOpen) {
      setSelectionCounts({})
    }
    onOpenChange(nextOpen)
  }

  const chooseOption = (groupId: string, productId: string, groupQuantity: number) => {
    const key = selectionKey(groupId, productId)
    const selectedCount = selectedCountForGroup(groupId)
    const currentCount = selectionCounts[key] ?? 0

    setSelectionCounts((current) => {
      if (groupQuantity === 1) {
        const next = { ...current }
        Object.keys(next).forEach((itemKey) => {
          if (itemKey.startsWith(`${groupId}::`)) delete next[itemKey]
        })
        return currentCount > 0 ? next : { ...next, [key]: 1 }
      }

      if (selectedCount < groupQuantity) {
        return { ...current, [key]: currentCount + 1 }
      }

      if (currentCount > 0) {
        return { ...current, [key]: Math.max(0, currentCount - 1) }
      }

      return current
    })
  }

  const removeSelectedOption = (groupId: string, productId: string) => {
    const key = selectionKey(groupId, productId)
    setSelectionCounts((current) => {
      const nextCount = Math.max(0, (current[key] ?? 0) - 1)
      const next = { ...current, [key]: nextCount }
      if (nextCount === 0) delete next[key]
      return next
    })
  }

  return (
    <Dialog open={open} onOpenChange={close}>
      <DialogContent
        showCloseButton={false}
        className="!top-auto bottom-0 left-1/2 flex h-[min(90vh,48rem)] w-full max-w-[32rem] !translate-y-0 flex-col gap-0 overflow-hidden rounded-t-[2rem] rounded-b-none border border-border/70 bg-white p-0 shadow-2xl sm:bottom-4 sm:w-[calc(100vw-2rem)] sm:max-w-2xl sm:rounded-[2rem]"
      >
        {product ? (
          <>
            <DialogHeader className="shrink-0 px-6 pb-4 pt-7 text-left sm:px-7">
              <div className="flex items-start justify-between gap-4">
                <DialogTitle className="text-2xl font-semibold leading-tight tracking-normal text-slate-950 sm:text-3xl">{product.name}</DialogTitle>
                <button
                  type="button"
                  className="flex size-11 shrink-0 items-center justify-center rounded-full text-slate-950 transition-colors hover:bg-slate-100"
                  aria-label="关闭套餐选择"
                  onClick={() => close(false)}
                >
                  <X className="size-7 stroke-[2.4]" />
                </button>
              </div>
              <DialogDescription className="mt-3 text-base leading-7 text-slate-400">
                <span className="font-semibold text-slate-900">已包含</span>
                <span className="mx-2 text-slate-200">|</span>
                共{totalIncluded}份：{includedSummary}
              </DialogDescription>
            </DialogHeader>

            <div className="min-h-0 flex-1 space-y-8 overflow-y-auto px-6 pb-5 pt-1 sm:px-7">
              {(product.bundleGroups ?? []).map((group) => {
                const selectedCount = selectedCountForGroup(group.id)

                return (
                  <section key={group.id} className="space-y-4">
                    <div className="flex flex-wrap items-baseline gap-2">
                      <h3 className="text-xl font-semibold text-slate-950 sm:text-2xl">{group.name}</h3>
                      <span className="text-base text-slate-400">
                        <span className="mx-1 text-slate-200">|</span>
                        可选{group.quantity}份，已选{selectedCount}份
                      </span>
                    </div>

                    <div className="grid grid-cols-2 gap-3 sm:gap-4">
                      {group.options.length === 0 ? (
                        <p className="col-span-2 rounded-2xl border border-dashed border-border/70 px-4 py-6 text-sm text-muted-foreground sm:col-span-3">
                          商家还没有配置这个套餐的可选商品。
                        </p>
                      ) : null}

                      {group.options.map((option) => {
                        const optionProduct = products.find((item) => item.id === option.productId)
                        if (!optionProduct) return null

                        const key = selectionKey(group.id, option.productId)
                        const count = selectionCounts[key] ?? 0
                        const unavailable = selectedCount >= group.quantity && count === 0
                        const extraPrice = bundleOptionExtraPrice(group, optionProduct, products)

                        return (
                          <button
                            key={option.productId}
                            type="button"
                            disabled={unavailable}
                            className={cn(
                              'group overflow-hidden rounded-2xl border bg-[#fffef8] text-left transition-[border-color,opacity,box-shadow,transform]',
                              count > 0
                                ? 'border-yellow-300 shadow-[0_10px_24px_rgba(250,204,21,0.18)]'
                                : 'border-slate-100 shadow-sm',
                              unavailable ? 'opacity-45 grayscale' : 'cursor-pointer hover:border-yellow-200 active:scale-[0.99]',
                            )}
                            onClick={() => chooseOption(group.id, option.productId, group.quantity)}
                          >
                            <div className="relative aspect-[1.05] overflow-hidden bg-slate-100">
                              {optionProduct.imageUrl?.trim() ? (
                                <img src={resolveApiMediaUrl(optionProduct.imageUrl)} alt={optionProduct.name} className="size-full object-cover" />
                              ) : (
                                <div className="flex size-full items-center justify-center px-3 text-center text-xs text-slate-400">暂无图片</div>
                              )}
                              {option.recommended ? (
                                <Badge className="absolute left-0 top-0 rounded-none rounded-br-lg bg-amber-50 px-2 py-1 text-xs font-medium text-amber-700 hover:bg-amber-50 sm:text-sm">
                                  商家推荐
                                </Badge>
                              ) : null}
                              <span className="absolute bottom-0 left-0 bg-black/55 px-2 py-1 text-sm font-semibold text-white">1人份</span>
                            </div>

                            <div className="relative min-h-28 p-3">
                              <p className="line-clamp-2 text-base font-medium leading-6 text-slate-950 sm:text-lg sm:leading-7">
                                {optionProduct.name}
                                {count > 1 ? ` x${count}` : ''}
                              </p>
                              <span className="absolute bottom-3 left-3 text-base font-medium text-slate-300">+¥{extraPrice.toFixed(1)}</span>
                              <span
                                className={cn(
                                  'absolute bottom-3 right-3 flex size-9 items-center justify-center rounded-full border-2 sm:size-10',
                                  count > 0 ? 'border-yellow-400 bg-yellow-400 text-slate-950' : 'border-slate-200 bg-white text-transparent',
                                )}
                              >
                                <Check className="size-5 stroke-[3]" aria-hidden />
                              </span>
                            </div>
                          </button>
                        )
                      })}
                    </div>
                  </section>
                )
              })}
            </div>

            <div className="shrink-0 rounded-t-[2rem] bg-[#28292b] text-white shadow-[0_-16px_40px_rgba(15,23,42,0.18)]">
              <div className="flex min-h-28 items-start gap-4 overflow-x-auto px-6 py-5 sm:px-7">
                {selectedOptionItems.length === 0 ? <p className="self-center text-sm text-white/55">请选择套餐内容</p> : null}
                {selectedOptionItems.map((selection) => (
                  <div key={selection.itemKey} className="relative shrink-0">
                    <div className="size-18 overflow-hidden rounded-full border-4 border-[#28292b] bg-white sm:size-20">
                      {selection.product.imageUrl?.trim() ? (
                        <img src={resolveApiMediaUrl(selection.product.imageUrl)} alt={selection.product.name} className="size-full object-cover" />
                      ) : (
                        <div className="flex size-full items-center justify-center text-[10px] text-slate-400">暂无图</div>
                      )}
                    </div>
                    <button
                      type="button"
                      className="absolute -right-1 -top-1 flex size-6 items-center justify-center rounded-full bg-slate-200 text-slate-950 shadow-sm"
                      aria-label={`移除${selection.product.name}`}
                      onClick={() => removeSelectedOption(selection.groupId, selection.productId)}
                    >
                      <X className="size-4" />
                    </button>
                  </div>
                ))}
              </div>

              <div className="flex items-center justify-between gap-4 border-t border-white/10 px-6 py-5 sm:px-7">
                <div>
                  {productPromotion ? (
                    <div className="space-y-1">
                      <p className="text-sm text-white/50 line-through">原价 ¥{unitPrice.toFixed(2)}</p>
                      <p className="text-3xl font-bold tabular-nums text-rose-300">¥{productPromotion.currentPrice.toFixed(2)}</p>
                      <p className="text-sm text-yellow-300">已优惠 ¥{productPromotion.discountAmount.toFixed(2)}</p>
                    </div>
                  ) : (
                    <p className="text-3xl font-bold tabular-nums">¥{unitPrice.toFixed(1)}</p>
                  )}
                  <p className="mt-1 text-sm text-white/55">{ready ? '限1份' : '请按类别选满套餐内容'}</p>
                </div>
                <Button
                  type="button"
                  disabled={!ready}
                  className="h-14 min-w-44 rounded-full bg-yellow-400 px-8 text-xl font-semibold text-slate-950 shadow-[inset_0_-2px_0_rgba(0,0,0,0.08)] hover:bg-yellow-300 disabled:opacity-50"
                  onClick={() => {
                    onAddToCart(product, selections)
                    setSelectionCounts({})
                    onOpenChange(false)
                  }}
                >
                  加入购物车
                </Button>
              </div>
            </div>
          </>
        ) : null}
      </DialogContent>
    </Dialog>
  )
}
