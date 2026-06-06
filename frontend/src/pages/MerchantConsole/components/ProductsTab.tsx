import { useEffect, useMemo, useRef, useState, type ChangeEvent } from 'react'
import { Check, ImageIcon, PackageSearch, Plus, Store, TicketPercent, Trash2, Upload } from 'lucide-react'

import { Badge } from '@/components/ui/badge'
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
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Textarea } from '@/components/ui/textarea'
import { PromotionDateInput, PromotionEnableControl } from '@/components/PromotionControls'
import { useAppChrome } from '@/hooks/useAppChrome'
import { resolveApiMediaUrl } from '@/lib/api-media-url'
import { bundleOptionExtraPrice } from '@/lib/bundles'
import { getLocalImageFileError } from '@/lib/local-image-file'
import type { CreateProductRequest } from '@/objects/merchant/apiTypes/CreateProductRequest'
import type { MerchantStoreProfile } from '@/objects/merchant/MerchantStoreProfile'
import type { Product, ProductBundleGroup } from '@/objects/merchant/Product'
import type { UpdateProductRequest } from '@/objects/merchant/apiTypes/UpdateProductRequest'
import { ListingStatuses } from '@/objects/shared/ids'
import type { ListingStatus, ProductId } from '@/objects/shared/ids'
import type { Promotion } from '@/objects/shared/Promotion'
import { promotionSummary, roundMoney } from '@/lib/promotions'
import { useMerchantConsoleStore } from '@/stores/pages/use-merchant-console-store'

import { MerchantAICopywritingCard } from './MerchantAICopywritingCard'

type ProductsTabProps = {
  selectedStore: MerchantStoreProfile | null
  onCreateProduct: (input: CreateProductRequest) => Promise<void>
  onEditProduct: (productId: ProductId, input: UpdateProductRequest) => Promise<void>
  onUploadProductImage: (productId: ProductId, file: File) => Promise<Product>
}

const listingStatuses = Object.values(ListingStatuses)

type ProductFormState = UpdateProductRequest
type CreateProductFormState = {
  name: string
  description: string
  imageUrl: string
  categoryName: string
  price: number
  remainingStock: number
  listingStatus: CreateProductRequest['listingStatus']
  bundleGroups: ProductBundleGroup[]
}

const initialCreateFormState: CreateProductFormState = {
  name: '',
  description: '',
  imageUrl: '',
  categoryName: '默认分类',
  price: 0,
  remainingStock: 0,
  listingStatus: ListingStatuses.listed,
  bundleGroups: [],
}

const productCategoryName = (product: Pick<Product, 'categoryName'>) => product.categoryName?.trim() || '默认分类'
const isBundleProduct = (product: Pick<Product, 'bundleGroups'>) => (product.bundleGroups ?? []).length > 0
const bundleGroupsBasePrice = (groups: ProductBundleGroup[], products: Product[]) =>
  roundMoney(groups.reduce((sum, group) => {
    const optionPrices = group.options
      .map((option) => products.find((product) => product.id === option.productId)?.price)
      .filter((price): price is number => typeof price === 'number' && Number.isFinite(price))
    const minPrice = optionPrices.length > 0 ? Math.min(...optionPrices) : 0
    return sum + minPrice * Math.max(1, Math.floor(group.quantity || 1))
  }, 0))

const createBundleGroup = (): ProductBundleGroup => ({
  id: `bundle-group-${Date.now()}-${Math.random().toString(16).slice(2)}`,
  name: '套餐类别',
  quantity: 1,
  options: [],
})

const sanitizeBundleGroups = (groups: ProductBundleGroup[]) =>
  groups
    .map((group) => ({
      ...group,
      name: group.name.trim() || '套餐类别',
      quantity: Math.max(1, Math.floor(group.quantity || 1)),
      options: group.options.filter((option, index, list) => option.productId && list.findIndex((item) => item.productId === option.productId) === index),
    }))
    .filter((group) => group.options.length > 0)

function productDiscountedPrice(product: Product, promotion: Promotion) {
  return roundMoney(product.price - promotion.discountValue)
}

