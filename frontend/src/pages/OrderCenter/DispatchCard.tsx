import { Radar } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import type { Rider } from '@/objects/rider/Rider'

interface DispatchCardProps {
  riders: Rider[]
  onDispatch: () => void
}

export function DispatchCard({ riders, onDispatch }: DispatchCardProps) {
  return (
    <Card className="border-orange-100 bg-white/95">
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <Radar className="size-5 text-orange-500" />
          基础派单系统
        </CardTitle>
        <CardDescription>策略：最近 3 公里内最闲的 3 位骑手抢单</CardDescription>
      </CardHeader>
      <CardContent className="space-y-3">
        <p className="text-sm text-slate-700">
          当前可调度骑手：{riders.filter((item) => item.status !== '配送中').length} / {riders.length}
        </p>
        <Button onClick={onDispatch}>执行派单</Button>
      </CardContent>
    </Card>
  )
}
