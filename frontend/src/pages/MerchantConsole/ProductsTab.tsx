import { useEffect, useState } from 'react'
import { PackageSearch, Store } from 'lucide-react'

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
import type { CreateProductRequest } from '@/objects/merchant/CreateProductRequest'
import type { MerchantStoreProfile } from '@/objects/merchant/MerchantStoreProfile'
import type { Product } from '@/objects/merchant/Product'
import type { UpdateProductRequest } from '@/objects/merchant/UpdateProductRequest'
import { ListingStatuses } from '@/objects/shared/ids'
import type { ListingStatus, ProductId } from '@/objects/shared/ids'

import { MerchantAICopywritingCard } from './MerchantAICopywritingCard'

type ProductsTabProps = {
  selectedStore: MerchantStoreProfile | null
  onCreateProduct: (input: CreateProductRequest) => Promise<void>
  onEditProduct: (productId: ProductId, input: UpdateProductRequest) => Promise<void>
}

const listingStatuses = Object.values(ListingStatuses)

type ProductFormState = UpdateProductRequest
type CreateProductFormState = {
  name: string
  description: string
  price: number
  remainingStock: number
  listingStatus: CreateProductRequest['listingStatus']
}

const initialCreateFormState: CreateProductFormState = {
  name: '',
  description: '',
  price: 0,
  remainingStock: 0,
  listingStatus: ListingStatuses.listed,
}

export function ProductsTab({ selectedStore, onCreateProduct, onEditProduct }: ProductsTabProps) {
  const merchantProducts = selectedStore?.products ?? []
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
                  <div className="space-y-3">
                    <div className="space-y-1">
                      <p className="font-medium text-slate-900">{product.name}</p>
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
        <DialogContent className="max-w-2xl rounded-2xl border border-orange-100 bg-white p-6">
          <DialogHeader>
            <DialogTitle>新建菜品</DialogTitle>
            <DialogDescription>填写新菜品的名称、描述、价格、库存和上/下架状态。</DialogDescription>
          </DialogHeader>

          <div className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="create-product-name">菜品名称</Label>
              <Input
                id="create-product-name"
                value={createFormState.name}
                onChange={(event) => setCreateFormState({ ...createFormState, name: event.target.value })}
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

          <DialogFooter>
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
        <DialogContent className="max-w-2xl rounded-2xl border border-orange-100 bg-white p-6">
          <DialogHeader>
            <DialogTitle>编辑菜品</DialogTitle>
            <DialogDescription>可修改菜品名称、描述信息、剩余库存、上/下架状态和价格。</DialogDescription>
          </DialogHeader>

          {formState ? (
            <div className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="product-name">菜品名称</Label>
                <Input
                  id="product-name"
                  value={formState.name}
                  onChange={(event) => setFormState({ ...formState, name: event.target.value })}
                />
              </div>

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

          <DialogFooter>
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