function BundleGroupsEditor({
  groups,
  products,
  onChange,
}: {
  groups: ProductBundleGroup[]
  products: Product[]
  onChange: (groups: ProductBundleGroup[]) => void
}) {
  const normalProducts = products.filter((product) => !isBundleProduct(product))

  const updateGroup = (groupId: string, patch: Partial<ProductBundleGroup>) => {
    onChange(groups.map((group) => group.id === groupId ? { ...group, ...patch } : group))
  }

  const toggleOption = (group: ProductBundleGroup, product: Product) => {
    const exists = group.options.some((option) => option.productId === product.id)
    updateGroup(group.id, {
      options: exists
        ? group.options.filter((option) => option.productId !== product.id)
        : [...group.options, { productId: product.id }],
    })
  }

  return (
    <div className="space-y-3 rounded-2xl border border-orange-100 bg-orange-50/50 p-3">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div>
          <p className="text-sm font-semibold text-slate-900">套餐类别</p>
          <p className="text-xs text-slate-500">每个类别可设置顾客需选择几件商品，可重复选择。</p>
        </div>
        <Button type="button" size="sm" variant="outline" onClick={() => onChange([...groups, createBundleGroup()])}>
          <Plus className="size-4" />
          添加类别
        </Button>
      </div>
      {groups.length === 0 ? (
        <p className="rounded-xl border border-dashed border-orange-200 bg-white/70 px-3 py-3 text-sm text-slate-500">
          还没有套餐类别，添加类别后选择已有菜品组成套餐。
        </p>
      ) : null}
      {groups.map((group) => (
        <div key={group.id} className="space-y-3 rounded-xl border border-orange-100 bg-white p-3">
          <div className="grid gap-3 sm:grid-cols-[minmax(0,1fr)_8rem_auto] sm:items-end">
            <div className="space-y-1">
              <Label>类别名称</Label>
              <Input value={group.name} onChange={(event) => updateGroup(group.id, { name: event.target.value })} />
            </div>
            <div className="space-y-1">
              <Label>可选件数</Label>
              <Input
                type="number"
                min="1"
                step="1"
                value={group.quantity}
                onChange={(event) => updateGroup(group.id, { quantity: Number(event.target.value) || 1 })}
              />
            </div>
            <Button type="button" variant="outline" className="text-rose-600" onClick={() => onChange(groups.filter((item) => item.id !== group.id))}>
              <Trash2 className="size-4" />
            </Button>
          </div>
          <div className="grid gap-2 sm:grid-cols-2">
            {normalProducts.length === 0 ? (
              <p className="rounded-xl border border-dashed border-orange-100 px-3 py-3 text-sm text-slate-500">暂无可选择的普通菜品。</p>
            ) : null}
            {normalProducts.map((product) => {
              const checked = group.options.some((option) => option.productId === product.id)
              const extraPrice = checked ? bundleOptionExtraPrice(group, product, normalProducts) : null
              return (
                <button
                  key={product.id}
                  type="button"
                  className={`flex min-w-0 items-center gap-2 rounded-xl border px-3 py-2 text-left text-sm transition-colors ${
                    checked ? 'border-orange-300 bg-orange-50 text-orange-800' : 'border-slate-200 bg-white text-slate-700 hover:border-orange-200'
                  }`}
                  onClick={() => toggleOption(group, product)}
                >
                  <span className={`flex size-5 shrink-0 items-center justify-center rounded-full border ${checked ? 'border-orange-400 bg-orange-400 text-white' : 'border-slate-300'}`}>
                    {checked ? <Check className="size-3.5" /> : null}
                  </span>
                  <span className="min-w-0 flex-1 truncate">{product.name}</span>
                  <span className="shrink-0 text-xs text-slate-500">
                    {extraPrice === null ? `¥${product.price.toFixed(2)}` : `+¥${extraPrice.toFixed(2)}`}
                  </span>
                </button>
              )
            })}
          </div>
        </div>
      ))}
    </div>
  )
}

