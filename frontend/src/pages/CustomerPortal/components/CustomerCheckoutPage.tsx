import { useEffect, useMemo, useState } from 'react'
import { ArrowLeft, ClipboardList, ImagePlus, MapPin, Phone, TicketPercent, User } from 'lucide-react'
import { useNavigate, useSearchParams } from 'react-router-dom'

import { DeliveryPageShell } from '@/components/DeliveryPageShell'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Label } from '@/components/ui/label'
import { Input } from '@/components/ui/input'
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group'
import { Textarea } from '@/components/ui/textarea'
import { useAppChrome } from '@/hooks/useAppChrome'
import { appendContact, normalizedDeliveryContacts } from '@/lib/deliveryContacts'
import { resolveApiMediaUrl } from '@/lib/api-media-url'
import { bundleLineUnitPrice, bundleSelectionSummary } from '@/lib/bundles'
import { bestPromotion, promotionDisplayName, promotionSummary, roundMoney } from '@/lib/promotions'
import type { CheckoutBundleSelection } from '@/objects/order/CheckoutLine'
import type { MerchantId } from '@/objects/shared/ids'
import { useCustomerPortalStore } from '@/stores/pages/use-customer-portal-store'

import { DeliveryContactAddDialog } from './DeliveryContactAddDialog'

const checkoutLineKey = (line: { merchantId: string; productId: string; bundleSelections?: CheckoutBundleSelection[] }) =>
  `${line.merchantId}::${line.productId}::${JSON.stringify(line.bundleSelections ?? [])}`

