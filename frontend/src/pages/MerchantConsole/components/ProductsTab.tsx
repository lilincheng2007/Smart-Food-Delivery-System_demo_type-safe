import { useEffect, useMemo, useRef, useState, type ChangeEvent } from 'react'
import { ArrowLeft, ImageIcon, PackageSearch, Plus, Save, Store, TicketPercent, Upload } from 'lucide-react'

import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog'
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
import {
  PromotionEditorCard,
  createDefaultPromotion,
  defaultPromotionSchedule,
  nextPromotionDateValue,
  promotionUsageText,
  todayPromotionDateValue,
  validatePromotionSchedule,
} from '@/components/PromotionEditorCard'
import { PromotionDateInput, PromotionEnableControl } from '@/components/PromotionControls'
import { useAppChrome } from '@/hooks/useAppChrome'
import { resolveApiMediaUrl } from '@/lib/api-media-url'
import { getLocalImageFileError } from '@/lib/local-image-file'
import type { Product, ProductInventoryMode } from '@/objects/merchant/Product'
import type { ListingStatus, ProductId } from '@/objects/shared/ids'
import type { Promotion } from '@/objects/shared/Promotion'
import { cn } from '@/lib/utils'
import { useMerchantConsoleStore } from '@/pages/MerchantConsole/stores/use-merchant-console-store'