export function ProductsTab({ selectedStore, onCreateProduct, onEditProduct, onUploadProductImage }: ProductsTabProps) {
  const { showNotice } = useAppChrome()
  const saveStorePromotions = useMerchantConsoleStore((state) => state.saveStorePromotions)
  const merchantProducts = useMemo(() => selectedStore?.products ?? [], [selectedStore?.products])
  const categoryGroups = useMemo(
    () =>
      merchantProducts.reduce<Array<{ categoryName: string; products: Product[] }>>((groups, product) => {
        const categoryName = productCategoryName(product)
        const matched = groups.find((group) => group.categoryName === categoryName)
        if (matched) {
          matched.products.push(product)
        } else {
          groups.push({ categoryName, products: [product] })
        }
        return groups
      }, []),
    [merchantProducts],
  )
  const productFileInputRef = useRef<HTMLInputElement>(null)
  const [isCreateDialogOpen, setIsCreateDialogOpen] = useState(false)
  const [editingProduct, setEditingProduct] = useState<Product | null>(null)
  const [discountProduct, setDiscountProduct] = useState<Product | null>(null)
  const [productPromotionDraft, setProductPromotionDraft] = useState<Promotion | null>(null)
  const [formState, setFormState] = useState<ProductFormState | null>(null)
  const [createFormState, setCreateFormState] = useState<CreateProductFormState>(initialCreateFormState)
  const [promotionsDraft, setPromotionsDraft] = useState<{ merchantId: string | null; promotions: Promotion[] }>({
    merchantId: null,
    promotions: [],
  })
  const [saving, setSaving] = useState(false)
  const selectedMerchantId = selectedStore?.merchant.id ?? null
  const promotions =
    promotionsDraft.merchantId === selectedMerchantId
      ? promotionsDraft.promotions
      : (selectedStore?.merchant.promotions ?? [])
  const storePromotions = promotions.filter((promotion) => promotion.discountType !== 'productAmount')

  const productPromotionFor = (productId: ProductId) =>
    promotions.find((promotion) => promotion.discountType === 'productAmount' && (promotion.productIds ?? []).includes(productId))
  const enabledProductPromotionFor = (productId: ProductId) => {
    const promotion = productPromotionFor(productId)
    return promotion?.enabled ? promotion : undefined
  }

  useEffect(() => {
    if (!editingProduct) {
      setFormState(null)
      return
    }

    setFormState({
      name: editingProduct.name,
      description: editingProduct.description,
      imageUrl: editingProduct.imageUrl,
      categoryName: productCategoryName(editingProduct),
      price: editingProduct.price,
      remainingStock: editingProduct.remainingStock,
      listingStatus: editingProduct.listingStatus,
      bundleGroups: editingProduct.bundleGroups ?? [],
    })
  }, [editingProduct])

  useEffect(() => {
    if (!promotionsDraft.merchantId) return
    const timer = window.setTimeout(() => {
      const invalidProductPromotion = promotionsDraft.promotions.find((promotion) => {
        if (promotion.discountType !== 'productAmount') return false
        const product = merchantProducts.find((item) => (promotion.productIds ?? []).includes(item.id))
        return !product || validateProductPromotion(product, promotion) !== null
      })
      if (invalidProductPromotion) return
      void saveStorePromotions(promotionsDraft.merchantId!, promotionsDraft.promotions).catch((error) => {
        showNotice(error instanceof Error ? error.message : '自动保存优惠失败', 'error')
      })
    }, 800)
    return () => window.clearTimeout(timer)
  }, [merchantProducts, promotionsDraft, saveStorePromotions, showNotice])

  if (!selectedStore) {
    return (
      <Card className="border-orange-100 bg-white/95">
        <CardContent className="p-6 text-sm text-slate-600">请先选择店铺后查看菜品管理内容。</CardContent>
      </Card>
    )
  }

  const createBundlePrice = bundleGroupsBasePrice(createFormState.bundleGroups, merchantProducts)
  const editBundleProducts = merchantProducts.filter((product) => product.id !== editingProduct?.id)
  const formBundlePrice = formState ? bundleGroupsBasePrice(formState.bundleGroups ?? [], editBundleProducts) : 0
  const createIsBundle = createFormState.bundleGroups.length > 0
  const formIsBundle = (formState?.bundleGroups ?? []).length > 0

  const handleSave = async () => {
    if (!editingProduct || !formState) {
      return
    }

    const normalizedBundleGroups = sanitizeBundleGroups(formState.bundleGroups ?? [])
    setSaving(true)
    try {
      await onEditProduct(editingProduct.id, {
        ...formState,
        price: normalizedBundleGroups.length > 0 ? bundleGroupsBasePrice(normalizedBundleGroups, editBundleProducts) : formState.price,
        bundleGroups: normalizedBundleGroups,
      })
      setEditingProduct(null)
    } finally {
      setSaving(false)
    }
  }

  const handleCreate = async () => {
    if (!selectedStore) {
      return
    }
    if (!createFormState.name.trim() || !createFormState.description.trim()) {
      return
    }

    const normalizedBundleGroups = sanitizeBundleGroups(createFormState.bundleGroups)
    setSaving(true)
    try {
      await onCreateProduct({
        merchantId: selectedStore.merchant.id,
        ...createFormState,
        price: normalizedBundleGroups.length > 0 ? bundleGroupsBasePrice(normalizedBundleGroups, merchantProducts) : createFormState.price,
        bundleGroups: normalizedBundleGroups,
      })
      setCreateFormState(initialCreateFormState)
      setIsCreateDialogOpen(false)
    } finally {
      setSaving(false)
    }
  }

  const handleProductFileChange = async (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0]
    event.target.value = ''
    if (!file || !editingProduct) {
      return
    }

    const fileError = getLocalImageFileError(file)
    if (fileError) {
      showNotice(fileError, 'error')
      return
    }

    setSaving(true)
    try {
      const updated = await onUploadProductImage(editingProduct.id, file)
      setEditingProduct(updated)
      setFormState({
        name: updated.name,
        description: updated.description,
        imageUrl: updated.imageUrl,
        categoryName: productCategoryName(updated),
        price: updated.price,
        remainingStock: updated.remainingStock,
        listingStatus: updated.listingStatus,
        bundleGroups: updated.bundleGroups ?? [],
      })
    } finally {
      setSaving(false)
    }
  }

  const normalizePromotionPatch = (promotion: Promotion, patch: Partial<Promotion>): Promotion => {
    const next = { ...promotion, ...patch }
    const usageLimit = next.usageLimit === null || next.usageLimit === undefined ? null : Math.max(1, Math.floor(next.usageLimit))
    return {
      ...next,
      usageLimit,
      remainingUses: usageLimit === null ? null : Math.min(next.remainingUses ?? usageLimit, usageLimit),
      productIds: next.discountType === 'productAmount' ? (next.productIds ?? []) : [],
    }
  }

  const updatePromotion = (id: string, patch: Partial<Promotion>) => {
    setPromotionsDraft({
      merchantId: selectedMerchantId,
      promotions: promotions.map((promotion) => promotion.id === id ? normalizePromotionPatch(promotion, patch) : promotion),
    })
  }

  const handleAddPromotion = () => {
    setPromotionsDraft({
      merchantId: selectedMerchantId,
      promotions: [
        ...promotions,
        {
          id: `merchant-promo-${Date.now()}`,
          title: '新优惠',
          discountType: 'amount',
          discountValue: 5,
          triggerType: 'none',
          triggerValue: 0,
          startsAt: null,
          endsAt: null,
          dailyStartTime: null,
          dailyEndTime: null,
          productIds: [],
          usageLimit: null,
          remainingUses: null,
          enabled: false,
        },
      ],
    })
  }

  const handleOpenProductPromotion = (product: Product) => {
    const existing = productPromotionFor(product.id)
    const maxDiscount = Math.max(0.01, roundMoney(product.price - 0.01))
    const draft = existing ?? {
      id: `product-promo-${product.id}-${Date.now()}`,
      title: `${product.name}专属优惠`,
      discountType: 'productAmount',
      discountValue: Math.min(1, maxDiscount),
      triggerType: 'none',
      triggerValue: 0,
      startsAt: null,
      endsAt: null,
      dailyStartTime: null,
      dailyEndTime: null,
      productIds: [product.id],
      usageLimit: null,
      remainingUses: null,
      enabled: false,
    }
    setDiscountProduct(product)
    setProductPromotionDraft(draft)
    if (!existing) {
      setPromotionsDraft({
        merchantId: selectedMerchantId,
        promotions: [...promotions, draft],
      })
    }
  }

  const closeProductPromotionDialog = () => {
    setDiscountProduct(null)
    setProductPromotionDraft(null)
  }

  const validateProductPromotion = (product: Product, promotion: Promotion): string | null => {
    if (promotion.discountValue <= 0) return '菜品优惠金额必须大于 0 元。'
    if (productDiscountedPrice(product, promotion) <= 0) return '优惠后的菜品价格必须大于 0 元。'
    return null
  }

  const updateProductPromotionDraft = (patch: Partial<Promotion>) => {
    if (!discountProduct || !productPromotionDraft) return
    const maxDiscount = Math.max(0.01, roundMoney(discountProduct.price - 0.01))
    const nextDraft = {
      ...productPromotionDraft,
      ...patch,
      discountValue: patch.discountValue === undefined ? productPromotionDraft.discountValue : Math.min(maxDiscount, Math.max(0.01, roundMoney(patch.discountValue))),
    }
    const normalizedPromotion: Promotion = {
      ...nextDraft,
      discountType: 'productAmount',
      productIds: [discountProduct.id],
      discountValue: roundMoney(nextDraft.discountValue),
      triggerType: 'none',
      triggerValue: 0,
    }
    setProductPromotionDraft(normalizedPromotion)

    setPromotionsDraft({
      merchantId: selectedMerchantId,
      promotions: [
        ...promotions.filter((promotion) => !(promotion.discountType === 'productAmount' && (promotion.productIds ?? []).includes(discountProduct.id)) && promotion.id !== normalizedPromotion.id),
        normalizedPromotion,
      ],
    })
  }

  const handleRemoveProductPromotion = () => {
    if (!discountProduct) return
    setPromotionsDraft({
      merchantId: selectedMerchantId,
      promotions: promotions.filter((promotion) => !(promotion.discountType === 'productAmount' && (promotion.productIds ?? []).includes(discountProduct.id))),
    })
    closeProductPromotionDialog()
  }

  return (
    <div className="space-y-4">
      <section className="grid gap-4 md:grid-cols-2">
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
            <CardDescription>主营商品数</CardDescription>
            <CardTitle>{merchantProducts.length}</CardTitle>
          </CardHeader>
        </Card>
      </section>

      <MerchantAICopywritingCard selectedStore={selectedStore} />

      <Card className="border-orange-100 bg-white/95">
        <CardHeader>
          <div className="flex items-center justify-between gap-3">
            <div>
              <CardTitle className="flex items-center gap-2">
                <PackageSearch className="size-5 text-orange-500" />
                商品管理
              </CardTitle>
              <CardDescription>可新建菜品，或通过编辑统一修改菜品名称、描述、库存、上/下架状态和价格</CardDescription>
            </div>
            <Button onClick={() => setIsCreateDialogOpen(true)}>新建菜品</Button>
          </div>
        </CardHeader>
        <CardContent className="space-y-3">
          {merchantProducts.length === 0 ? (
            <p className="text-sm text-slate-500">当前店铺暂无商品，请先创建菜品。</p>
          ) : (
            categoryGroups.map((group) => (
              <section key={group.categoryName} className="space-y-3">
                <div className="flex items-center gap-3">
                  <h3 className="text-base font-semibold text-slate-950">{group.categoryName}</h3>
                  <span className="text-xs text-slate-500">{group.products.length} 个菜品</span>
                  <span className="h-px flex-1 bg-orange-100" />
                </div>
                <div className="space-y-3">
                  {group.products.map((product) => (
                    (() => {
                      const productPromotion = enabledProductPromotionFor(product.id)
                      const discountedPrice = productPromotion ? productDiscountedPrice(product, productPromotion) : null
                      const bundleProduct = isBundleProduct(product)
                      return (
                        <div
                          key={product.id}
                          className="rounded-xl border border-orange-100 p-4"
                        >
                          <div className="flex flex-wrap items-start justify-between gap-3">
                            {product.imageUrl?.trim() ? (
                              <div className="aspect-[4/3] w-28 shrink-0 overflow-hidden rounded-xl border border-orange-100 bg-orange-50">
                                <img
                                  src={resolveApiMediaUrl(product.imageUrl)}
                                  alt={product.name}
                                  className="size-full object-cover"
                                />
                              </div>
                            ) : (
                              <div className="flex aspect-[4/3] w-28 shrink-0 items-center justify-center rounded-xl border border-dashed border-orange-100 bg-orange-50 text-orange-400">
                                <ImageIcon className="size-5" />
                              </div>
                            )}
                            <div className="space-y-3">
                              <div className="space-y-1">
                                <div className="flex flex-wrap items-center gap-2">
                                  <p className="font-medium text-slate-900">{product.name}</p>
                                  {bundleProduct ? <Badge className="bg-amber-100 text-amber-700 hover:bg-amber-100">套餐</Badge> : null}
                                  {productPromotion ? <Badge variant="secondary">优惠后 ¥{discountedPrice?.toFixed(2)}</Badge> : null}
                                </div>
                                <p className="text-sm text-slate-600">{product.description}</p>
                                {bundleProduct ? (
                                  <p className="text-xs text-amber-700">
                                    {(product.bundleGroups ?? []).map((group) => `${group.name}选${group.quantity}`).join(' · ')}
                                  </p>
                                ) : null}
                              </div>
                                <div className="grid gap-2 text-sm text-slate-600 md:grid-cols-2">
                                <p>{bundleProduct ? '默认最低价' : '价格'}：¥{product.price.toFixed(2)} / 份</p>
                                <p>金额：¥{product.price.toFixed(2)}</p>
                                <p>剩余库存：{product.remainingStock}</p>
                                <p>月销量：{product.monthlySales}</p>
                              </div>
                            </div>
                            <div className="flex flex-col items-end gap-2">
                              <Badge variant="outline">{product.listingStatus}</Badge>
                              <Button size="sm" variant="outline" onClick={() => handleOpenProductPromotion(product)}>
                                优惠
                              </Button>
                              <Button size="sm" onClick={() => setEditingProduct(product)}>
                                编辑
                              </Button>
                            </div>
                          </div>
                        </div>
                      )
                    })()
                  ))}
                </div>
              </section>
            ))
          )}
        </CardContent>
      </Card>

      <Card className="border-orange-100 bg-white/95">
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <TicketPercent className="size-5 text-orange-500" />
            店铺优惠
          </CardTitle>
          <CardDescription>仅当前店铺可用；顾客按优惠后付款，商家收入也按优惠后结算。</CardDescription>
        </CardHeader>
        <CardContent className="space-y-3">
          {storePromotions.length === 0 ? <p className="text-sm text-slate-500">当前未设置店铺优惠。</p> : null}
          {storePromotions.map((promotion) => {
            const finite = promotion.usageLimit !== null && promotion.usageLimit !== undefined
            return (
              <div key={promotion.id} className="space-y-3 rounded-xl border border-orange-100 bg-orange-50/50 p-3">
                <div className="grid gap-3 md:grid-cols-[1fr_11rem_8rem]">
                  <div className="space-y-1">
                    <Label>优惠名称</Label>
                    <Input value={promotion.title} onChange={(event) => updatePromotion(promotion.id, { title: event.target.value })} />
                  </div>
                  <div className="space-y-1">
                    <Label>优惠类型</Label>
                    <select className="h-10 w-full rounded-md border border-input bg-background px-3 text-sm" value={promotion.discountType} onChange={(event) => updatePromotion(promotion.id, { discountType: event.target.value as Promotion['discountType'] })}>
                      <option value="amount">减xx元</option>
                      <option value="percent">xx折</option>
                    </select>
                  </div>
                  <div className="space-y-1">
                    <Label>{promotion.discountType === 'percent' ? '折扣' : '金额'}</Label>
                    <Input type="number" min="0" step="0.1" value={promotion.discountValue} onChange={(event) => updatePromotion(promotion.id, { discountValue: Number(event.target.value) })} />
                  </div>
                </div>

                <div className="grid gap-3 md:grid-cols-[9rem_8rem_1fr_1fr] md:items-end">
                  <div className="space-y-1">
                    <Label>触发条件</Label>
                    <select className="h-10 w-full rounded-md border border-input bg-background px-3 text-sm" value={promotion.triggerType} onChange={(event) => updatePromotion(promotion.id, { triggerType: event.target.value as Promotion['triggerType'], triggerValue: event.target.value === 'none' ? 0 : promotion.triggerValue })}>
                      <option value="none">无条件</option>
                      <option value="amount">满xx元</option>
                      <option value="items">满xx件</option>
                    </select>
                  </div>
                  <div className="space-y-1">
                    <Label>门槛</Label>
                    <Input type="number" min="0" step="1" value={promotion.triggerValue} disabled={promotion.triggerType === 'none'} onChange={(event) => updatePromotion(promotion.id, { triggerValue: Number(event.target.value) })} />
                  </div>
                  <div className="space-y-1">
                    <Label>开始日期</Label>
                    <PromotionDateInput value={promotion.startsAt} onChange={(value) => updatePromotion(promotion.id, { startsAt: value })} />
                  </div>
                  <div className="space-y-1">
                    <Label>结束日期</Label>
                    <PromotionDateInput value={promotion.endsAt} onChange={(value) => updatePromotion(promotion.id, { endsAt: value })} />
                  </div>
                </div>

                <div className="grid gap-3 md:grid-cols-[1fr_1fr_9rem_8rem_auto] md:items-end">
                  <div className="space-y-1">
                    <Label>每日开始时刻</Label>
                    <Input type="time" value={promotion.dailyStartTime ?? ''} onChange={(event) => updatePromotion(promotion.id, { dailyStartTime: event.target.value || null })} />
                  </div>
                  <div className="space-y-1">
                    <Label>每日结束时刻</Label>
                    <Input type="time" value={promotion.dailyEndTime ?? ''} onChange={(event) => updatePromotion(promotion.id, { dailyEndTime: event.target.value || null })} />
                  </div>
                  <div className="space-y-1">
                    <Label>次数</Label>
                    <select className="h-10 w-full rounded-md border border-input bg-background px-3 text-sm" value={finite ? 'finite' : 'infinite'} onChange={(event) => updatePromotion(promotion.id, { usageLimit: event.target.value === 'finite' ? (promotion.usageLimit ?? 10) : null, remainingUses: event.target.value === 'finite' ? (promotion.remainingUses ?? promotion.usageLimit ?? 10) : null })}>
                      <option value="infinite">无限次</option>
                      <option value="finite">有限次数</option>
                    </select>
                  </div>
                  <div className="space-y-1">
                    <Label>可用次数</Label>
                    <Input type="number" min="1" step="1" value={promotion.usageLimit ?? ''} disabled={!finite} onChange={(event) => updatePromotion(promotion.id, { usageLimit: Number(event.target.value) || 1, remainingUses: Number(event.target.value) || 1 })} />
                  </div>
                  <Button type="button" variant="outline" className="text-rose-600" onClick={() => setPromotionsDraft({ merchantId: selectedMerchantId, promotions: promotions.filter((item) => item.id !== promotion.id) })}>
                    <Trash2 className="size-4" />
                  </Button>
                </div>

                <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
                  <p className="min-w-0 break-words text-xs text-orange-700">
                    {promotion.enabled
                      ? `预览：${promotionSummary(promotion)} · ${finite ? `${promotion.remainingUses ?? promotion.usageLimit}张优惠券` : '优惠'}`
                      : '已禁用：顾客端、结算和商品展示中不会显示该优惠'}
                  </p>
                  <PromotionEnableControl enabled={promotion.enabled} onChange={(enabled) => updatePromotion(promotion.id, { enabled })} />
                </div>
              </div>
            )
          })}
          <div className="flex flex-wrap justify-start gap-2">
            <Button type="button" variant="outline" onClick={handleAddPromotion}>
              <Plus className="size-4" />
              添加优惠
            </Button>
          </div>
        </CardContent>
      </Card>

      <Dialog open={Boolean(discountProduct)} onOpenChange={(open) => !open && closeProductPromotionDialog()}>
        <DialogContent className="flex max-h-[min(36rem,calc(100vh-2rem))] w-[calc(100vw-2rem)] max-w-2xl flex-col overflow-hidden rounded-2xl border border-orange-100 bg-white p-0">
          <DialogHeader className="shrink-0 px-6 pt-6">
            <DialogTitle>{discountProduct ? `${discountProduct.name}优惠` : '菜品优惠'}</DialogTitle>
            <DialogDescription>为当前菜品单独设置优惠金额，优惠后的菜品价格必须大于 0 元。</DialogDescription>
          </DialogHeader>
          {discountProduct && productPromotionDraft ? (
            (() => {
              const finite = productPromotionDraft.usageLimit !== null && productPromotionDraft.usageLimit !== undefined
              return (
                <div className="min-h-0 flex-1 space-y-3 overflow-y-auto px-6 py-4">
                  <div className="grid gap-3 md:grid-cols-[minmax(0,1fr)_11rem_8rem]">
                    <div className="space-y-1">
                      <Label>优惠名称</Label>
                      <Input value={productPromotionDraft.title} onChange={(event) => updateProductPromotionDraft({ title: event.target.value })} />
                    </div>
                    <div className="space-y-1">
                      <Label>优惠类型</Label>
                      <Input value="针对当前菜品减xx元" disabled />
                    </div>
                    <div className="space-y-1">
                      <Label>金额</Label>
                      <Input
                        type="number"
                        min="0.01"
                        step="0.01"
                        value={productPromotionDraft.discountValue}
                        onChange={(event) => updateProductPromotionDraft({ discountValue: Number(event.target.value) })}
                      />
                    </div>
                  </div>

                  <div className="grid gap-3 md:grid-cols-[minmax(0,1fr)_minmax(0,1fr)_8rem] md:items-end">
                    <div className="space-y-1">
                      <Label>开始日期</Label>
                      <PromotionDateInput value={productPromotionDraft.startsAt} onChange={(value) => updateProductPromotionDraft({ startsAt: value })} />
                    </div>
                    <div className="space-y-1">
                      <Label>结束日期</Label>
                      <PromotionDateInput value={productPromotionDraft.endsAt} onChange={(value) => updateProductPromotionDraft({ endsAt: value })} />
                    </div>
                    <div className="space-y-1">
                      <Label>优惠后价格</Label>
                      <Input value={`¥${productDiscountedPrice(discountProduct, productPromotionDraft).toFixed(2)}`} disabled />
                    </div>
                  </div>

                  <div className="grid gap-3 md:grid-cols-[minmax(0,1fr)_minmax(0,1fr)_9rem_8rem] md:items-end">
                    <div className="space-y-1">
                      <Label>每日开始时刻</Label>
                      <Input type="time" value={productPromotionDraft.dailyStartTime ?? ''} onChange={(event) => updateProductPromotionDraft({ dailyStartTime: event.target.value || null })} />
                    </div>
                    <div className="space-y-1">
                      <Label>每日结束时刻</Label>
                      <Input type="time" value={productPromotionDraft.dailyEndTime ?? ''} onChange={(event) => updateProductPromotionDraft({ dailyEndTime: event.target.value || null })} />
                    </div>
                    <div className="space-y-1">
                      <Label>次数</Label>
                      <select className="h-10 w-full rounded-md border border-input bg-background px-3 text-sm" value={finite ? 'finite' : 'infinite'} onChange={(event) => updateProductPromotionDraft({ usageLimit: event.target.value === 'finite' ? (productPromotionDraft.usageLimit ?? 10) : null, remainingUses: event.target.value === 'finite' ? (productPromotionDraft.remainingUses ?? productPromotionDraft.usageLimit ?? 10) : null })}>
                        <option value="infinite">无限次</option>
                        <option value="finite">有限次数</option>
                      </select>
                    </div>
                    <div className="space-y-1">
                      <Label>可用次数</Label>
                      <Input type="number" min="1" step="1" value={productPromotionDraft.usageLimit ?? ''} disabled={!finite} onChange={(event) => updateProductPromotionDraft({ usageLimit: Number(event.target.value) || 1, remainingUses: Number(event.target.value) || 1 })} />
                    </div>
                  </div>

                  <div className="space-y-1">
                    <Label>适用菜品</Label>
                    <Input value={discountProduct.name} disabled />
                  </div>

                  <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
                    <p className="min-w-0 break-words text-xs text-orange-700">
                      {productPromotionDraft.enabled
                        ? `预览：${promotionSummary(productPromotionDraft)} · ${finite ? `${productPromotionDraft.remainingUses ?? productPromotionDraft.usageLimit}张优惠券` : '优惠'} · 原价 ¥${discountProduct.price.toFixed(2)}，优惠后 ¥${productDiscountedPrice(discountProduct, productPromotionDraft).toFixed(2)}`
                        : '已禁用：商家端菜品卡片、顾客端和结算中不会显示或使用该优惠'}
                    </p>
                    <PromotionEnableControl enabled={productPromotionDraft.enabled} onChange={(enabled) => updateProductPromotionDraft({ enabled })} />
                  </div>
                </div>
              )
            })()
          ) : null}
          <DialogFooter className="shrink-0 flex-wrap gap-2 border-t border-orange-100 px-6 py-4">
            {discountProduct && productPromotionFor(discountProduct.id) ? (
              <Button type="button" variant="outline" className="text-rose-600" onClick={handleRemoveProductPromotion}>
                删除优惠
              </Button>
            ) : null}
            <Button type="button" variant="outline" onClick={closeProductPromotionDialog}>取消</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog open={isCreateDialogOpen} onOpenChange={setIsCreateDialogOpen}>
        <DialogContent className="flex max-h-[min(42rem,calc(100vh-2rem))] max-w-2xl flex-col overflow-hidden rounded-2xl border border-orange-100 bg-white p-0">
          <DialogHeader className="shrink-0 px-6 pt-6">
            <DialogTitle>新建商品</DialogTitle>
            <DialogDescription>可创建普通菜品，也可开启套餐并选择已有菜品组成套餐。</DialogDescription>
          </DialogHeader>

          <div className="min-h-0 flex-1 space-y-4 overflow-y-auto px-6 py-4">
            {createFormState.imageUrl.trim() ? (
              <div className="aspect-video w-full overflow-hidden rounded-xl border border-orange-100 bg-orange-50">
                <img
                  src={resolveApiMediaUrl(createFormState.imageUrl)}
                  alt="菜品预览"
                  className="size-full object-cover"
                />
              </div>
            ) : null}

            <div className="space-y-2">
              <Label htmlFor="create-product-name">商品名称</Label>
              <Input
                id="create-product-name"
                value={createFormState.name}
                onChange={(event) => setCreateFormState({ ...createFormState, name: event.target.value })}
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="create-product-image-url">商品图片链接</Label>
              <Input
                id="create-product-image-url"
                type="text"
                inputMode="url"
                placeholder="https://example.com/product.jpg"
                value={createFormState.imageUrl}
                onChange={(event) => setCreateFormState({ ...createFormState, imageUrl: event.target.value })}
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="create-product-category">商品类别</Label>
              <Input
                id="create-product-category"
                value={createFormState.categoryName}
                placeholder="例如：人气Top、精选套餐、饮品"
                onChange={(event) => setCreateFormState({ ...createFormState, categoryName: event.target.value })}
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="create-product-description">描述信息</Label>
              <Textarea
                id="create-product-description"
                value={createFormState.description}
                onChange={(event) => setCreateFormState({ ...createFormState, description: event.target.value })}
              />
            </div>

            <div className="grid gap-4 md:grid-cols-2">
              <div className="space-y-2">
                <Label htmlFor="create-product-price">{createIsBundle ? '套餐默认最低价' : '价格'}</Label>
                <Input
                  id="create-product-price"
                  type={createIsBundle ? 'text' : 'number'}
                  min="0"
                  step="0.01"
                  disabled={createIsBundle}
                  value={createIsBundle ? `¥${createBundlePrice.toFixed(2)}` : (createFormState.price === 0 ? '' : createFormState.price)}
                  onChange={(event) =>
                    setCreateFormState({ ...createFormState, price: Number(event.target.value) || 0 })
                  }
                />
                {createIsBundle ? <p className="text-xs text-slate-500">根据每个套餐类别中价格最低的已选菜品自动计算。</p> : null}
              </div>

              <div className="space-y-2">
                <Label htmlFor="create-product-stock">剩余库存</Label>
                <Input
                  id="create-product-stock"
                  type="number"
                  min="0"
                  step="1"
                  value={createFormState.remainingStock === 0 ? '' : createFormState.remainingStock}
                  onChange={(event) =>
                    setCreateFormState({ ...createFormState, remainingStock: Number(event.target.value) || 0 })
                  }
                />
              </div>
            </div>

            <div className="space-y-2">
              <Label>上/下架状态</Label>
              <Select
                value={createFormState.listingStatus}
                onValueChange={(value: ListingStatus) => setCreateFormState({ ...createFormState, listingStatus: value })}
              >
                <SelectTrigger>
                  <SelectValue placeholder="请选择状态" />
                </SelectTrigger>
                <SelectContent>
                  {listingStatuses.map((status) => (
                    <SelectItem key={status} value={status}>
                      {status}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-3">
              <label className="flex cursor-pointer items-center gap-2 rounded-xl border border-orange-100 bg-orange-50/60 px-3 py-2 text-sm font-medium text-slate-800">
                <input
                  type="checkbox"
                  className="size-4 accent-orange-500"
                  checked={createFormState.bundleGroups.length > 0}
                  onChange={(event) => setCreateFormState({ ...createFormState, bundleGroups: event.target.checked ? [createBundleGroup()] : [] })}
                />
                这是一个套餐商品
              </label>
              {createFormState.bundleGroups.length > 0 ? (
                <BundleGroupsEditor
                  groups={createFormState.bundleGroups}
                  products={merchantProducts}
                  onChange={(bundleGroups) => setCreateFormState({ ...createFormState, bundleGroups })}
                />
              ) : null}
            </div>
          </div>

          <DialogFooter className="shrink-0 border-t border-orange-100 px-6 py-4">
            <Button
              variant="outline"
              onClick={() => {
                setCreateFormState(initialCreateFormState)
                setIsCreateDialogOpen(false)
              }}
              disabled={saving}
            >
              取消
            </Button>
            <Button
              onClick={() => void handleCreate()}
              disabled={saving || !createFormState.name.trim() || !createFormState.description.trim()}
            >
              创建商品
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog open={!!editingProduct} onOpenChange={(open) => !open && setEditingProduct(null)}>
        <DialogContent className="flex max-h-[min(42rem,calc(100vh-2rem))] max-w-2xl flex-col overflow-hidden rounded-2xl border border-orange-100 bg-white p-0">
          <DialogHeader className="shrink-0 px-6 pt-6">
            <DialogTitle>编辑商品</DialogTitle>
            <DialogDescription>可修改普通菜品信息，也可把商品设置为套餐并配置可选类别。</DialogDescription>
          </DialogHeader>

          {formState ? (
            <div className="min-h-0 flex-1 space-y-4 overflow-y-auto px-6 py-4">
              <input
                ref={productFileInputRef}
                type="file"
                accept="image/jpeg,image/png,image/gif,image/webp,.jpg,.jpeg,.png,.gif,.webp"
                className="sr-only"
                onChange={(event) => void handleProductFileChange(event)}
              />
              {formState.imageUrl.trim() ? (
                <div className="aspect-video w-full overflow-hidden rounded-xl border border-orange-100 bg-orange-50">
                  <img
                    src={resolveApiMediaUrl(formState.imageUrl)}
                    alt={formState.name || '菜品预览'}
                    className="size-full object-cover"
                  />
                </div>
              ) : null}

              <div className="space-y-2">
                <Label htmlFor="product-name">商品名称</Label>
                <Input
                  id="product-name"
                  value={formState.name}
                  onChange={(event) => setFormState({ ...formState, name: event.target.value })}
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="product-image-url">商品图片链接</Label>
                <Input
                  id="product-image-url"
                  type="text"
                  inputMode="url"
                  placeholder="https://example.com/product.jpg"
                  value={formState.imageUrl}
                  onChange={(event) => setFormState({ ...formState, imageUrl: event.target.value })}
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="product-category">商品类别</Label>
                <Input
                  id="product-category"
                  value={formState.categoryName}
                  placeholder="例如：人气Top、精选套餐、饮品"
                  onChange={(event) => setFormState({ ...formState, categoryName: event.target.value })}
                />
              </div>

              <Button
                type="button"
                variant="secondary"
                disabled={saving}
                onClick={() => productFileInputRef.current?.click()}
              >
                <Upload className="size-4" />
                从本地上传菜品图
              </Button>

              <div className="space-y-2">
                <Label htmlFor="product-description">描述信息</Label>
                <Textarea
                  id="product-description"
                  value={formState.description}
                  onChange={(event) => setFormState({ ...formState, description: event.target.value })}
                />
              </div>

              <div className="grid gap-4 md:grid-cols-2">
                <div className="space-y-2">
                  <Label htmlFor="product-price">{formIsBundle ? '套餐默认最低价' : '价格'}</Label>
                  <Input
                    id="product-price"
                    type={formIsBundle ? 'text' : 'number'}
                    min="0"
                    step="0.01"
                    disabled={formIsBundle}
                    value={formIsBundle ? `¥${formBundlePrice.toFixed(2)}` : (formState.price === 0 ? '' : formState.price)}
                    onChange={(event) =>
                      setFormState({ ...formState, price: Number(event.target.value) || 0 })
                    }
                  />
                  {formIsBundle ? <p className="text-xs text-slate-500">根据每个套餐类别中价格最低的已选菜品自动计算。</p> : null}
                </div>

                <div className="space-y-2">
                  <Label htmlFor="product-stock">剩余库存</Label>
                  <Input
                    id="product-stock"
                    type="number"
                    min="0"
                    step="1"
                    value={formState.remainingStock === 0 ? '' : formState.remainingStock}
                    onChange={(event) =>
                      setFormState({ ...formState, remainingStock: Number(event.target.value) || 0 })
                    }
                  />
                </div>
              </div>

              <div className="space-y-2">
                <Label>上/下架状态</Label>
                <Select
                  value={formState.listingStatus}
                  onValueChange={(value: ListingStatus) => setFormState({ ...formState, listingStatus: value })}
                >
                  <SelectTrigger>
                    <SelectValue placeholder="请选择状态" />
                  </SelectTrigger>
                  <SelectContent>
                    {listingStatuses.map((status) => (
                      <SelectItem key={status} value={status}>
                        {status}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              <div className="space-y-3">
                <label className="flex cursor-pointer items-center gap-2 rounded-xl border border-orange-100 bg-orange-50/60 px-3 py-2 text-sm font-medium text-slate-800">
                  <input
                    type="checkbox"
                    className="size-4 accent-orange-500"
                    checked={(formState.bundleGroups ?? []).length > 0}
                    onChange={(event) => setFormState({ ...formState, bundleGroups: event.target.checked ? [createBundleGroup()] : [] })}
                  />
                  这是一个套餐商品
                </label>
                {(formState.bundleGroups ?? []).length > 0 ? (
                  <BundleGroupsEditor
                    groups={formState.bundleGroups ?? []}
                    products={editBundleProducts}
                    onChange={(bundleGroups) => setFormState({ ...formState, bundleGroups })}
                  />
                ) : null}
              </div>
            </div>
          ) : null}

          <DialogFooter className="shrink-0 border-t border-orange-100 px-6 py-4">
            <Button variant="outline" onClick={() => setEditingProduct(null)} disabled={saving}>
              取消
            </Button>
            <Button onClick={() => void handleSave()} disabled={!formState || saving}>
              保存修改
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
