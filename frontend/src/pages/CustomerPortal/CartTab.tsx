import { Minus, Plus, ShoppingCart } from 'lucide-react'

import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import type { Merchant, Product } from '@/objects/merchant'
import type { MerchantId, ProductId } from '@/objects/shared'
import type { CartLine } from '@/stores/pages/use-customer-portal-store'

type CartTabProps = {
  merchants: Merchant[]
  products: Product[]
  cartLines: CartLine[]
  onChangeQuantity: (merchantId: MerchantId, productId: ProductId, nextQuantity: number) => void
  onCheckout: () => void
}

export function CartTab({ merchants, products, cartLines, onChangeQuantity, onCheckout }: CartTabProps) {
  const cartGroupedByMerchant = cartLines.reduce<Record<string, CartLine[]>>((grouped, line) => {
    if (!grouped[line.merchantId]) {
      grouped[line.merchantId] = []
    }
    grouped[line.merchantId].push(line)
    return grouped
  }, {})

  const cartTotal = cartLines.reduce((sum, line) => {
    const product = products.find((item) => item.id === line.productId)
    return sum + (product ? product.price * line.quantity : 0)
  }, 0)

  return (
    <div className="space-y-4">
      <Card className="border-orange-100 bg-white/95">
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <ShoppingCart className="size-5 text-orange-500" />
            购物车
          </CardTitle>
          <CardDescription>按商家分组展示已选菜品（参考外卖平台购物车布局）</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          {Object.keys(cartGroupedByMerchant).length === 0 ? (
            <div className="rounded-xl border border-dashed border-orange-200 p-8 text-center text-sm text-slate-500">
              购物车为空，去首页选择菜品吧。
            </div>
          ) : (
            Object.entries(cartGroupedByMerchant).map(([merchantId, lines]) => {
              const merchant = merchants.find((item) => item.id === merchantId)
              if (!merchant) {
                return null
              }

              return (
                <div key={merchantId} className="rounded-2xl border border-orange-100 bg-white p-4">
                  <div className="mb-3 flex items-center justify-between">
                    <p className="text-lg font-semibold text-slate-900">{merchant.storeName}</p>
                    <Badge variant="outline">{merchant.category}</Badge>
                  </div>
                  <div className="space-y-3">
                    {lines.map((line) => {
                      const product = products.find((item) => item.id === line.productId)
                      if (!product) {
                        return null
                      }

                      return (
                        <div key={line.productId} className="flex items-center justify-between gap-4 rounded-xl bg-slate-50 p-3">
                          <div className="space-y-1">
                            <p className="font-medium text-slate-900">{product.name}</p>
                            <p className="text-sm text-slate-600">单价 ¥{product.price.toFixed(1)}</p>
                          </div>
                          <div className="flex items-center gap-2">
                            <Button
                              size="icon"
                              variant="outline"
                              className="size-8"
                              onClick={() => onChangeQuantity(line.merchantId, line.productId, line.quantity - 1)}
                            >
                              <Minus className="size-4" />
                            </Button>
                            <span className="w-6 text-center text-sm font-medium">{line.quantity}</span>
                            <Button
                              size="icon"
                              variant="outline"
                              className="size-8"
                              onClick={() => onChangeQuantity(line.merchantId, line.productId, line.quantity + 1)}
                            >
                              <Plus className="size-4" />
                            </Button>
                          </div>
                        </div>
                      )
                    })}
                  </div>
                </div>
              )
            })
          )}
        </CardContent>
      </Card>

      <Card className="border-orange-100 bg-white/95">
        <CardContent className="flex items-center justify-between p-4">
          <div className="text-sm text-slate-700">
            合计：
            <span className="ml-1 text-xl font-semibold text-orange-600">¥{cartTotal.toFixed(1)}</span>
          </div>
          <Button onClick={onCheckout} disabled={cartLines.length === 0}>
            结算
          </Button>
        </CardContent>
      </Card>
    </div>
  )
}
