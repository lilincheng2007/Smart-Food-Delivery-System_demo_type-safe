import { ChartNoAxesCombined } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import type { MerchantStoreProfile } from '@/objects/merchant'

type ProfileTabProps = {
  selectedStore: MerchantStoreProfile | null
  onOpenStoreDialog: () => void
}

export function ProfileTab({ selectedStore, onOpenStoreDialog }: ProfileTabProps) {
  const merchantPendingOrders = selectedStore?.pendingOrders ?? []
  const merchantHistoryOrders = selectedStore?.historyOrders ?? []
  const merchantOrders = [...merchantPendingOrders, ...merchantHistoryOrders]

  return (
    <div className="space-y-4">
      <Card className="border-orange-100 bg-white/95">
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <ChartNoAxesCombined className="size-5 text-orange-500" />
            营业概况
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-3 text-sm text-slate-700">
          {selectedStore ? (
            <>
              <div className="flex items-center justify-between rounded-xl border border-orange-100 p-3">
                <span>当前店铺</span>
                <span>{selectedStore.merchant.storeName}</span>
              </div>
              <div className="flex items-center justify-between rounded-xl border border-orange-100 p-3">
                <span>待处理订单</span>
                <span>{merchantPendingOrders.length}</span>
              </div>
              <div className="flex items-center justify-between rounded-xl border border-orange-100 p-3">
                <span>历史订单</span>
                <span>{merchantHistoryOrders.length}</span>
              </div>
              <div className="flex items-center justify-between rounded-xl border border-orange-100 p-3">
                <span>总成交额（模拟）</span>
                <span>{merchantOrders.reduce((sum, item) => sum + item.totalAmount, 0)} 元</span>
              </div>
            </>
          ) : (
            <p>当前未选择店铺。</p>
          )}
        </CardContent>
      </Card>

      <Card className="border-orange-100 bg-white/95">
        <CardContent className="flex items-center justify-between gap-3 p-4">
          <p className="text-sm text-slate-700">可随时切换已创建店铺，查看对应店铺数据。</p>
          <Button onClick={onOpenStoreDialog}>更改店铺</Button>
        </CardContent>
      </Card>
    </div>
  )
}
