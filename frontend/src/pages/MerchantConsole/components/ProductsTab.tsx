import { useEffect, useRef, useState, type ChangeEvent } from 'react'
import { ImageIcon, PackageSearch, Store, Upload } from 'lucide-react'

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
import { useAppChrome } from '@/hooks/useAppChrome'
import { resolveApiMediaUrl } from '@/lib/api-media-url'
import { getLocalImageFileError } from '@/lib/local-image-file'
import type { CreateProductRequest } from '@/objects/merchant/apiTypes/CreateProductRequest'
import type { MerchantStoreProfile } from '@/objects/merchant/MerchantStoreProfile'
import type { Product } from '@/objects/merchant/Product'
import type { UpdateProductRequest } from '@/objects/merchant/apiTypes/UpdateProductRequest'
import { ListingStatuses } from '@/objects/shared/ids'
import type { ListingStatus, ProductId } from '@/objects/shared/ids'

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
}

const initialCreateFormState: CreateProductFormState = {
  name: '',
  description: '',
  imageUrl: '',
  categoryName: '默认分类',
  price: 0,
  remainingStock: 0,
  listingStatus: ListingStatuses.listed,
}

const productCategoryName = (product: Pick<Product, 'categoryName'>) => product.categoryName?.trim() || '默认分类'

export function ProductsTab({ selectedStore, onCreateProduct, onEditProduct, onUploadProductImage }: ProductsTabProps) {
  const { showNotice } = useAppChrome()
  const merchantProducts = selectedStore?.products ?? []
  const productFileInputRef = useRef<HTMLInputElement>(null)
  const [isCreateDialogOpen, setIsCreateDialogOpen] = useState(false)
  const [editingProduct, setEditingProduct] = useState<Product | null>(null)
  const [formState, setFormState] = useState<ProductFormState | null>(null)
  const [createFormState, setCreateFormState] = useState<CreateProductFormState>(initialCreateFormState)
  const [saving, setSaving] = useState(false)

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
    })
  }, [editingProduct])

  if (!selectedStore) {
    return (
      <Card className="border-orange-100 bg-white/95">
        <CardContent className="p-6 text-sm text-slate-600">请先选择店铺后查看菜品管理内容。</CardContent>
      </Card>
    )
  }

  const handleSave = async () => {
    if (!editingProduct || !formState) {
      return
    }

    setSaving(true)
    try {
      await onEditProduct(editingProduct.id, formState)
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

    setSaving(true)
    try {
      await onCreateProduct({
        merchantId: selectedStore.merchant.id,
        ...createFormState,
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
      })
    } finally {
      setSaving(false)
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
              <CardDescription>可新建菜品，或通过编辑统一修改菜品名称、描述、库存、上/下架状态和价格</CardDescription>
            </div>
            <Button onClick={() => setIsCreateDialogOpen(true)}>新建菜品</Button>
          </div>
        </CardHeader>
        <CardContent className="space-y-3">
          {merchantProducts.length === 0 ? (
            <p className="text-sm text-slate-500">当前店铺暂无商品，请先创建菜品。</p>
          ) : (
            merchantProducts.map((product) => (
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
                        <Badge variant="secondary">{productCategoryName(product)}</Badge>
                      </div>
                      <p className="text-sm text-slate-600">{product.description}</p>
                    </div>
                    <div className="grid gap-2 text-sm text-slate-600 md:grid-cols-2">
                      <p>价格：¥{product.price.toFixed(2)} / 份</p>
                      <p>金额：¥{product.price.toFixed(2)}</p>
                      <p>剩余库存：{product.remainingStock}</p>
                      <p>月销量：{product.monthlySales}</p>
                    </div>
                  </div>
                  <div className="flex flex-col items-end gap-2">
                    <Badge variant="outline">{product.listingStatus}</Badge>
                    <Button size="sm" onClick={() => setEditingProduct(product)}>
                      编辑
                    </Button>
                  </div>
                </div>
              </div>
            ))
          )}
        </CardContent>
      </Card>

      <Dialog open={isCreateDialogOpen} onOpenChange={setIsCreateDialogOpen}>
        <DialogContent className="flex max-h-[min(42rem,calc(100vh-2rem))] max-w-2xl flex-col overflow-hidden rounded-2xl border border-orange-100 bg-white p-0">
          <DialogHeader className="shrink-0 px-6 pt-6">
            <DialogTitle>新建菜品</DialogTitle>
            <DialogDescription>填写新菜品的名称、图片、描述、价格、库存和上/下架状态。</DialogDescription>
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
              <Label htmlFor="create-product-name">菜品名称</Label>
              <Input
                id="create-product-name"
                value={createFormState.name}
                onChange={(event) => setCreateFormState({ ...createFormState, name: event.target.value })}
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="create-product-image-url">菜品图片链接</Label>
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
              <Label htmlFor="create-product-category">菜品类别</Label>
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
                <Label htmlFor="create-product-price">价格</Label>
                <Input
                  id="create-product-price"
                  type="number"
                  min="0"
                  step="0.01"
                  value={createFormState.price === 0 ? '' : createFormState.price}
                  onChange={(event) =>
                    setCreateFormState({ ...createFormState, price: Number(event.target.value) || 0 })
                  }
                />
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
              创建菜品
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog open={!!editingProduct} onOpenChange={(open) => !open && setEditingProduct(null)}>
        <DialogContent className="flex max-h-[min(42rem,calc(100vh-2rem))] max-w-2xl flex-col overflow-hidden rounded-2xl border border-orange-100 bg-white p-0">
          <DialogHeader className="shrink-0 px-6 pt-6">
            <DialogTitle>编辑菜品</DialogTitle>
            <DialogDescription>可修改菜品名称、图片、描述信息、剩余库存、上/下架状态和价格。</DialogDescription>
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
                <Label htmlFor="product-name">菜品名称</Label>
                <Input
                  id="product-name"
                  value={formState.name}
                  onChange={(event) => setFormState({ ...formState, name: event.target.value })}
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="product-image-url">菜品图片链接</Label>
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
                <Label htmlFor="product-category">菜品类别</Label>
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
                  <Label htmlFor="product-price">价格</Label>
                  <Input
                    id="product-price"
                    type="number"
                    min="0"
                    step="0.01"
                    value={formState.price === 0 ? '' : formState.price}
                    onChange={(event) =>
                      setFormState({ ...formState, price: Number(event.target.value) || 0 })
                    }
                  />
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
