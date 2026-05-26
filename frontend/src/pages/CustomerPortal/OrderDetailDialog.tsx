import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import type { Order } from '@/objects/order/Order'

type OrderDetailDialogProps = {
  selectedOrder: Order | null
  onOpenChange: (open: boolean) => void
  onClose: () => void
  onCancelOrder: (order: Order) => void
}

function canCancel(order: Order): boolean {
  return order.status !== '已取消' && order.status !== '已送达' && order.status !== '已完成' && order.status !== '配送中' && !order.riderId
}

export function OrderDetailDialog({ selectedOrder, onOpenChange, onClose, onCancelOrder }: OrderDetailDialogProps) {
  return (
    <Dialog open={selectedOrder !== null} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-md rounded-2xl border border-orange-100 bg-white p-6">
        <DialogHeader>
          <DialogTitle>订单详情</DialogTitle>
          <DialogDescription>{selectedOrder ? `订单号：${selectedOrder.id}` : '查看订单商品与金额信息'}</DialogDescription>
        </DialogHeader>
        {selectedOrder ? (
          <div className="space-y-3">
            <div className="rounded-xl bg-orange-50 px-3 py-2 text-sm text-slate-700">
              订单金额：
              <span className="ml-1 font-semibold text-orange-600">¥{selectedOrder.totalAmount.toFixed(2)}</span>
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
