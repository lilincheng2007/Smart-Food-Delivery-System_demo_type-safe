import { PackageSearch, Store } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import type { MerchantStoreProfile } from '@/objects/merchant'

type ProductsTabProps = {
  selectedStore: MerchantStoreProfile | null
  onEditProduct: () => void
  onToggleProduct: () => void
}

export function ProductsTab({ selectedStore, onEditProduct, onToggleProduct }: ProductsTabProps) {
  const merchantProducts = selectedStore?.products ?? []

  if (!selectedStore) {
    return (
      <Card className="border-orange-100 bg-white/95">
        <CardContent className="p-6 text-sm text-slate-600">请先选择店铺后查看菜品管理内容。</CardContent>
      </Card>
    )
  }

  return (
    <div className="space-y-4">
      <section className="grid gap-4 md:grid-cols-3">
        <Card className="border-orange-100 bg-white/95 py-0">
          <CardHeader className="pb-2">
            <CardDescription>店铺名称</CardDescription>
            <CardTitle className="flex items-center gap-2">
              <Store className="size-4 text-orange-500" />
              {selectedStore.merchant.storeName}
            </CardTitle>
          </CardHeader>
        </Card>
        <Card className="border-orange-100 bg-white/95 py-0">
          <CardHeader className="pb-2">
            <CardDescription>店铺评分</CardDescription>
            <CardTitle>{selectedStore.merchant.rating.toFixed(1)} / 5.0</CardTitle>
          </CardHeader>
        </Card>
        <Card className="border-orange-100 bg-white/95 py-0">
          <CardHeader className="pb-2">
            <CardDescription>主营商品数</CardDescription>
            <CardTitle>{merchantProducts.length}</CardTitle>
          </CardHeader>
        </Card>
      </section>

      <Card className="border-orange-100 bg-white/95">
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <PackageSearch className="size-5 text-orange-500" />
            商品管理（上架 / 下架 / 编辑）
          </CardTitle>
          <CardDescription>商家可实时更新商品信息与库存状态</CardDescription>
        </CardHeader>
        <CardContent className="space-y-3">
          {merchantProducts.length === 0 ? (
            <p className="text-sm text-slate-500">当前店铺暂无商品，请先创建菜品。</p>
          ) : (
            merchantProducts.map((product) => (
              <div
                key={product.id}
                className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-orange-100 p-4"
              >
                <div className="space-y-1">
                  <p className="font-medium text-slate-900">{product.name}</p>
                  <p className="text-sm text-slate-600">
                    月销量 {product.monthlySales} · 库存状态 {product.inventoryStatus}
                  </p>
                </div>
                <div className="flex gap-2">
                  <Button size="sm" variant="outline" onClick={onEditProduct}>
                    编辑
                  </Button>
                  <Button size="sm" onClick={onToggleProduct}>
                    上/下架
                  </Button>
                </div>
              </div>
            ))
          )}
        </CardContent>
      </Card>
    </div>
  )
}
