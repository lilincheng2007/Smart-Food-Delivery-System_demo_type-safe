import { useEffect, useRef, useState, type ChangeEvent } from 'react'
import { ChartNoAxesCombined, Megaphone } from 'lucide-react'

import { fetchMerchantReviewsIO } from '@/apis/review/MerchantReviewsAPI'
import { runTask } from '@/apis/shared/client'
import { DeliveryLogoutBar } from '@/components/DeliveryLogoutBar'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { useAppChrome } from '@/hooks/useAppChrome'
import { resolveApiMediaUrl } from '@/lib/api-media-url'
import { getLocalImageFileError } from '@/lib/local-image-file'
import { businessStatusLabels } from '@/lib/merchant-business-hours'
import type { MerchantBusinessStatus, MerchantHolidayBusinessHour, MerchantWeeklyBusinessHour } from '@/objects/merchant/MerchantBusinessHours'
import type { MerchantStoreProfile } from '@/objects/merchant/MerchantStoreProfile'
import { OrderStatuses } from '@/objects/shared/ids'
import { useMerchantConsoleStore } from '@/pages/MerchantConsole/stores/use-merchant-console-store'
import type { MerchantReviewsResponse } from '@/objects/review/apiTypes/MerchantReviewsResponse'

type ProfileTabProps = {
  selectedStore: MerchantStoreProfile | null
  onOpenStoreDialog: () => void
}

type BusinessHoursDraft = {
  merchantId: string | null
  businessStatus: MerchantBusinessStatus
  weeklyBusinessHours: MerchantWeeklyBusinessHour[]
  holidayBusinessHours: MerchantHolidayBusinessHour[]
}

const dayLabels = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
const defaultWeeklyHours = (): MerchantWeeklyBusinessHour[] =>
  dayLabels.map((_, index) => ({ dayOfWeek: index + 1, startTime: '09:00', endTime: '21:00', enabled: index < 5 }))

