import { Bike } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import type { Rider } from '@/objects/rider'

interface DispatchCardProps {
  rider: Rider
  onGrabOrder: () => void
}

export function DispatchCard({ rider, onGrabOrder }: DispatchCardProps) {
  return (
    <Card className="border-orange-100 bg-white/95">
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <Bike className="size-5 text-orange-500" />
          抢单 / 系统派单
        </CardTitle>
        <CardDescription>基础派单：优先分配给 3 公里内最闲的骑手进行抢单</CardDescription>
      </CardHeader>
      <CardContent className="space-y-3">
        <div className="rounded-xl border border-orange-100 p-4 text-sm text-slate-700">
          当前定位：{rider.realtimeLocation} · 所属站点：{rider.station}
        </div>
        <Button onClick={onGrabOrder}>参与抢单</Button>
      </CardContent>
    </Card>
  )
}