import { MerchantAICopywritingCard } from './MerchantAICopywritingCard'
import { BundleGroupsEditor } from './products/BundleGroupsEditor'
import {
  createBundleGroup,
  initialCreateFormState,
  inventoryModeOptions,
  inventoryText,
  isBundleProduct,
  listingStatuses,
  productCategoryName,
  productDiscountedPrice,
  productDiscountRateText,
  promotionSummary,
  roundMoney,
  sanitizeBundleGroups,
  serializePromotion,
  validateBundleGroups,
} from '../functions/productFormMapping'
import type { CreateProductFormState, ProductFormState, ProductsTabProps, StorePromotionDialogState } from '../objects/productDraft'

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
  const createProductFileInputRef = useRef<HTMLInputElement>(null)
  const [isCreateDialogOpen, setIsCreateDialogOpen] = useState(false)
  const [editingProduct, setEditingProduct] = useState<Product | null>(null)
  const [discountProduct, setDiscountProduct] = useState<Product | null>(null)
  const [productPromotionDraft, setProductPromotionDraft] = useState<Promotion | null>(null)
  const [storePromotionDialog, setStorePromotionDialog] = useState<StorePromotionDialogState | null>(null)
  const [storePromotionExitPromptOpen, setStorePromotionExitPromptOpen] = useState(false)
  const [formState, setFormState] = useState<ProductFormState | null>(null)
  const [createFormState, setCreateFormState] = useState<CreateProductFormState>(initialCreateFormState)
  const [createImageFile, setCreateImageFile] = useState<File | null>(null)
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
      remainingStock: editingProduct.inventoryMode === 'unlimited' ? 0 : editingProduct.remainingStock,
      listingStatus: editingProduct.listingStatus,
      inventoryMode: editingProduct.inventoryMode ?? 'finite',
      maxPerOrder: editingProduct.maxPerOrder ?? null,
      bundleGroups: editingProduct.bundleGroups ?? [],
    })
  }, [editingProduct])


  if (!selectedStore) {
    return (
      <Card className="border-orange-100 bg-white/95">
        <CardContent className="p-6 text-sm text-slate-600">请先选择店铺后查看菜品管理内容。</CardContent>
      </Card>
    )
  }

  const editBundleProducts = merchantProducts.filter((product) => product.id !== editingProduct?.id)
  const createIsBundle = createFormState.bundleGroups.length > 0
  const formIsBundle = (formState?.bundleGroups ?? []).length > 0
  const createSelectableProducts = merchantProducts.filter((product) => !isBundleProduct(product))
  const editSelectableProducts = editBundleProducts.filter((product) => !isBundleProduct(product))
  const createNormalizedBundleGroups = sanitizeBundleGroups(createFormState.bundleGroups, merchantProducts)
  const formNormalizedBundleGroups = sanitizeBundleGroups(formState?.bundleGroups ?? [], editBundleProducts)
  const createBundleIncomplete = createIsBundle && createNormalizedBundleGroups.length === 0
  const formBundleIncomplete = formIsBundle && formNormalizedBundleGroups.length === 0
  const createInventoryStock = createFormState.inventoryMode === 'unlimited' ? 0 : createFormState.remainingStock
  const formInventoryStock = formState?.inventoryMode === 'unlimited' ? 0 : (formState?.remainingStock ?? 0)
  const createSubmitDisabled = saving || !createFormState.name.trim() || createBundleIncomplete
  const formSubmitDisabled = !formState || saving || formBundleIncomplete

  const openCreateProductDialog = () => {
    setCreateImageFile(null)
    setCreateFormState({ ...initialCreateFormState, bundleGroups: [] })
    setIsCreateDialogOpen(true)
  }

  const openCreateBundleDialog = () => {
    setCreateImageFile(null)
    setCreateFormState({ ...initialCreateFormState, categoryName: '精选套餐', bundleGroups: [createBundleGroup()] })
    setIsCreateDialogOpen(true)
  }

  const closeCreateDialog = () => {
    setCreateImageFile(null)
    setCreateFormState({ ...initialCreateFormState, bundleGroups: [] })
    setIsCreateDialogOpen(false)
  }

  const handleSave = async () => {
    if (!editingProduct || !formState) {
      return
    }

    const normalizedBundleGroups = sanitizeBundleGroups(formState.bundleGroups ?? [], editBundleProducts)
    if (formIsBundle && editSelectableProducts.length === 0) {
      showNotice('请先保留至少一个普通菜品，再把商品设置为套餐。', 'error')
      return
    }
    if (formIsBundle && normalizedBundleGroups.length === 0) {
      showNotice('请至少添加一个套餐类别，并在类别中选择已有菜品。', 'error')
      return
    }
    const bundleValidationMessage = validateBundleGroups(normalizedBundleGroups, editBundleProducts)
    if (formIsBundle && bundleValidationMessage) {
      showNotice(bundleValidationMessage, 'error')
      return
    }

    setSaving(true)
    try {
      await onEditProduct(editingProduct.id, {
        ...formState,
        description: formState.description.trim(),
        price: formState.price,
        remainingStock: formInventoryStock,
        inventoryMode: formState.inventoryMode,
        maxPerOrder: formState.maxPerOrder,
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
    if (!createFormState.name.trim()) {
      return
    }

    const normalizedBundleGroups = sanitizeBundleGroups(createFormState.bundleGroups, merchantProducts)
    if (createIsBundle && createSelectableProducts.length === 0) {
      showNotice('请先创建普通菜品，再创建套餐。', 'error')
      return
    }
    if (createIsBundle && normalizedBundleGroups.length === 0) {
      showNotice('请至少添加一个套餐类别，并在类别中选择已有菜品。', 'error')
      return
    }
    const bundleValidationMessage = validateBundleGroups(normalizedBundleGroups, merchantProducts)
    if (createIsBundle && bundleValidationMessage) {
      showNotice(bundleValidationMessage, 'error')
      return
    }

    setSaving(true)
    try {
      const created = await onCreateProduct({
        merchantId: selectedStore.merchant.id,
        ...createFormState,
        description: createFormState.description.trim(),
        price: createFormState.price,
        remainingStock: createInventoryStock,
        inventoryMode: createFormState.inventoryMode,
        maxPerOrder: createFormState.maxPerOrder,
        bundleGroups: normalizedBundleGroups,
      })
      if (createImageFile) {
        await onUploadProductImage(created.id, createImageFile)
      }
      closeCreateDialog()
    } finally {
      setSaving(false)
    }
  }

  const handleCreateProductFileChange = (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0]
    event.target.value = ''
    if (!file) {
      return
    }

    const fileError = getLocalImageFileError(file)
    if (fileError) {
      showNotice(fileError, 'error')
      return
    }

    setCreateImageFile(file)
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
    const triggerType = next.discountType === 'productAmount' ? 'none' : (next.triggerType === 'items' ? 'items' : 'amount')
    const defaultTriggerValue = triggerType === 'items' ? 2 : 50
    return {
      ...next,
      triggerType,
      triggerValue: triggerType === 'none' ? 0 : Math.max(1, Number(next.triggerValue) || defaultTriggerValue),
      usageLimit,
      remainingUses: usageLimit === null ? null : Math.min(next.remainingUses ?? usageLimit, usageLimit),
      productIds: next.discountType === 'productAmount' ? (next.productIds ?? []) : [],
    }
  }

  const openStorePromotionDialog = (promotion: Promotion) => {
    setStorePromotionDialog({ mode: 'edit', promotion, original: promotion })
  }

  const handleAddPromotion = () => {
    const promotion = createDefaultPromotion('merchant-promo', '新优惠')
    setStorePromotionDialog({ mode: 'add', promotion, original: null })
  }

  const updateStorePromotionDraft = (patch: Partial<Promotion>) => {
    setStorePromotionDialog((current) => current ? { ...current, promotion: normalizePromotionPatch(current.promotion, patch) } : current)
  }

  const storePromotionDirty = storePromotionDialog ? serializePromotion(storePromotionDialog.promotion) !== serializePromotion(storePromotionDialog.original) : false

  const requestCloseStorePromotionDialog = () => {
    if (storePromotionDirty) {
      setStorePromotionExitPromptOpen(true)
      return
    }
    setStorePromotionDialog(null)
  }

  const saveStorePromotionDraft = async () => {
    if (!storePromotionDialog || !selectedMerchantId) return false
    const validationMessage = validatePromotionSchedule(storePromotionDialog.promotion)
    if (validationMessage) {
      showNotice(validationMessage, 'error')
      return false
    }
    const nextPromotions = storePromotionDialog.mode === 'add'
      ? [...promotions, storePromotionDialog.promotion]
      : promotions.map((promotion) => promotion.id === storePromotionDialog.promotion.id ? storePromotionDialog.promotion : promotion)
    try {
      await saveStorePromotions(selectedMerchantId, nextPromotions)
      setPromotionsDraft({ merchantId: selectedMerchantId, promotions: nextPromotions })
      showNotice('店铺优惠已提交保存。', 'success')
      setStorePromotionDialog({ ...storePromotionDialog, original: storePromotionDialog.promotion })
      return true
    } catch (error) {
      showNotice(error instanceof Error ? error.message : '保存店铺优惠失败', 'error')
      return false
    }
  }

  const saveAndCloseStorePromotionDialog = async () => {
    const saved = await saveStorePromotionDraft()
    if (saved) {
      setStorePromotionExitPromptOpen(false)
      setStorePromotionDialog(null)
    }
  }

  const discardAndCloseStorePromotionDialog = () => {
    setStorePromotionExitPromptOpen(false)
    setStorePromotionDialog(null)
  }

  const removeStorePromotion = async (promotionId: string) => {
    if (!selectedMerchantId) return
    const nextPromotions = promotions.filter((promotion) => promotion.id !== promotionId)
    try {
      await saveStorePromotions(selectedMerchantId, nextPromotions)
      setPromotionsDraft({ merchantId: selectedMerchantId, promotions: nextPromotions })
      showNotice('店铺优惠已删除。', 'success')
      setStorePromotionDialog(null)
    } catch (error) {
      showNotice(error instanceof Error ? error.message : '删除店铺优惠失败', 'error')
    }
  }

  const handleOpenProductPromotion = (product: Product) => {
    const existing = productPromotionFor(product.id)
    const maxDiscount = Math.max(0.01, roundMoney(product.price - 0.01))
    const schedule = defaultPromotionSchedule()
    const draft = existing ?? {
      id: `product-promo-${product.id}-${Date.now()}`,
      title: `${product.name}专属优惠`,
      discountType: 'productAmount',
      discountValue: Math.min(1, maxDiscount),
      triggerType: 'none',
      triggerValue: 0,
      startsAt: schedule.startsAt,
      endsAt: schedule.endsAt,
      dailyStartTime: schedule.dailyStartTime,
      dailyEndTime: schedule.dailyEndTime,
      productIds: [product.id],
      usageLimit: null,
      remainingUses: null,
      enabled: false,
    }
    setDiscountProduct(product)
    setProductPromotionDraft(draft)
  }

  const closeProductPromotionDialog = () => {
    setDiscountProduct(null)
    setProductPromotionDraft(null)
  }

  const validateProductPromotion = (product: Product, promotion: Promotion): string | null => {
    const scheduleValidationMessage = validatePromotionSchedule(promotion)
    if (scheduleValidationMessage) return scheduleValidationMessage
    const currentPrice = productDiscountedPrice(product, promotion)
    if (currentPrice <= 0) return '优惠后价格必须大于 0 元。'
    if (currentPrice >= product.price) return '优惠后价格必须小于原价。'
    return null
  }

  const updateProductPromotionDraft = (patch: Partial<Promotion>) => {
    if (!discountProduct || !productPromotionDraft) return
    const nextDiscountValue = patch.discountValue === undefined
      ? productPromotionDraft.discountValue
      : (Number.isFinite(patch.discountValue) ? roundMoney(patch.discountValue) : 0)
    const normalizedPromotion: Promotion = {
      ...productPromotionDraft,
      ...patch,
      title: `${discountProduct.name}专属优惠`,
      discountType: 'productAmount',
      productIds: [discountProduct.id],
      discountValue: nextDiscountValue,
      triggerType: 'none',
      triggerValue: 0,
    }
    setProductPromotionDraft(normalizedPromotion)
  }

  const handleSubmitProductPromotion = async () => {
    if (!discountProduct || !productPromotionDraft || !selectedMerchantId) return
    const validationMessage = validateProductPromotion(discountProduct, productPromotionDraft)
    if (validationMessage) {
      showNotice(validationMessage, 'error')
      return
    }

    const nextPromotions = [
      ...promotions.filter((promotion) => !(promotion.discountType === 'productAmount' && (promotion.productIds ?? []).includes(discountProduct.id)) && promotion.id !== productPromotionDraft.id),
      productPromotionDraft,
    ]
    try {
      await saveStorePromotions(selectedMerchantId, nextPromotions)
      setPromotionsDraft({ merchantId: selectedMerchantId, promotions: nextPromotions })
      showNotice('菜品优惠已提交保存。', 'success')
      closeProductPromotionDialog()
    } catch (error) {
      showNotice(error instanceof Error ? error.message : '保存菜品优惠失败', 'error')
    }
  }

  const handleRemoveProductPromotion = async () => {
    if (!discountProduct || !selectedMerchantId) return
    const nextPromotions = promotions.filter((promotion) => !(promotion.discountType === 'productAmount' && (promotion.productIds ?? []).includes(discountProduct.id)))
    try {
      await saveStorePromotions(selectedMerchantId, nextPromotions)
      setPromotionsDraft({ merchantId: selectedMerchantId, promotions: nextPromotions })
      showNotice('菜品优惠已删除。', 'success')
      closeProductPromotionDialog()
    } catch (error) {
      showNotice(error instanceof Error ? error.message : '删除菜品优惠失败', 'error')
    }
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
              <CardDescription>可新建菜品和套餐，或通过编辑统一修改商品名称、描述、库存、上/下架状态和价格</CardDescription>
            </div>
            <div className="flex flex-wrap justify-end gap-2">
              <Button onClick={openCreateProductDialog}>新建菜品</Button>
              <Button variant="secondary" onClick={openCreateBundleDialog}>新建套餐</Button>
            </div>
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
                                <p>{bundleProduct ? '套餐基础价' : '价格'}：¥{product.price.toFixed(2)} / 份</p>
                                <p>金额：¥{product.price.toFixed(2)}</p>
                                <p>{inventoryText(product)}</p>
                                <p>每单限购：{product.maxPerOrder ? `${product.maxPerOrder} 份` : '不限购'}</p>
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
        <CardHeader className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <CardTitle className="flex items-center gap-2">
              <TicketPercent className="size-5 text-orange-500" />
              店铺优惠
            </CardTitle>
            <CardDescription>首页仅展示只读优惠信息；进入管理后可编辑并提交保存。</CardDescription>
          </div>
          <Button type="button" variant="outline" onClick={handleAddPromotion}>
            <Plus className="size-4" />
            新增优惠
          </Button>
        </CardHeader>
        <CardContent className="space-y-2">
          {storePromotions.length === 0 ? <p className="text-sm text-slate-500">当前未设置店铺优惠。</p> : null}
          {storePromotions.map((promotion) => {
            const usageText = promotionUsageText(promotion)
            return (
              <div
                key={promotion.id}
                className={cn(
                  'flex min-w-0 items-center gap-2 rounded-xl border px-3 py-2 text-sm',
                  promotion.enabled ? 'border-orange-100 bg-orange-50/60 text-orange-800' : 'border-slate-200 bg-slate-100 text-slate-500',
                )}
              >
                <Badge variant={promotion.enabled ? 'default' : 'outline'} className="shrink-0">
                  {promotion.enabled ? '启用' : '停用'}
                </Badge>
                <span className="shrink-0 font-semibold">{promotion.title}</span>
                <span className="min-w-0 flex-1 truncate">
                  {promotionSummary(promotion)}{usageText ? ` · ${usageText}` : ''}
                </span>
                <Button type="button" size="sm" variant="outline" className="shrink-0" onClick={() => openStorePromotionDialog(promotion)}>
                  管理优惠
                </Button>
              </div>
            )
          })}
        </CardContent>
      </Card>

      <Dialog open={Boolean(storePromotionDialog)} onOpenChange={(open) => !open && requestCloseStorePromotionDialog()}>
        <DialogContent showCloseButton={false} className="flex max-h-[min(42rem,calc(100vh-2rem))] max-w-4xl flex-col overflow-hidden rounded-2xl border border-orange-100 bg-white p-0">
          <DialogHeader className="shrink-0 border-b border-orange-100 px-6 py-4">
            <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
              <div className="flex items-start gap-3">
                <Button type="button" variant="outline" size="sm" onClick={requestCloseStorePromotionDialog}>
                  <ArrowLeft className="size-4" />
                  返回
                </Button>
                <div>
                  <DialogTitle>{storePromotionDialog?.mode === 'add' ? '添加店铺优惠' : '管理店铺优惠'}</DialogTitle>
                  <DialogDescription>修改优惠内容或启停状态后，点击右上角提交才会保存。</DialogDescription>
                </div>
              </div>
              <Button type="button" onClick={() => void saveStorePromotionDraft()} disabled={!storePromotionDirty}>
                <Save className="size-4" />
                提交
              </Button>
            </div>
          </DialogHeader>
          <div className="min-h-0 flex-1 overflow-y-auto px-6 py-4">
            {storePromotionDialog ? (
              <PromotionEditorCard
                promotion={storePromotionDialog.promotion}
                onChange={(_, patch) => updateStorePromotionDraft(patch)}
                onRemove={(id) => void removeStorePromotion(id)}
              />
            ) : null}
          </div>
        </DialogContent>
      </Dialog>

      <AlertDialog open={storePromotionExitPromptOpen} onOpenChange={setStorePromotionExitPromptOpen}>
        <AlertDialogContent>
          <AlertDialogCancel className="mb-1 w-fit border-0 px-0 text-slate-500 shadow-none hover:bg-transparent" onClick={() => setStorePromotionExitPromptOpen(false)}>
            <ArrowLeft className="size-4" />
            返回优惠管理
          </AlertDialogCancel>
          <AlertDialogHeader>
            <AlertDialogTitle>有未提交的优惠修改</AlertDialogTitle>
            <AlertDialogDescription>离开前请确认是否保存。本次修改只有点击“提交”后才会生效。</AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogAction className="bg-slate-700 hover:bg-slate-800" onClick={discardAndCloseStorePromotionDialog}>
              不保存并退出
            </AlertDialogAction>
            <AlertDialogAction onClick={() => void saveAndCloseStorePromotionDialog()}>
              保存并退出
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      <Dialog open={Boolean(discountProduct)} onOpenChange={(open) => !open && closeProductPromotionDialog()}>
        <DialogContent className="flex max-h-[min(36rem,calc(100vh-2rem))] w-[calc(100vw-2rem)] max-w-2xl flex-col overflow-hidden rounded-2xl border border-orange-100 bg-white p-0">
          <DialogHeader className="shrink-0 px-6 pt-6">
            <DialogTitle>{discountProduct ? `${discountProduct.name}优惠` : '菜品优惠'}</DialogTitle>
            <DialogDescription>为当前菜品输入优惠后的现在金额，提交时会校验必须小于原价。</DialogDescription>
          </DialogHeader>
          {discountProduct && productPromotionDraft ? (
            (() => {
              const finite = productPromotionDraft.usageLimit !== null && productPromotionDraft.usageLimit !== undefined
              const currentPrice = productDiscountedPrice(discountProduct, productPromotionDraft)
              const discountRate = productDiscountRateText(discountProduct.price, currentPrice)
              const validationMessage = validateProductPromotion(discountProduct, productPromotionDraft)
              const startMinDate = todayPromotionDateValue()
              const endMinDate = nextPromotionDateValue(productPromotionDraft.startsAt) || nextPromotionDateValue(startMinDate)
              return (
                <div
                  className={cn(
                    'min-h-0 flex-1 space-y-3 overflow-y-auto px-6 py-4 transition-colors',
                    !productPromotionDraft.enabled && 'bg-slate-50 text-slate-500',
                  )}
                >
                  <div className="grid gap-3 md:grid-cols-[minmax(0,1fr)_8rem_8rem_8rem]">
                    <div className="space-y-1">
                      <Label>当前菜品</Label>
                      <Input value={discountProduct.name} disabled />
                    </div>
                    <div className="space-y-1">
                      <Label>原价</Label>
                      <Input value={`¥${discountProduct.price.toFixed(2)}`} disabled />
                    </div>
                    <div className="space-y-1">
                      <Label>现在金额</Label>
                      <Input
                        type="number"
                        min="0.01"
                        step="0.01"
                        value={Number.isFinite(currentPrice) ? currentPrice : ''}
                        onChange={(event) => {
                          const nextPrice = Number(event.target.value)
                          updateProductPromotionDraft({ discountValue: roundMoney(discountProduct.price - (Number.isFinite(nextPrice) ? nextPrice : 0)) })
                        }}
                      />
                    </div>
                    <div className="space-y-1">
                      <Label>当前折扣</Label>
                      <Input value={discountRate} disabled />
                    </div>
                  </div>
                  {validationMessage ? <p className="text-xs text-rose-600">{validationMessage}</p> : null}

                  <div className="grid gap-3 md:grid-cols-2 md:items-end">
                    <div className="space-y-1">
                      <Label>开始日期</Label>
                      <PromotionDateInput value={productPromotionDraft.startsAt} min={startMinDate} onChange={(value) => updateProductPromotionDraft({ startsAt: value })} />
                    </div>
                    <div className="space-y-1">
                      <Label>截止日期</Label>
                      <PromotionDateInput value={productPromotionDraft.endsAt} min={endMinDate} onChange={(value) => updateProductPromotionDraft({ endsAt: value })} />
                    </div>
                  </div>

                  <div className="grid gap-3 md:grid-cols-[minmax(0,1fr)_minmax(0,1fr)_9rem_8rem] md:items-end">
                    <div className="space-y-1">
                      <Label>每日开始时刻</Label>
                      <Input type="time" value={productPromotionDraft.dailyStartTime ?? ''} onChange={(event) => updateProductPromotionDraft({ dailyStartTime: event.target.value || null })} />
                      <p className="text-xs text-slate-500">若截止时间早于开始时间，将按次日截止计算。</p>
                    </div>
                    <div className="space-y-1">
                      <Label>每日截止时刻</Label>
                      <Input type="time" value={productPromotionDraft.dailyEndTime ?? ''} onChange={(event) => updateProductPromotionDraft({ dailyEndTime: event.target.value || null })} />
                      <p className="text-xs text-slate-500">开始时间不能与截止时间相同。</p>
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

                  <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
                    <p className={cn('min-w-0 break-words text-xs', productPromotionDraft.enabled ? 'text-orange-700' : 'text-slate-500')}>
                      {productPromotionDraft.enabled
                        ? `预览：${discountProduct.name} 原价 ¥${discountProduct.price.toFixed(2)}，现在 ¥${currentPrice.toFixed(2)}，${discountRate} · ${finite ? `${productPromotionDraft.remainingUses ?? productPromotionDraft.usageLimit}张优惠券` : '优惠'}`
                        : '已停用：商家端菜品卡片、顾客端和结算中不会显示或使用该优惠'}
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
            <Button type="button" onClick={() => void handleSubmitProductPromotion()}>提交优惠</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog open={isCreateDialogOpen} onOpenChange={(open) => (open ? setIsCreateDialogOpen(true) : closeCreateDialog())}>
        <DialogContent className="flex max-h-[min(42rem,calc(100vh-2rem))] max-w-2xl flex-col overflow-hidden rounded-2xl border border-orange-100 bg-white p-0">
          <DialogHeader className="shrink-0 px-6 pt-6">
            <DialogTitle>{createIsBundle ? '新建套餐' : '新建商品'}</DialogTitle>
            <DialogDescription>
              {createIsBundle ? '选择当前店铺已有普通菜品组成套餐，并设置套餐基础价。' : '创建普通菜品后，可继续用它组成套餐。'}
            </DialogDescription>
          </DialogHeader>

          <div className="min-h-0 flex-1 space-y-4 overflow-y-auto px-6 py-4">
            <input
              ref={createProductFileInputRef}
              type="file"
              accept="image/jpeg,image/png,image/gif,image/webp,.jpg,.jpeg,.png,.gif,.webp"
              className="sr-only"
              onChange={handleCreateProductFileChange}
            />
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
              <Label htmlFor="create-product-name">{createIsBundle ? '套餐名称' : '商品名称'}</Label>
              <Input
                id="create-product-name"
                placeholder={createIsBundle ? '例如：双人精选套餐' : '例如：招牌牛肉饭'}
                value={createFormState.name}
                onChange={(event) => setCreateFormState({ ...createFormState, name: event.target.value })}
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="create-product-image-url">{createIsBundle ? '套餐图片' : '菜品图片'}</Label>
              <Input
                id="create-product-image-url"
                type="text"
                inputMode="url"
                placeholder="可粘贴 https://example.com/product.jpg，或选择本地图片"
                value={createFormState.imageUrl}
                onChange={(event) => setCreateFormState({ ...createFormState, imageUrl: event.target.value })}
              />
              <div className="flex flex-wrap items-center gap-2">
                <Button
                  type="button"
                  variant="secondary"
                  disabled={saving}
                  onClick={() => createProductFileInputRef.current?.click()}
                >
                  <Upload className="size-4" />
                  {createIsBundle ? '上传本地套餐图' : '上传本地菜品图'}
                </Button>
                {createImageFile ? (
                  <span className="text-xs text-slate-500">已选择：{createImageFile.name}，创建后自动上传。</span>
                ) : null}
              </div>
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
                <Label htmlFor="create-product-price">{createIsBundle ? '套餐自定义价格' : '价格'}</Label>
                <Input
                  id="create-product-price"
                  type="number"
                  min="0"
                  step="0.01"
                  value={createFormState.price === 0 ? '' : createFormState.price}
                  onChange={(event) => {
                    const price = Number(event.target.value) || 0
                    setCreateFormState((current) => ({ ...current, price }))
                  }}
                />
                {createIsBundle ? <p className="text-xs text-slate-500">顾客支付套餐价，加上各类别中超出包含价的菜品加价。</p> : null}
              </div>

              <div className="space-y-2">
                <Label>库存模式</Label>
                <Select
                  value={createFormState.inventoryMode}
                  onValueChange={(value: ProductInventoryMode) => setCreateFormState({ ...createFormState, inventoryMode: value })}
                >
                  <SelectTrigger>
                    <SelectValue placeholder="请选择库存模式" />
                  </SelectTrigger>
                  <SelectContent>
                    {inventoryModeOptions.map((option) => (
                      <SelectItem key={option.value} value={option.value}>{option.label}</SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              <div className="space-y-2">
                <Label htmlFor="create-product-stock">今日库存数量</Label>
                <Input
                  id="create-product-stock"
                  type="number"
                  min="0"
                  step="1"
                  disabled={createFormState.inventoryMode !== 'finite'}
                  value={createFormState.inventoryMode !== 'finite' || createFormState.remainingStock === 0 ? '' : createFormState.remainingStock}
                  onChange={(event) =>
                    setCreateFormState({ ...createFormState, remainingStock: Number(event.target.value) || 0 })
                  }
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="create-product-max-per-order">每单限购数量</Label>
                <Input
                  id="create-product-max-per-order"
                  type="number"
                  min="1"
                  step="1"
                  placeholder="留空表示不限购"
                  value={createFormState.maxPerOrder ?? ''}
                  onChange={(event) => setCreateFormState({ ...createFormState, maxPerOrder: event.target.value ? Math.max(1, Number(event.target.value) || 1) : null })}
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
              {createIsBundle && createSelectableProducts.length === 0 ? (
                <p className="rounded-xl border border-dashed border-orange-200 bg-white/70 px-3 py-3 text-sm text-orange-700">
                  请先创建至少一个普通菜品，再回来选择已有菜品组成套餐。
                </p>
              ) : null}
              {createIsBundle && createSelectableProducts.length > 0 && createBundleIncomplete ? (
                <p className="rounded-xl border border-dashed border-orange-200 bg-white/70 px-3 py-3 text-sm text-orange-700">
                  请至少保留一个套餐类别，并在类别中选择已有普通菜品。
                </p>
              ) : null}
              {createFormState.bundleGroups.length > 0 ? (
                <BundleGroupsEditor
                  groups={createFormState.bundleGroups}
                  products={merchantProducts}
                  onChange={(bundleGroups) => setCreateFormState((current) => ({ ...current, bundleGroups }))}
                />
              ) : null}
            </div>
          </div>

          <DialogFooter className="shrink-0 border-t border-orange-100 px-6 py-4">
            <Button
              variant="outline"
              onClick={closeCreateDialog}
              disabled={saving}
            >
              取消
            </Button>
            <Button
              onClick={() => void handleCreate()}
              disabled={createSubmitDisabled}
            >
              {createIsBundle ? '创建套餐' : '创建商品'}
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
                从本地上传商品图
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
                  <Label htmlFor="product-price">{formIsBundle ? '套餐自定义价格' : '价格'}</Label>
                  <Input
                    id="product-price"
                    type="number"
                    min="0"
                    step="0.01"
                    value={formState.price === 0 ? '' : formState.price}
                    onChange={(event) => {
                      const price = Number(event.target.value) || 0
                      setFormState((current) => current ? { ...current, price } : current)
                    }}
                  />
                  {formIsBundle ? <p className="text-xs text-slate-500">顾客支付套餐价，加上各类别中超出包含价的菜品加价。</p> : null}
                </div>

                <div className="space-y-2">
                  <Label>库存模式</Label>
                  <Select
                    value={formState.inventoryMode}
                    onValueChange={(value: ProductInventoryMode) => setFormState({ ...formState, inventoryMode: value })}
                  >
                    <SelectTrigger>
                      <SelectValue placeholder="请选择库存模式" />
                    </SelectTrigger>
                    <SelectContent>
                      {inventoryModeOptions.map((option) => (
                        <SelectItem key={option.value} value={option.value}>{option.label}</SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>

                <div className="space-y-2">
                  <Label htmlFor="product-stock">今日库存数量</Label>
                  <Input
                    id="product-stock"
                    type="number"
                    min="0"
                    step="1"
                    disabled={formState.inventoryMode !== 'finite'}
                    value={formState.inventoryMode !== 'finite' || formState.remainingStock === 0 ? '' : formState.remainingStock}
                    onChange={(event) =>
                      setFormState({ ...formState, remainingStock: Number(event.target.value) || 0 })
                    }
                  />
                </div>

                <div className="space-y-2">
                  <Label htmlFor="product-max-per-order">每单限购数量</Label>
                  <Input
                    id="product-max-per-order"
                    type="number"
                    min="1"
                    step="1"
                    placeholder="留空表示不限购"
                    value={formState.maxPerOrder ?? ''}
                    onChange={(event) => setFormState({ ...formState, maxPerOrder: event.target.value ? Math.max(1, Number(event.target.value) || 1) : null })}
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
                    onChange={(event) => setFormState((current) => current ? { ...current, bundleGroups: event.target.checked ? [createBundleGroup()] : [] } : current)}
                  />
                  这是一个套餐商品
                </label>
                {formIsBundle && editSelectableProducts.length === 0 ? (
                  <p className="rounded-xl border border-dashed border-orange-200 bg-white/70 px-3 py-3 text-sm text-orange-700">
                    请先保留至少一个普通菜品，再把商品设置为套餐。
                  </p>
                ) : null}
                {formIsBundle && editSelectableProducts.length > 0 && formBundleIncomplete ? (
                  <p className="rounded-xl border border-dashed border-orange-200 bg-white/70 px-3 py-3 text-sm text-orange-700">
                    请至少保留一个套餐类别，并在类别中选择已有普通菜品。
                  </p>
                ) : null}
                {(formState.bundleGroups ?? []).length > 0 ? (
                  <BundleGroupsEditor
                    groups={formState.bundleGroups ?? []}
                    products={editBundleProducts}
                    onChange={(bundleGroups) => setFormState((current) => current ? { ...current, bundleGroups } : current)}
                  />
                ) : null}
              </div>
            </div>
          ) : null}

          <DialogFooter className="shrink-0 border-t border-orange-100 px-6 py-4">
            <Button variant="outline" onClick={() => setEditingProduct(null)} disabled={saving}>
              取消
            </Button>
            <Button onClick={() => void handleSave()} disabled={formSubmitDisabled}>
              保存修改
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
