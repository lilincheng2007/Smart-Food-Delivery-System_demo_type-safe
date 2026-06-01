import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Separator } from '@/components/ui/separator'
import type { Order } from '@/objects/order/Order'
import { OrderStatuses } from '@/objects/shared/ids'
import type { OrderStatus } from '@/objects/shared/ids'

type OrderDetailDialogProps = {
  selectedOrder: Order | null
  onOpenChange: (open: boolean) => void
  onClose: () => void
  onCancelOrder: (order: Order) => void
  onCompleteOrder: (order: Order) => void
}

const nonCancelableStatuses: OrderStatus[] = [
  OrderStatuses.canceled,
  OrderStatuses.delivered,
  OrderStatuses.completed,
  OrderStatuses.delivering,
]

function canCancel(order: Order): boolean {
  return !nonCancelableStatuses.includes(order.status) && !order.riderId
}

function canComplete(order: Order): boolean {
  return order.status === OrderStatuses.delivered
}

function orderStatusDescription(order: Order): string | null {
  if (order.status === OrderStatuses.waitingForPickup) {
    return '商家已出餐，正在等待骑手接单取餐。'
  }
  if (order.status === OrderStatuses.delivered) {
    return '餐品已送达，可确认完成。'
  }
  return null
}

export function OrderDetailDialog({
  selectedOrder,
  onOpenChange,
  onClose,
  onCancelOrder,
  onCompleteOrder,
}: OrderDetailDialogProps) {
  return (
    <Dialog open={selectedOrder !== null} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-md rounded-2xl border border-orange-100 bg-white p-6">
        <DialogHeader>
          <DialogTitle>订单详情</DialogTitle>
          <DialogDescription>{selectedOrder ? `订单号：${selectedOrder.id}` : '查看订单商品与金额信息'}</DialogDescription>
        </DialogHeader>
        {selectedOrder ? (
          <div className="space-y-3">
            <div className="space-y-2 rounded-2xl border border-orange-100 bg-gradient-to-br from-orange-50 to-rose-50 px-4 py-3 text-sm text-slate-700">
              <div className="flex items-center justify-between">
                <span>商品原价</span>
                <span className="tabular-nums">¥{selectedOrder.originalAmount.toFixed(2)}</span>
              </div>
              <div className="flex items-center justify-between text-green-600">
                <span>优惠抵扣</span>
                <span className="tabular-nums">-¥{selectedOrder.discountAmount.toFixed(2)}</span>
              </div>
              {selectedOrder.usedVoucher ? (
                <p className="rounded-lg bg-white/70 px-2 py-1 text-xs text-orange-700">
                  已使用：{selectedOrder.usedVoucher.title}
                </p>
              ) : null}
              <Separator className="bg-orange-100" />
              <div className="flex items-center justify-between font-semibold text-orange-700">
                <span>实付金额</span>
                <span className="text-lg tabular-nums">¥{selectedOrder.payableAmount.toFixed(2)}</span>
              </div>
              <div className="flex items-center justify-between text-xs text-slate-500">
                <span>{selectedOrder.pointsAwarded > 0 ? '已获得积分' : '预计获得积分'}</span>
                <span>+{selectedOrder.pointsAwarded > 0 ? selectedOrder.pointsAwarded : Math.floor(selectedOrder.payableAmount)}</span>
              </div>
            </div>
            <div className="rounded-xl bg-orange-50 px-3 py-2 text-sm text-slate-700">
              当前状态：
              <span className="ml-1 font-semibold text-orange-600">{selectedOrder.status}</span>
              {orderStatusDescription(selectedOrder) ? (
                <p className="mt-1 text-xs text-slate-500">{orderStatusDescription(selectedOrder)}</p>
              ) : null}
            </div>
            <div className="space-y-2">
              <p className="text-sm font-medium text-slate-900">商品明细</p>
              {selectedOrder.items.map((item) => (
                <div
                  key={`${selectedOrder.id}-${item.productId}`}
                  className="flex items-center justify-between rounded-lg border border-orange-100 px-3 py-2"
                >
                  <div className="text-sm text-slate-700">
                    <p>{item.name}</p>
                    <p className="text-xs text-slate-500">x{item.quantity}</p>
                  </div>
                  <p className="text-sm font-medium text-slate-900">¥{(item.unitPrice * item.quantity).toFixed(2)}</p>
                </div>
              ))}
            </div>
          </div>
        ) : null}
        <DialogFooter>
          {selectedOrder && canComplete(selectedOrder) ? (
            <Button onClick={() => onCompleteOrder(selectedOrder)}>
              完成订单
            </Button>
          ) : null}
          {selectedOrder && canCancel(selectedOrder) ? (
            <Button variant="destructive" onClick={() => onCancelOrder(selectedOrder)}>
              取消订单
            </Button>
          ) : null}
          <Button variant="outline" onClick={onClose}>
            关闭
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
