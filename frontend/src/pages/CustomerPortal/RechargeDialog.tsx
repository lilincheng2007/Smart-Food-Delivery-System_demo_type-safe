import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'

type RechargeDialogProps = {
  open: boolean
  amountInput: string
  onOpenChange: (open: boolean) => void
  onAmountChange: (value: string) => void
  onConfirm: () => void
}

export function RechargeDialog({
  open,
  amountInput,
  onOpenChange,
  onAmountChange,
  onConfirm,
}: RechargeDialogProps) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-md rounded-2xl border border-orange-100 bg-white p-6">
        <DialogHeader>
          <DialogTitle>余额充值</DialogTitle>
          <DialogDescription>输入充值金额并确认，金额会立即计入当前账户余额。</DialogDescription>
        </DialogHeader>
        <div className="space-y-2">
          <Label htmlFor="recharge-amount">充值金额（元）</Label>
          <Input
            id="recharge-amount"
            type="number"
            min="0"
            step="100"
            value={amountInput}
            placeholder="例如：100"
            onChange={(event) => onAmountChange(event.target.value)}
          />
          <p className="text-xs text-slate-500">可使用输入框右侧上下按钮按 100 调整金额。</p>
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            取消
          </Button>
          <Button onClick={onConfirm}>确认充值</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