export function ProfileTab({ selectedStore, onOpenStoreDialog }: ProfileTabProps) {
  const { showNotice } = useAppChrome()
  const updateStoreImage = useMerchantConsoleStore((state) => state.updateStoreImage)
  const uploadStoreImageFile = useMerchantConsoleStore((state) => state.uploadStoreImageFile)
  const saveStoreAnnouncement = useMerchantConsoleStore((state) => state.saveStoreAnnouncement)
  const saveBusinessHours = useMerchantConsoleStore((state) => state.saveBusinessHours)
  const fileInputRef = useRef<HTMLInputElement>(null)
  const [storeImageDraft, setStoreImageDraft] = useState<{ merchantId: string | null; imageUrl: string }>({
    merchantId: null,
    imageUrl: '',
  })
  const [reviews, setReviews] = useState<MerchantReviewsResponse | null>(null)
  const [announcementDraft, setAnnouncementDraft] = useState<{ merchantId: string | null; text: string }>({
    merchantId: null,
    text: '',
  })
  const [businessHoursDraft, setBusinessHoursDraft] = useState<BusinessHoursDraft>({
    merchantId: null,
    businessStatus: 'open',
    weeklyBusinessHours: defaultWeeklyHours(),
    holidayBusinessHours: [],
  })
  const [holidayDraft, setHolidayDraft] = useState<MerchantHolidayBusinessHour>({
    date: '',
    businessStatus: 'closedToday',
    startTime: null,
    endTime: null,
  })
  const selectedMerchantId = selectedStore?.merchant.id ?? null
  const storeImageUrl =
    storeImageDraft.merchantId === selectedMerchantId
      ? storeImageDraft.imageUrl
      : (selectedStore?.merchant.imageUrl?.trim() ?? '')
  const coverUrl = storeImageUrl.trim()
  const announcementText =
    announcementDraft.merchantId === selectedMerchantId
      ? announcementDraft.text
      : (selectedStore?.merchant.announcement ?? '')
  const businessHours =
    businessHoursDraft.merchantId === selectedMerchantId
      ? businessHoursDraft
      : {
          merchantId: selectedMerchantId,
          businessStatus: selectedStore?.merchant.businessStatus ?? 'open',
          weeklyBusinessHours: selectedStore?.merchant.weeklyBusinessHours?.length ? selectedStore.merchant.weeklyBusinessHours : defaultWeeklyHours(),
          holidayBusinessHours: selectedStore?.merchant.holidayBusinessHours ?? [],
        }

  const merchantPendingOrders = selectedStore?.pendingOrders ?? []
  const merchantHistoryOrders = selectedStore?.historyOrders ?? []
  const activeProcessingOrders = merchantPendingOrders.filter(
    (order) => order.status === OrderStatuses.waitingForMerchantAcceptance || order.status === OrderStatuses.cooking,
  )
  const totalTurnover = merchantHistoryOrders.reduce((sum, item) => sum + (item.merchantReceivableAmount ?? item.payableAmount), 0)

  useEffect(() => {
    if (!selectedMerchantId) {
      setReviews(null)
      return
    }
    void runTask(fetchMerchantReviewsIO(selectedMerchantId)).then(setReviews).catch(() => setReviews(null))
  }, [selectedMerchantId])

  const handleSaveStoreImage = async () => {
    if (!selectedStore) {
      showNotice('请先选择店铺。', 'error')
      return
    }

    try {
      await updateStoreImage(selectedStore.merchant.id, storeImageUrl)
      setStoreImageDraft({ merchantId: null, imageUrl: '' })
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

    const fileError = getLocalImageFileError(file)
    if (fileError) {
      showNotice(fileError, 'error')
      return
    }

    try {
      await uploadStoreImageFile(selectedStore.merchant.id, file)
      setStoreImageDraft({ merchantId: null, imageUrl: '' })
      showNotice('本地上传成功，顾客端首页将显示该图片。', 'success')
    } catch (error) {
      showNotice(error instanceof Error ? error.message : '上传失败', 'error')
    }
  }

  const handleSaveAnnouncement = async () => {
    if (!selectedStore) {
      showNotice('请先选择店铺。', 'error')
      return
    }

    try {
      await saveStoreAnnouncement(selectedStore.merchant.id, announcementText)
      setAnnouncementDraft({ merchantId: null, text: '' })
      showNotice(announcementText.trim() ? '店铺公告已发布，顾客端进店页会展示。' : '店铺公告已清空。', 'success')
    } catch (error) {
      showNotice(error instanceof Error ? error.message : '保存公告失败', 'error')
    }
  }

  const updateBusinessHours = (patch: Partial<BusinessHoursDraft>) => {
    setBusinessHoursDraft({ ...businessHours, ...patch, merchantId: selectedMerchantId })
  }

  const updateWeeklyHour = (dayOfWeek: number, patch: Partial<MerchantWeeklyBusinessHour>) => {
    updateBusinessHours({
      weeklyBusinessHours: businessHours.weeklyBusinessHours.map((item) => item.dayOfWeek === dayOfWeek ? { ...item, ...patch } : item),
    })
  }

  const addHolidayHour = () => {
    if (!holidayDraft.date) {
      showNotice('请选择特殊日期。', 'error')
      return
    }
    updateBusinessHours({
      holidayBusinessHours: [
        ...businessHours.holidayBusinessHours.filter((item) => item.date !== holidayDraft.date),
        holidayDraft,
      ].sort((a, b) => a.date.localeCompare(b.date)),
    })
    setHolidayDraft({ date: '', businessStatus: 'closedToday', startTime: null, endTime: null })
  }

  const handleSaveBusinessHours = async () => {
    if (!selectedStore) {
      showNotice('请先选择店铺。', 'error')
      return
    }
    try {
      await saveBusinessHours({
        merchantId: selectedStore.merchant.id,
        businessStatus: businessHours.businessStatus,
        weeklyBusinessHours: businessHours.weeklyBusinessHours,
        holidayBusinessHours: businessHours.holidayBusinessHours,
      })
      setBusinessHoursDraft({ merchantId: null, businessStatus: 'open', weeklyBusinessHours: defaultWeeklyHours(), holidayBusinessHours: [] })
      showNotice('营业状态和营业时间已保存。', 'success')
    } catch (error) {
      showNotice(error instanceof Error ? error.message : '保存营业时间失败', 'error')
    }
  }

  return (
    <div className="space-y-4">
      <Card className="border-orange-100 bg-white/95">
        <CardHeader>
          <CardTitle>营业状态与时间</CardTitle>
          <CardDescription>顾客端会根据这里判断是否可下单；休息中、今日打烊或暂停接单时不可下单。</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="space-y-2">
            <Label>当前状态</Label>
            <select
              className="h-10 w-full rounded-md border border-input bg-background px-3 text-sm"
              value={businessHours.businessStatus}
              disabled={!selectedStore}
              onChange={(event) => updateBusinessHours({ businessStatus: event.target.value as MerchantBusinessStatus })}
            >
              {Object.entries(businessStatusLabels).map(([value, label]) => (
                <option key={value} value={value}>{label}</option>
              ))}
            </select>
          </div>
          <div className="space-y-2">
            <p className="text-sm font-medium text-slate-900">每周营业时间</p>
            <div className="grid gap-2">
              {businessHours.weeklyBusinessHours.map((item) => (
                <div key={item.dayOfWeek} className="grid gap-2 rounded-xl border border-orange-100 p-3 md:grid-cols-[5rem_1fr_1fr_5rem] md:items-center">
                  <span className="text-sm font-medium">{dayLabels[item.dayOfWeek - 1]}</span>
                  <Input type="time" value={item.startTime} onChange={(event) => updateWeeklyHour(item.dayOfWeek, { startTime: event.target.value })} />
                  <Input type="time" value={item.endTime} onChange={(event) => updateWeeklyHour(item.dayOfWeek, { endTime: event.target.value })} />
                  <label className="flex items-center gap-2 text-sm">
                    <input type="checkbox" checked={item.enabled} onChange={(event) => updateWeeklyHour(item.dayOfWeek, { enabled: event.target.checked })} />
                    启用
                  </label>
                </div>
              ))}
            </div>
          </div>
          <div className="space-y-2 rounded-xl border border-dashed border-orange-200 p-3">
            <p className="text-sm font-medium text-slate-900">节假日特殊营业时间</p>
            <div className="grid gap-2 md:grid-cols-[1fr_1fr_1fr_1fr_auto] md:items-end">
              <Input type="date" value={holidayDraft.date} onChange={(event) => setHolidayDraft({ ...holidayDraft, date: event.target.value })} />
              <select className="h-10 rounded-md border border-input bg-background px-3 text-sm" value={holidayDraft.businessStatus} onChange={(event) => setHolidayDraft({ ...holidayDraft, businessStatus: event.target.value as MerchantBusinessStatus })}>
                {Object.entries(businessStatusLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
              </select>
              <Input type="time" value={holidayDraft.startTime ?? ''} onChange={(event) => setHolidayDraft({ ...holidayDraft, startTime: event.target.value || null })} />
              <Input type="time" value={holidayDraft.endTime ?? ''} onChange={(event) => setHolidayDraft({ ...holidayDraft, endTime: event.target.value || null })} />
              <Button type="button" variant="outline" onClick={addHolidayHour}>添加</Button>
            </div>
            {businessHours.holidayBusinessHours.length > 0 ? (
              <div className="flex flex-wrap gap-2">
                {businessHours.holidayBusinessHours.map((item) => (
                  <button key={item.date} type="button" className="rounded-full border border-orange-100 px-3 py-1 text-xs text-slate-600" onClick={() => updateBusinessHours({ holidayBusinessHours: businessHours.holidayBusinessHours.filter((current) => current.date !== item.date) })}>
                    {item.date} · {businessStatusLabels[item.businessStatus]} {item.startTime && item.endTime ? `${item.startTime}-${item.endTime}` : ''} · 点击删除
                  </button>
                ))}
              </div>
            ) : null}
          </div>
          <Button type="button" disabled={!selectedStore} onClick={() => void handleSaveBusinessHours()}>保存营业设置</Button>
        </CardContent>
      </Card>

      <Card className="border-orange-100 bg-white/95">
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Megaphone className="size-5 text-orange-500" />
            店铺公告
          </CardTitle>
          <CardDescription>公告会展示在顾客端进店页的显眼位置。可填写促销、营业调整、出餐提醒等内容，留空保存可清除。</CardDescription>
        </CardHeader>
        <CardContent className="space-y-3">
          <div className="space-y-2">
            <Label htmlFor="store-announcement">公告内容</Label>
            <Textarea
              id="store-announcement"
              value={announcementText}
              maxLength={180}
              placeholder="例如：今日满 50 减 8；晚高峰出餐约 20 分钟，请合理安排下单时间。"
              disabled={!selectedStore}
              onChange={(event) => setAnnouncementDraft({ merchantId: selectedMerchantId, text: event.target.value })}
            />
            <div className="flex items-center justify-between gap-3">
              <span className="text-xs text-slate-500">{announcementText.trim().length}/180</span>
              <Button type="button" disabled={!selectedStore} onClick={() => void handleSaveAnnouncement()}>
                保存公告
              </Button>
            </div>
          </div>
          {announcementText.trim() ? (
            <div className="rounded-xl border border-orange-100 bg-orange-50 px-3 py-2 text-sm leading-6 text-orange-800">
              顾客端预览：{announcementText.trim()}
            </div>
          ) : null}
        </CardContent>
      </Card>

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
              onChange={(event) => setStoreImageDraft({ merchantId: selectedMerchantId, imageUrl: event.target.value })}
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
                <span>{activeProcessingOrders.length}</span>
              </div>
              <div className="flex items-center justify-between rounded-xl border border-orange-100 p-3">
                <span>历史订单</span>
                <span>{merchantHistoryOrders.length}</span>
              </div>
              <div className="flex items-center justify-between rounded-xl border border-orange-100 p-3">
                <span>总成交额</span>
                <span>{totalTurnover} 元</span>
              </div>
              <div className="flex items-center justify-between rounded-xl border border-orange-100 p-3">
                <span>当前评分</span>
                <span>★ {(reviews?.summary.averageRating ?? selectedStore.merchant.rating).toFixed(1)} / {reviews?.summary.reviewCount ?? 0} 条评价</span>
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
