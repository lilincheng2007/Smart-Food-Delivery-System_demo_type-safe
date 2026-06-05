import { useEffect, useState } from 'react'
import { Upload } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { resolveApiMediaUrl } from '@/lib/api-media-url'
import type { Order } from '@/objects/order/Order'
import type { OrderId } from '@/objects/shared/ids'

type OrderRefundDialogProps = {
  order: Order | null
  onOpenChange: (open: boolean) => void
  onUploadImage: (file: File) => Promise<{ ok: true; imageUrl: string } | { ok: false; message: string }>
  onSubmitRefund: (input: {
    orderId: OrderId
    reason: string
    imageUrl: string | null
  }) => Promise<{ ok: true } | { ok: false; message: string }>
  onNotice: (message: string, type?: 'success' | 'error' | 'info') => void
}

export function OrderRefundDialog({ order, onOpenChange, onUploadImage, onSubmitRefund, onNotice }: OrderRefundDialogProps) {
  const [reason, setReason] = useState('')
  const [imageUrl, setImageUrl] = useState('')
  const [isUploading, setIsUploading] = useState(false)
  const [isSubmitting, setIsSubmitting] = useState(false)

  useEffect(() => {
    if (order) {
      setReason('')
      setImageUrl('')
      setIsUploading(false)
      setIsSubmitting(false)
    }
  }, [order])

  const handleFileChange = async (file: File | undefined) => {
    if (!file) return
    setIsUploading(true)
    try {
      const result = await onUploadImage(file)
      if (result.ok) {
        setImageUrl(result.imageUrl)
        onNotice('退款凭证已上传。', 'success')
      } else {
        onNotice(result.message, 'error')
      }
    } finally {
      setIsUploading(false)
    }
  }

  const handleSubmit = async () => {
    if (!order) return
    if (!reason.trim()) {
      onNotice('请填写退款理由。', 'error')
      return
    }

    setIsSubmitting(true)
    try {
      const result = await onSubmitRefund({
        orderId: order.id,
        reason: reason.trim(),
        imageUrl: imageUrl.trim() || null,
      })
      if (result.ok) {
        onNotice('退款申请已提交，等待商家处理。', 'success')
        onOpenChange(false)
      } else {
        onNotice(result.message, 'error')
      }
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <Dialog open={order !== null} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-md rounded-2xl border border-orange-100 bg-white p-6">
        <DialogHeader>
          <DialogTitle>申请退款</DialogTitle>
          <DialogDescription>{order ? `订单号：${order.id}` : '提交退款理由和凭证'}</DialogDescription>
        </DialogHeader>
        <div className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="refund-reason">退款理由</Label>
            <Textarea
              id="refund-reason"
              value={reason}
              className="min-h-32 resize-y"
              placeholder="请说明退款原因，商家处理时会看到这段文字"
              onChange={(event) => setReason(event.target.value)}
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="refund-image">退款凭证图片</Label>
            <div className="flex gap-2">
              <Input
                id="refund-image"
                type="file"
                accept="image/jpeg,image/png,image/gif,image/webp"
                onChange={(event) => void handleFileChange(event.target.files?.[0])}
              />
              <Button type="button" variant="outline" disabled={isUploading}>
                <Upload className="size-4" />
                {isUploading ? '上传中' : '上传'}
              </Button>
            </div>
            {imageUrl ? (
              <img src={resolveApiMediaUrl(imageUrl)} alt="退款凭证" className="aspect-video w-full rounded-xl object-cover" />
            ) : null}
          </div>
        </div>
        <DialogFooter>
          <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
            取消
          </Button>
          <Button type="button" onClick={() => void handleSubmit()} disabled={isSubmitting || isUploading}>
            提交申请
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
