import { ArrowRightLeft } from 'lucide-react'

import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'

import { statusFlow } from './constants'

export function StatusFlowCard() {
  return (
    <Card className="border-orange-100 bg-white/95">
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <ArrowRightLeft className="size-5 text-orange-500" />
          订单状态流转
        </CardTitle>
        <CardDescription>标准状态：待接单 → 制作中 → 配送中 → 已完成</CardDescription>
      </CardHeader>
      <CardContent className="grid gap-3 md:grid-cols-4">
        {statusFlow.map((status) => (
          <div key={status} className="rounded-xl border border-orange-100 p-3 text-center">
            <p className="font-medium text-slate-900">{status}</p>
          </div>
        ))}
      </CardContent>
    </Card>
  )
}