export default function CustomerCheckoutPage() {
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const { showNotice } = useAppChrome()
  const [submitting, setSubmitting] = useState(false)
  const [addContactOpen, setAddContactOpen] = useState(false)
  const [selectedId, setSelectedId] = useState('')
  const [selectedVoucherId, setSelectedVoucherId] = useState('')
  const [merchantNoteTextById, setMerchantNoteTextById] = useState<Record<string, string>>({})
  const [merchantNoteImageById, setMerchantNoteImageById] = useState<Record<string, string>>({})

  const bootstrapDone = useCustomerPortalStore((s) => s.bootstrapDone)
  const loadError = useCustomerPortalStore((s) => s.loadError)
  const customerAccount = useCustomerPortalStore((s) => s.customerAccount)
  const merchants = useCustomerPortalStore((s) => s.merchants)
  const products = useCustomerPortalStore((s) => s.products)
  const platformPromotions = useCustomerPortalStore((s) => s.platformPromotions)
  const cartLines = useCustomerPortalStore((s) => s.cartLines)
  const walletBalance = useCustomerPortalStore((s) => s.walletBalance)
  const checkout = useCustomerPortalStore((s) => s.checkout)
  const uploadOrderImage = useCustomerPortalStore((s) => s.uploadOrderImage)
  const setActiveTab = useCustomerPortalStore((s) => s.setActiveTab)
  const saveDeliveryContacts = useCustomerPortalStore((s) => s.saveDeliveryContacts)

  const merchantIdParam = searchParams.get('merchantId') as MerchantId | null

  useEffect(() => {
    void (async () => {
      const st = useCustomerPortalStore.getState()
      if (!st.bootstrapDone && !st.loadError) {
        await st.bootstrap()
      }
    })()
  }, [])

  const lines = useMemo(() => {
    if (!merchantIdParam) return cartLines
    return cartLines.filter((l) => l.merchantId === merchantIdParam)
  }, [cartLines, merchantIdParam])

  const total = useMemo(() => {
    return lines.reduce((sum, line) => {
      const p = products.find((x) => x.id === line.productId)
      return sum + (p ? bundleLineUnitPrice(p, line.bundleSelections, products) * line.quantity : 0)
    }, 0)
  }, [lines, products])
  const itemCount = useMemo(() => lines.reduce((sum, line) => sum + line.quantity, 0), [lines])
  const merchantDiscounts = useMemo(() => {
    return [...new Set(lines.map((line) => line.merchantId))].map((merchantId) => {
      const merchantLines = lines.filter((line) => line.merchantId === merchantId)
      const merchantTotal = merchantLines.reduce((sum, line) => {
        const product = products.find((item) => item.id === line.productId)
        return sum + (product ? bundleLineUnitPrice(product, line.bundleSelections, products) * line.quantity : 0)
      }, 0)
      const merchantItemCount = merchantLines.reduce((sum, line) => sum + line.quantity, 0)
      const merchant = merchants.find((item) => item.id === merchantId)
      const promotionLines = merchantLines.flatMap((line) => {
        const product = products.find((item) => item.id === line.productId)
        return product ? [{ productId: line.productId, unitPrice: bundleLineUnitPrice(product, line.bundleSelections, products), quantity: line.quantity }] : []
      })
      return {
        merchantId,
        merchantName: merchant?.storeName ?? '未知商家',
        applied: bestPromotion(merchant?.promotions, merchantTotal, merchantItemCount, promotionLines),
      }
    }).filter((item) => item.applied)
  }, [lines, merchants, products])
  const merchantDiscount = roundMoney(merchantDiscounts.reduce((sum, item) => sum + (item.applied?.discountAmount ?? 0), 0))
  const afterMerchantDiscount = Math.max(0, roundMoney(total - merchantDiscount))

  const vouchers = customerAccount?.profile.vouchers ?? []
  const todayStart = useMemo(() => {
    const now = new Date()
    return new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime()
  }, [])
  const isVoucherExpired = (expiresAt: string) => {
    const time = Date.parse(`${expiresAt}T00:00:00`)
    return Number.isNaN(time) || time < todayStart
  }
  const usableVouchers = vouchers.filter(
    (voucher) => voucher.remainingCount > 0 && afterMerchantDiscount >= voucher.minSpend && !isVoucherExpired(voucher.expiresAt),
  )
  const checkoutVouchers = vouchers.filter((voucher) => !isVoucherExpired(voucher.expiresAt))
  const selectedVoucher = usableVouchers.find((voucher) => voucher.id === selectedVoucherId)
  const voucherDiscount = selectedVoucher ? Math.min(selectedVoucher.discountAmount, afterMerchantDiscount) : 0
  const promotionLines = lines.flatMap((line) => {
    const product = products.find((item) => item.id === line.productId)
    return product ? [{ productId: line.productId, unitPrice: bundleLineUnitPrice(product, line.bundleSelections, products), quantity: line.quantity }] : []
  })
  const platformPromotion = bestPromotion(platformPromotions, Math.max(0, afterMerchantDiscount - voucherDiscount), itemCount, promotionLines)
  const platformDiscount = platformPromotion?.discountAmount ?? 0
  const discount = roundMoney(merchantDiscount + voucherDiscount + platformDiscount)
  const payable = Math.max(0, roundMoney(total - discount))
  const estimatedPoints = Math.floor(payable)
  const insufficient = walletBalance < payable
  const merchantIdsInOrder = useMemo(() => [...new Set(lines.map((line) => line.merchantId))], [lines])

  useEffect(() => {
    if (!bootstrapDone || loadError) return
    if (!customerAccount) {
      navigate('/delivery/customer', { replace: true })
      return
    }
    if (lines.length === 0) {
      navigate('/delivery/customer', { replace: true })
    }
  }, [bootstrapDone, customerAccount, lines.length, loadError, navigate])

  useEffect(() => {
    if (selectedVoucherId && !usableVouchers.some((voucher) => voucher.id === selectedVoucherId)) {
      setSelectedVoucherId('')
    }
  }, [selectedVoucherId, usableVouchers])

  const handleBack = () => {
    if (merchantIdParam) {
      navigate(`/delivery/customer/m/${encodeURIComponent(merchantIdParam)}`)
    } else {
      setActiveTab('cart')
      navigate('/delivery/customer')
    }
  }

  const contacts = useMemo(() => {
    if (!customerAccount) {
      return []
    }
    return normalizedDeliveryContacts(customerAccount.profile)
  }, [customerAccount])

  useEffect(() => {
    if (contacts.length === 0) {
      return
    }
    setSelectedId((prev) => {
      if (prev && contacts.some((c) => c.id === prev)) {
        return prev
      }
      return contacts.find((c) => c.isDefault)?.id ?? contacts[0]?.id ?? ''
    })
  }, [contacts])

  const handleConfirm = async () => {
    if (insufficient || lines.length === 0 || submitting) return
    const selected = contacts.find((c) => c.id === selectedId) ?? contacts[0]
    if (!selected) {
      showNotice('请选择一组收货信息。', 'error')
      return
    }
    const delivery = {
      customerName: selected.name,
      customerPhone: selected.phone,
      deliveryAddress: selected.address,
    }
    setSubmitting(true)
    try {
      const voucherId = selectedVoucherId || undefined
      const merchantNotes = merchantIdsInOrder
        .map((merchantId) => ({
          merchantId,
          text: merchantNoteTextById[merchantId]?.trim() || null,
          imageUrl: merchantNoteImageById[merchantId] || null,
        }))
        .filter((note) => note.text || note.imageUrl)
      const result = merchantIdParam
        ? await checkout({ merchantId: merchantIdParam, delivery, voucherId, merchantNotes })
        : await checkout({ delivery, voucherId, merchantNotes })
      if (result.ok) {
        showNotice(`结算成功，已提交 ${result.createdCount} 笔订单，等待商家接单。`, 'success')
        setActiveTab('profile')
        navigate('/delivery/customer')
        return
      }
      showNotice(result.message, 'error')
    } finally {
      setSubmitting(false)
    }
  }

  const handleNoteImageUpload = async (merchantId: MerchantId, file: File | undefined) => {
    if (!file) return
    const result = await uploadOrderImage(file)
    if (result.ok) {
      setMerchantNoteImageById((current) => ({ ...current, [merchantId]: result.imageUrl }))
      showNotice('备注图片已上传。', 'success')
      return
    }
    showNotice(result.message, 'error')
  }

  const voucherUnavailableReason = (voucher: (typeof vouchers)[number]) => {
    if (voucher.remainingCount <= 0) return '已使用完'
    if (isVoucherExpired(voucher.expiresAt)) return '已过期'
    if (afterMerchantDiscount < voucher.minSpend) return `还差 ¥${(voucher.minSpend - afterMerchantDiscount).toFixed(2)} 可用`
    return null
  }

  if (!bootstrapDone) {
    return (
      <DeliveryPageShell>
        <Card className="border-border/70 bg-card/90 backdrop-blur-sm">
          <CardContent className="p-6 text-sm text-muted-foreground">加载中…</CardContent>
        </Card>
      </DeliveryPageShell>
    )
  }

  if (loadError || !customerAccount || lines.length === 0) {
    return (
      <DeliveryPageShell>
        <Card className="border-border/70 bg-card/90 backdrop-blur-sm">
          <CardContent className="p-6 text-sm text-muted-foreground">正在跳转…</CardContent>
        </Card>
      </DeliveryPageShell>
    )
  }

  return (
    <DeliveryPageShell>
      <div className="mx-auto max-w-2xl space-y-5">
        <Button type="button" variant="outline" className="cursor-pointer" onClick={handleBack}>
          <ArrowLeft className="mr-2 size-4" aria-hidden />
          返回修改
        </Button>

        <div className="space-y-1">
          <h1 className="text-2xl font-semibold tracking-tight text-foreground">确认订单</h1>
          <p className="text-sm text-muted-foreground">
            请核对收货信息与商品明细，确认后将创建订单并从钱包扣款（本页不可修改购物车）。
          </p>
        </div>

        <Card className="border-border/70 bg-card/90 shadow-sm">
          <CardHeader className="flex flex-row flex-wrap items-start justify-between gap-3 pb-3">
            <div className="space-y-1">
              <CardTitle className="flex items-center gap-2 text-lg">
                <User className="size-5 text-primary" aria-hidden />
                收货信息
              </CardTitle>
              <CardDescription>选择一组档案中的收货信息用于本单；也可新增并写入档案后再选用。</CardDescription>
            </div>
            <Button type="button" size="sm" variant="outline" className="cursor-pointer shrink-0" onClick={() => setAddContactOpen(true)}>
              新增收货信息
            </Button>
          </CardHeader>
          <CardContent className="space-y-4 text-sm">
            <RadioGroup value={selectedId} onValueChange={setSelectedId} className="gap-3">
              {contacts.map((c) => {
                const inputId = `checkout-dc-${c.id}`
                return (
                  <div
                    key={c.id}
                    className="flex gap-3 rounded-xl border border-border/60 bg-muted/15 px-3 py-3 shadow-sm"
                  >
                    <RadioGroupItem value={c.id} id={inputId} className="mt-1 shrink-0" />
                    <Label htmlFor={inputId} className="min-w-0 flex-1 cursor-pointer space-y-2 leading-snug">
                      <div className="flex flex-wrap items-center gap-2">
                        <span className="font-medium text-foreground">{c.name}</span>
                        {c.isDefault ? (
                          <Badge variant="secondary" className="text-xs">
                            默认
                          </Badge>
                        ) : null}
                      </div>
                      <p className="flex items-center gap-1.5 text-muted-foreground">
                        <Phone className="size-3.5 shrink-0" aria-hidden />
                        {c.phone}
                      </p>
                      <p className="flex items-start gap-1.5 text-muted-foreground">
                        <MapPin className="mt-0.5 size-3.5 shrink-0" aria-hidden />
                        <span className="leading-relaxed">{c.address}</span>
                      </p>
                    </Label>
                  </div>
                )
              })}
            </RadioGroup>
          </CardContent>
        </Card>

        <DeliveryContactAddDialog
          open={addContactOpen}
          onOpenChange={setAddContactOpen}
          title="新增收货信息"
          description="保存后将写入「我的」中的收货信息组，并自动选为本单使用。"
          onSubmit={async (payload) => {
            const newId = crypto.randomUUID()
            const next = appendContact(contacts, payload, newId)
            const result = await saveDeliveryContacts(next)
            if (result.ok) {
              setSelectedId(newId)
              showNotice('已保存并选中新的收货信息。', 'success')
              return { ok: true as const }
            }
            return { ok: false as const, message: result.message }
          }}
        />

        <Card className="border-border/70 bg-card/90 shadow-sm">
          <CardHeader className="pb-3">
            <CardTitle className="flex items-center gap-2 text-lg">
              <ClipboardList className="size-5 text-primary" aria-hidden />
              商品明细
            </CardTitle>
            <CardDescription>只读展示，不可在本页增删改</CardDescription>
          </CardHeader>
          <CardContent className="space-y-3">
            {lines.map((line) => {
              const product = products.find((p) => p.id === line.productId)
              const merchant = merchants.find((m) => m.id === line.merchantId)
              if (!product || !merchant) return null
              const unitPrice = bundleLineUnitPrice(product, line.bundleSelections, products)
              const subtotal = unitPrice * line.quantity
              const selectionSummary = bundleSelectionSummary(product, line.bundleSelections, products)
              return (
                <div
                  key={checkoutLineKey(line)}
                  className="rounded-xl border border-border/60 bg-muted/20 px-4 py-3"
                >
                  <div className="flex flex-wrap items-start justify-between gap-2">
                    <div className="min-w-0 space-y-1">
                      <p className="font-medium text-foreground">{product.name}</p>
                      <p className="text-xs text-muted-foreground">{merchant.storeName}</p>
                      {selectionSummary ? <p className="line-clamp-2 text-xs text-amber-600">{selectionSummary}</p> : null}
                    </div>
                    <Badge variant="outline" className="shrink-0 text-xs">
                      ×{line.quantity}
                    </Badge>
                  </div>
                  <div className="mt-2 flex flex-wrap items-center justify-between gap-2 text-sm text-muted-foreground">
                    <span>单价 ¥{unitPrice.toFixed(1)}</span>
                    <span className="font-semibold tabular-nums text-primary">小计 ¥{subtotal.toFixed(1)}</span>
                  </div>
                </div>
              )
            })}
          </CardContent>
        </Card>

        <Card className="border-border/70 bg-card/90 shadow-sm">
          <CardHeader className="pb-3">
            <CardTitle className="text-lg">订单备注</CardTitle>
            <CardDescription>可分别给每个商家填写备注，商家和骑手会在订单上看到。</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            {merchantIdsInOrder.map((merchantId) => {
              const merchant = merchants.find((item) => item.id === merchantId)
              const imageUrl = merchantNoteImageById[merchantId]
              return (
                <div key={merchantId} className="space-y-3 rounded-xl border border-border/60 bg-muted/20 px-4 py-3">
                  <Label htmlFor={`merchant-note-${merchantId}`} className="font-semibold text-foreground">
                    {merchant?.storeName ?? '商家'}备注
                  </Label>
                  <Textarea
                    id={`merchant-note-${merchantId}`}
                    value={merchantNoteTextById[merchantId] ?? ''}
                    className="min-h-24 resize-y bg-background"
                    placeholder="可填写口味、餐具、配送等备注"
                    onChange={(event) => setMerchantNoteTextById((current) => ({ ...current, [merchantId]: event.target.value }))}
                  />
                  <div className="flex flex-wrap items-center gap-2">
                    <Input
                      id={`merchant-note-image-${merchantId}`}
                      type="file"
                      accept="image/jpeg,image/png,image/gif,image/webp"
                      className="hidden"
                      onChange={(event) => void handleNoteImageUpload(merchantId, event.target.files?.[0])}
                    />
                    <Button type="button" variant="outline" size="sm" onClick={() => document.getElementById(`merchant-note-image-${merchantId}`)?.click()}>
                      <ImagePlus className="size-4" />
                      上传备注图片
                    </Button>
                    {imageUrl ? <span className="text-xs text-muted-foreground">已上传图片</span> : null}
                  </div>
                  {imageUrl ? <img src={resolveApiMediaUrl(imageUrl)} alt="备注图片" className="aspect-video w-full max-w-xs rounded-xl object-cover" /> : null}
                </div>
              )
            })}
          </CardContent>
        </Card>

        <Card className="overflow-hidden border-orange-200/70 bg-gradient-to-br from-orange-50/95 via-card to-rose-50/80 shadow-[0_18px_45px_rgba(249,115,22,0.12)]">
          <CardHeader className="pb-3">
            <CardTitle className="flex items-center gap-2 text-lg">
              <TicketPercent className="size-5 text-orange-500" aria-hidden />
              优惠券
            </CardTitle>
            <CardDescription>每单最多使用 1 张优惠券，最终优惠以后端结算为准。</CardDescription>
          </CardHeader>
          <CardContent className="space-y-3">
            {merchantDiscounts.length > 0 || platformPromotion ? (
              <div className="space-y-2 rounded-2xl border border-orange-200 bg-white/75 px-4 py-3 text-sm">
                {merchantDiscounts.map((item) => (
                  <p key={item.merchantId} className="text-orange-700">
                    {item.merchantName}：{item.applied ? `${promotionDisplayName(item.applied.promotion)} · ${promotionSummary(item.applied.promotion)}` : ''}，已减 ¥{item.applied?.discountAmount.toFixed(2)}
                  </p>
                ))}
                {platformPromotion ? (
                  <p className="text-green-700">
                    平台优惠：{promotionDisplayName(platformPromotion.promotion)} · {promotionSummary(platformPromotion.promotion)}，已减 ¥{platformPromotion.discountAmount.toFixed(2)}
                  </p>
                ) : null}
              </div>
            ) : null}
            <div className="space-y-3">
              {checkoutVouchers.length === 0 ? (
                <p className="rounded-xl border border-dashed border-orange-200 bg-white/60 px-4 py-3 text-sm text-muted-foreground">
                  暂无未过期优惠券。确认完成订单升级后可继续获得平台满30减10优惠券。
                </p>
              ) : null}
              {checkoutVouchers.map((voucher) => {
                const reason = voucherUnavailableReason(voucher)
                const disabled = reason !== null
                const selected = selectedVoucherId === voucher.id && !disabled
                return (
                  <div
                    key={voucher.id}
                    className={`relative overflow-hidden rounded-2xl border px-4 py-3 shadow-sm transition-[transform,border-color,box-shadow] duration-200 ${
                      selected
                        ? 'border-orange-400 bg-orange-50 text-orange-950 shadow-[0_14px_30px_rgba(249,115,22,0.18)]'
                        : disabled
                        ? 'border-border/50 bg-muted/35 text-muted-foreground opacity-70'
                        : 'cursor-pointer border-orange-200 bg-white/85 hover:-translate-y-0.5 hover:border-orange-300 hover:shadow-[0_14px_30px_rgba(249,115,22,0.16)]'
                    }`}
                  >
                    <div className="pointer-events-none absolute -right-5 top-1/2 size-10 -translate-y-1/2 rounded-full bg-orange-50" />
                    <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                      <div className="min-w-0 flex-1">
                        <span className="block text-base font-semibold text-foreground">{voucher.title}</span>
                        <span className="mt-1 block text-xs text-muted-foreground">
                          满 ¥{voucher.minSpend.toFixed(0)} 减 ¥{voucher.discountAmount.toFixed(0)} · 有效期至 {voucher.expiresAt}
                        </span>
                        <span className="mt-1 block text-xs text-muted-foreground">剩余 {voucher.remainingCount} 次</span>
                        <span
                          className={`mt-2 inline-flex rounded-full px-2 py-0.5 text-xs font-medium ${
                            selected
                              ? 'bg-orange-600 text-white'
                              : reason
                              ? 'bg-muted text-muted-foreground'
                              : 'bg-emerald-50 text-emerald-700'
                          }`}
                        >
                          {selected ? '当前已使用' : reason ? `不可用：${reason}` : '当前未使用'}
                        </span>
                      </div>
                      <Button
                        type="button"
                        size="sm"
                        disabled={disabled}
                        className={selected ? 'bg-orange-600 text-white hover:bg-orange-600' : 'cursor-pointer'}
                        variant={selected ? 'default' : 'outline'}
                        onClick={() => setSelectedVoucherId(selected ? '' : voucher.id)}
                      >
                        {selected ? '取消使用' : disabled ? '无法使用' : '使用此券'}
                      </Button>
                    </div>
                  </div>
                )
              })}
            </div>
          </CardContent>
        </Card>

        <Card className="border-border/70 bg-card/90 shadow-sm">
          <CardHeader className="pb-3">
            <CardTitle className="text-lg">金额</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3 text-sm">
            <div className="flex items-center justify-between">
              <span className="text-muted-foreground">商品总金额</span>
              <span className="tabular-nums text-foreground">¥{total.toFixed(2)}</span>
            </div>
            <div className="flex items-center justify-between text-muted-foreground">
              <span>商家优惠</span>
              <span className="tabular-nums text-green-600">-¥{merchantDiscount.toFixed(2)}</span>
            </div>
            <div className="flex items-center justify-between text-muted-foreground">
              <span>平台/优惠券抵扣</span>
              <span className="tabular-nums text-green-600">-¥{(voucherDiscount + platformDiscount).toFixed(2)}</span>
            </div>
            <div className="flex items-center justify-between text-muted-foreground">
              <span>优惠抵扣合计</span>
              <span className="tabular-nums text-green-600">-¥{discount.toFixed(2)}</span>
            </div>
            <div className="flex items-center justify-between rounded-xl bg-orange-50 px-3 py-2">
              <span className="font-medium text-orange-900">实付金额</span>
              <span className="text-xl font-semibold tabular-nums text-primary">¥{payable.toFixed(2)}</span>
            </div>
            <div className="flex items-center justify-between text-muted-foreground">
              <span>预计获得积分</span>
              <span className="tabular-nums text-orange-600">+{estimatedPoints}</span>
            </div>
            <div className="flex items-center justify-between text-muted-foreground">
              <span>当前钱包余额</span>
              <span className="tabular-nums">¥{walletBalance.toFixed(2)}</span>
            </div>
            {insufficient ? (
              <p className="rounded-lg border border-destructive/40 bg-destructive/10 px-3 py-2 text-sm text-destructive">
                余额不足，请先充值后再结算。
              </p>
            ) : null}
          </CardContent>
        </Card>

        <div className="flex flex-col gap-3 pb-8 sm:flex-row sm:justify-end">
          <Button type="button" variant="outline" className="h-11 min-w-[8rem] cursor-pointer sm:order-1" onClick={handleBack}>
            返回修改
          </Button>
          <Button
            type="button"
            className="h-11 min-w-[10rem] cursor-pointer bg-gradient-to-r from-primary to-[oklch(0.62_0.18_45)] text-base font-semibold text-primary-foreground shadow-md disabled:opacity-50 dark:to-[oklch(0.68_0.14_45)]"
            disabled={insufficient || submitting}
            onClick={() => void handleConfirm()}
          >
            {submitting ? '提交中…' : `确认支付 ¥${payable.toFixed(2)}`}
          </Button>
        </div>
      </div>
    </DeliveryPageShell>
  )
}
