import { useEffect, useRef, useState, type ChangeEvent } from 'react'
import { ChartNoAxesCombined } from 'lucide-react'

import { DeliveryLogoutBar } from '@/components/DeliveryLogoutBar'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { useAppChrome } from '@/hooks/useAppChrome'
import { resolveApiMediaUrl } from '@/lib/api-media-url'
import type { MerchantStoreProfile } from '@/objects/merchant'
import { useMerchantConsoleStore } from '@/stores/pages/use-merchant-console-store'

type ProfileTabProps = {
  selectedStore: MerchantStoreProfile | null
  onOpenStoreDialog: () => void
}

export function ProfileTab({ selectedStore, onOpenStoreDialog }: ProfileTabProps) {
  const { showNotice } = useAppChrome()
  const updateStoreImage = useMerchantConsoleStore((state) => state.updateStoreImage)
  const uploadStoreImageFile = useMerchantConsoleStore((state) => state.uploadStoreImageFile)
  const fileInputRef = useRef<HTMLInputElement>(null)
  const [storeImageUrl, setStoreImageUrl] = useState('')

  useEffect(() => {
    setStoreImageUrl(selectedStore?.merchant.imageUrl?.trim() ?? '')
  }, [selectedStore?.merchant.id, selectedStore?.merchant.imageUrl])

  /** 预览与输入框一致：本地上传后接口写入 merchant，输入框也可能暂存链接；避免仅 merchant 未同步时预览空白 */
  const coverUrl =
    (selectedStore?.merchant.imageUrl ?? '').trim() || storeImageUrl.trim()

  const merchantPendingOrders = selectedStore?.pendingOrders ?? []
  const merchantHistoryOrders = selectedStore?.historyOrders ?? []
  const activeCookingOrders = merchantPendingOrders.filter((order) => order.status === '制作中')
  const historyOrders = [...merchantPendingOrders.filter((order) => order.status !== '制作中'), ...merchantHistoryOrders]
  const totalTurnover = historyOrders.reduce((sum, item) => sum + item.totalAmount, 0)

  const handleSaveStoreImage = async () => {
    if (!selectedStore) {
      showNotice('请先选择店铺。', 'error')
      return
    }

    try {
      await updateStoreImage(selectedStore.merchant.id, storeImageUrl)
      showNotice('店铺图片已保存，顾客端首页将展示该链接图片。', 'success')
    } catch (error) {
      showNotice(error instanceof Error ? error.message : '保存失败', 'error')
    }
  }

  const handleLocalFileChange = async (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0]
    event.target.value = ''
    if (!file || !selectedStore) {
      return
    }

    if (!file.type.startsWith('image/')) {
      showNotice('请选择图片文件。', 'error')
      return
    }

    if (file.size > 2 * 1024 * 1024) {
      showNotice('图片不能超过 2MB。', 'error')
      return
    }

    try {
      await uploadStoreImageFile(selectedStore.merchant.id, file)
      showNotice('本地上传成功，顾客端首页将显示该图片。', 'success')
    } catch (error) {
      showNotice(error instanceof Error ? error.message : '上传失败', 'error')
    }
  }

  return (
    <div className="space-y-4">
      <Card className="border-orange-100 bg-white/95">
        <CardHeader>
          <CardTitle>店铺图片</CardTitle>
          <CardDescription>
            支持本地上传（JPEG/PNG/GIF/WebP，最大 2MB）或填写 http(s) 图片链接；将显示在顾客端首页商家卡片上。留空链接并保存可清除。
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <input
            ref={fileInputRef}
            type="file"
            accept="image/jpeg,image/png,image/gif,image/webp,.jpg,.jpeg,.png,.gif,.webp"
            className="sr-only"
            onChange={(e) => void handleLocalFileChange(e)}
          />
          {coverUrl ? (
            <div className="mx-auto aspect-square w-full max-w-xs overflow-hidden rounded-xl border border-orange-100">
              <img
                src={resolveApiMediaUrl(coverUrl)}
                alt={selectedStore ? `${selectedStore.merchant.storeName} 店铺` : '店铺'}
                className="size-full object-cover"
              />
            </div>
          ) : (
            <p className="text-sm text-slate-500">当前未设置店铺图片。</p>
          )}
          <div className="space-y-2">
            <Label htmlFor="store-image-url">图片链接</Label>
            <Input
              id="store-image-url"
              type="text"
              inputMode="url"
              placeholder="https://example.com/your-store-cover.jpg"
              value={storeImageUrl}
              onChange={(event) => setStoreImageUrl(event.target.value)}
              disabled={!selectedStore}
            />
          </div>
          <div className="flex flex-wrap gap-2">
            <Button
              type="button"
              variant="secondary"
              disabled={!selectedStore}
              onClick={() => fileInputRef.current?.click()}
            >
              从本地上传
            </Button>
            <Button type="button" onClick={() => void handleSaveStoreImage()} disabled={!selectedStore}>
              保存链接
            </Button>
          </div>
        </CardContent>
      </Card>

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
                <span>{activeCookingOrders.length}</span>
              </div>
              <div className="flex items-center justify-between rounded-xl border border-orange-100 p-3">
                <span>历史订单</span>
                <span>{historyOrders.length}</span>
              </div>
              <div className="flex items-center justify-between rounded-xl border border-orange-100 p-3">
                <span>总成交额</span>
                <span>{totalTurnover} 元</span>
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

      <DeliveryLogoutBar />
    </div>
  )
}
