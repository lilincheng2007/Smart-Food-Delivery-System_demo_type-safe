import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import type { MerchantStoreProfile } from '@/objects/merchant/MerchantStoreProfile'

type StoreSelectorDialogProps = {
  open: boolean
  selectedStoreId: string | null
  newStoreName: string
  newStoreAddress: string
  stores: MerchantStoreProfile[]
  onOpenChange: (open: boolean) => void
  onSelectStore: (storeId: string) => void
  onChangeStoreName: (value: string) => void
  onChangeStoreAddress: (value: string) => void
  onEnterSelectedStore: () => void
  onCreateStore: () => void
}

export function StoreSelectorDialog({
  open,
  selectedStoreId,
  newStoreName,
  newStoreAddress,
  stores,
  onOpenChange,
  onSelectStore,
  onChangeStoreName,
  onChangeStoreAddress,
  onEnterSelectedStore,
  onCreateStore,
}: StoreSelectorDialogProps) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-2xl rounded-2xl border border-orange-100 bg-white p-6">
        <DialogHeader>
          <DialogTitle>选择店铺</DialogTitle>
          <DialogDescription>可选择已有店铺，或创建新店铺后进入管理。</DialogDescription>
        </DialogHeader>

        <div className="space-y-3">
          <p className="text-sm font-medium text-slate-900">已有店铺</p>
          <div className="grid gap-2">
            {stores.length === 0 ? (
              <p className="text-sm text-slate-500">暂无店铺，请先创建。</p>
            ) : (
              stores.map((storeItem) => (
                <button
                  key={storeItem.merchant.id}
                  type="button"
                  className={`rounded-xl border px-4 py-3 text-left transition-colors ${
                    selectedStoreId === storeItem.merchant.id
                      ? 'border-orange-400 bg-orange-50'
                      : 'border-orange-100 hover:bg-orange-50/60'
                  }`}
                  onClick={() => onSelectStore(storeItem.merchant.id)}
                >
                  <div className="flex items-center justify-between">
                    <p className="font-medium text-slate-900">{storeItem.merchant.storeName}</p>
                    <Badge variant="outline">{storeItem.merchant.category}</Badge>
                  </div>
                  <p className="mt-1 text-xs text-slate-500">{storeItem.merchant.address}</p>
                </button>
              ))
            )}
          </div>
          <div className="flex justify-end">
            <Button type="button" onClick={onEnterSelectedStore} disabled={!selectedStoreId}>
              进入所选店铺
            </Button>
          </div>
        </div>

        <div className="space-y-3 rounded-xl border border-dashed border-orange-200 p-4">
          <p className="text-sm font-medium text-slate-900">创建店铺</p>
          <div className="space-y-2">
            <Label htmlFor="create-store-name">店铺名称</Label>
            <Input
              id="create-store-name"
              value={newStoreName}
              placeholder="请输入店铺名称"
              onChange={(event) => onChangeStoreName(event.target.value)}
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="create-store-address">店铺地址</Label>
            <Input
              id="create-store-address"
              value={newStoreAddress}
              placeholder="请输入店铺地址"
              onChange={(event) => onChangeStoreAddress(event.target.value)}
            />
          </div>
          <Button type="button" variant="outline" onClick={onCreateStore}>
            创建并进入店铺
          </Button>
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            取消
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
