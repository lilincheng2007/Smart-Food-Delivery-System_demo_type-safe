import { ShieldCheck } from 'lucide-react'

import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'

interface SalaryCardProps {
  walletBalance: number
  salary: number
}

export function SalaryCard({ walletBalance, salary }: SalaryCardProps) {
  return (
    <Card className="border-orange-100 bg-white/95">
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <ShieldCheck className="size-5 text-orange-500" />
          薪资与合规
        </CardTitle>
      </CardHeader>
      <CardContent className="text-sm text-slate-700">
        当前账户余额：{walletBalance} 元；当月薪资（模拟）：{salary} 元。客服可在严重投诉场景下触发扣款流程。
      </CardContent>
    </Card>
  )
}
