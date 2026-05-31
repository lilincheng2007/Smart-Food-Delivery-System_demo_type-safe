import { CheckCircle2, Sparkles, Wand2 } from 'lucide-react'
import { useState } from 'react'

import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { useAppChrome } from '@/hooks/useAppChrome'
import type { AIMerchantProductDescriptionsResponse } from '@/objects/ai/AIMerchantProductDescriptionsResponse'
import type { AIMerchantStoreDescriptionResponse } from '@/objects/ai/AIMerchantStoreDescriptionResponse'
import type { MerchantStoreProfile } from '@/objects/merchant/MerchantStoreProfile'
import { useMerchantConsoleStore } from '@/stores/pages/use-merchant-console-store'

type MerchantAICopywritingCardProps = {
  selectedStore: MerchantStoreProfile | null
}

type LoadingAction = 'store-generate' | 'store-save' | 'product-generate' | 'product-save' | null

export function MerchantAICopywritingCard({ selectedStore }: MerchantAICopywritingCardProps) {
  const { showNotice } = useAppChrome()
  const generateStoreDescription = useMerchantConsoleStore((state) => state.generateStoreDescription)
  const saveStoreDescription = useMerchantConsoleStore((state) => state.saveStoreDescription)
  const generateProductDescriptions = useMerchantConsoleStore((state) => state.generateProductDescriptions)
  const saveProductDescriptions = useMerchantConsoleStore((state) => state.saveProductDescriptions)
  const [storeKeywords, setStoreKeywords] = useState('烟火气、家庭聚餐、热乎下饭')
  const [productKeywords, setProductKeywords] = useState('招牌风味、口感层次、午晚餐推荐')
  const [storeDraft, setStoreDraft] = useState<AIMerchantStoreDescriptionResponse | null>(null)
  const [productDraft, setProductDraft] = useState<AIMerchantProductDescriptionsResponse | null>(null)
  const [loadingAction, setLoadingAction] = useState<LoadingAction>(null)
  const [storeSavedHint, setStoreSavedHint] = useState(false)

  const productCount = selectedStore?.products.length ?? 0
  const currentDescription = selectedStore?.merchant.description.trim() ?? ''

  const handleGenerateStore = () => {
    if (!selectedStore) {
      showNotice('请先选择店铺。', 'error')
      return
    }
    if (!storeKeywords.trim()) {
      showNotice('请输入店铺关键词。', 'error')
      return
    }

    setLoadingAction('store-generate')
    generateStoreDescription(selectedStore.merchant.id, storeKeywords)
      .then((draft) => {
        setStoreDraft(draft)
      })
      .catch((error) => {
        console.error(error)
        showNotice(error instanceof Error ? error.message : '店铺描述生成失败', 'error')
      })
      .finally(() => setLoadingAction(null))
  }

  const handleSaveStore = () => {
    if (!storeDraft) return

    setLoadingAction('store-save')
    saveStoreDescription(storeDraft.merchantId, storeDraft.description)
      .then(() => {
        setStoreDraft(null)
        setStoreSavedHint(true)
        showNotice('店铺描述已保存，建议继续生成菜品描述。', 'success')
      })
      .catch((error) => {
        console.error(error)
        showNotice(error instanceof Error ? error.message : '店铺描述保存失败', 'error')
      })
      .finally(() => setLoadingAction(null))
  }

  const handleGenerateProducts = () => {
    if (!selectedStore) {
      showNotice('请先选择店铺。', 'error')
      return
    }
    if (productCount === 0) {
      showNotice('请先创建菜品后再生成菜品描述。', 'error')
      return
    }
    if (!productKeywords.trim()) {
      showNotice('请输入菜品关键词。', 'error')
      return
    }

    setLoadingAction('product-generate')
    generateProductDescriptions(selectedStore.merchant.id, productKeywords)
      .then((draft) => {
        setProductDraft(draft)
        setStoreSavedHint(false)
      })
      .catch((error) => {
        console.error(error)
        showNotice(error instanceof Error ? error.message : '菜品描述生成失败', 'error')
      })
      .finally(() => setLoadingAction(null))
  }

  const handleSaveProducts = () => {
    if (!productDraft) return

    setLoadingAction('product-save')
    saveProductDescriptions(
      productDraft.merchantId,
      productDraft.products.map((product) => ({ productId: product.productId, description: product.description })),
    )
      .then(() => {
        setProductDraft(null)
        showNotice('菜品描述已批量保存。', 'success')
      })
      .catch((error) => {
        console.error(error)
        showNotice(error instanceof Error ? error.message : '菜品描述保存失败', 'error')
      })
      .finally(() => setLoadingAction(null))
  }

  return (
    <Card className="overflow-hidden border-orange-100 bg-gradient-to-br from-orange-50 via-white to-rose-50 shadow-sm">
      <CardHeader className="gap-2">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div className="space-y-1">
            <CardTitle className="flex items-center gap-2 text-slate-950">
              <span className="flex size-9 items-center justify-center rounded-2xl bg-orange-500 text-white shadow-lg shadow-orange-200">
                <Sparkles className="size-5" />
              </span>
              AI 文案助手
            </CardTitle>
            <CardDescription>先生成店铺描述，保存后再继续为已创建菜品批量优化描述。</CardDescription>
          </div>
          <div className="rounded-full border border-orange-200 bg-white/80 px-3 py-1 text-xs font-medium text-orange-700 shadow-sm">
            {productCount} 个菜品可优化
          </div>
        </div>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="rounded-2xl border border-orange-100 bg-white/80 p-4 shadow-sm backdrop-blur">
          <div className="flex flex-col gap-3 md:flex-row md:items-start md:justify-between">
            <div className="space-y-2 md:max-w-[55%]">
              <p className="text-sm font-semibold text-slate-900">当前店铺描述</p>
              <p className="text-sm leading-relaxed text-slate-600">
                {currentDescription || '尚未生成店铺描述，顾客端暂不会展示品牌介绍。'}
              </p>
            </div>
            <div className="w-full space-y-2 md:max-w-sm">
              <Label htmlFor="store-ai-keywords">店铺关键词</Label>
              <Textarea
                id="store-ai-keywords"
                value={storeKeywords}
                onChange={(event) => setStoreKeywords(event.target.value)}
                disabled={!selectedStore || loadingAction !== null}
                className="min-h-20 border-orange-100 bg-white/90"
              />
              <Button
                type="button"
                className="w-full cursor-pointer bg-gradient-to-r from-orange-500 to-rose-500 text-white shadow-md shadow-orange-100 transition-[filter,box-shadow] hover:brightness-105 hover:shadow-lg"
                disabled={!selectedStore || !storeKeywords.trim() || loadingAction !== null}
                onClick={handleGenerateStore}
              >
                <Wand2 className="size-4" />
                {loadingAction === 'store-generate' ? '正在生成店铺描述…' : '生成店铺描述'}
              </Button>
            </div>
          </div>
        </div>

        {storeSavedHint ? (
          <div className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-800">
            <span className="flex items-center gap-2 font-medium">
              <CheckCircle2 className="size-4" />
              店铺描述已完成，建议继续生成菜品描述，让顾客看到统一风格的菜单文案。
            </span>
            <Button type="button" size="sm" className="cursor-pointer" onClick={handleGenerateProducts} disabled={loadingAction !== null || productCount === 0}>
              继续生成菜品描述
            </Button>
          </div>
        ) : null}

        <div className="rounded-2xl border border-orange-100 bg-white/80 p-4 shadow-sm backdrop-blur">
          <div className="grid gap-3 md:grid-cols-[1fr_18rem] md:items-end">
            <div className="space-y-2">
              <Label htmlFor="product-ai-keywords">菜品关键词</Label>
              <Textarea
                id="product-ai-keywords"
                value={productKeywords}
                onChange={(event) => setProductKeywords(event.target.value)}
                disabled={!selectedStore || productCount === 0 || loadingAction !== null}
                className="min-h-20 border-orange-100 bg-white/90"
              />
              <p className="text-xs text-slate-500">菜品描述生成只要求菜品存在，不检查库存、上下架或剩余库存。</p>
            </div>
            <Button
              type="button"
              variant="outline"
              className="cursor-pointer border-orange-200 bg-white/90 text-orange-700 hover:bg-orange-50"
              disabled={!selectedStore || productCount === 0 || !productKeywords.trim() || loadingAction !== null}
              onClick={handleGenerateProducts}
            >
              <Sparkles className="size-4" />
              {loadingAction === 'product-generate' ? '正在生成菜品描述…' : '生成菜品描述'}
            </Button>
          </div>
        </div>
      </CardContent>

      <Dialog open={!!storeDraft} onOpenChange={(open) => !open && loadingAction !== 'store-save' && setStoreDraft(null)}>
        <DialogContent className="max-w-xl rounded-2xl border border-orange-100 bg-white p-6">
          <DialogHeader>
            <DialogTitle>确认店铺描述</DialogTitle>
            <DialogDescription>保存后将展示在顾客端商家列表和店铺详情中。</DialogDescription>
          </DialogHeader>
          <div className="rounded-2xl border border-orange-100 bg-orange-50/70 p-4 text-sm leading-relaxed text-slate-700">
            {storeDraft?.description}
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setStoreDraft(null)} disabled={loadingAction === 'store-save'}>
              取消
            </Button>
            <Button onClick={handleSaveStore} disabled={loadingAction === 'store-save'}>
              {loadingAction === 'store-save' ? '保存中…' : '确认保存店铺描述'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog open={!!productDraft} onOpenChange={(open) => !open && loadingAction !== 'product-save' && setProductDraft(null)}>
        <DialogContent className="max-h-[85vh] max-w-3xl overflow-hidden rounded-2xl border border-orange-100 bg-white p-6">
          <DialogHeader>
            <DialogTitle>确认菜品描述</DialogTitle>
            <DialogDescription>将批量保存以下菜品描述，只更新描述字段。</DialogDescription>
          </DialogHeader>
          <div className="max-h-[56vh] space-y-3 overflow-y-auto pr-1">
            {productDraft?.products.map((product) => (
              <div key={product.productId} className="rounded-2xl border border-orange-100 bg-orange-50/60 p-4">
                <p className="font-medium text-slate-900">{product.productName}</p>
                <p className="mt-2 text-sm leading-relaxed text-slate-600">{product.description}</p>
              </div>
            ))}
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setProductDraft(null)} disabled={loadingAction === 'product-save'}>
              取消
            </Button>
            <Button onClick={handleSaveProducts} disabled={loadingAction === 'product-save'}>
              {loadingAction === 'product-save' ? '保存中…' : '确认批量保存'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </Card>
  )
}
