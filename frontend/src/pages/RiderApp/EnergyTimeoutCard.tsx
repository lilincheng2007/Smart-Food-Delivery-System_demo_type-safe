import { BatteryCharging, ShieldCheck, Zap } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import type { Rider } from '@/objects/rider/Rider'

interface EnergyTimeoutCardProps {
  rider: Rider
  onRedeem: () => void
}

const ENERGY_COST = 100

export function EnergyTimeoutCard({ rider, onRedeem }: EnergyTimeoutCardProps) {
  const progress = Math.min(100, (rider.energyPoints / ENERGY_COST) * 100)
  const missingEnergy = Math.max(ENERGY_COST - rider.energyPoints, 0)
  const canRedeem = rider.energyPoints >= ENERGY_COST

  return (
    <Card className="overflow-hidden border-orange-100 bg-gradient-to-br from-amber-50 via-white to-orange-50 shadow-sm transition-transform duration-200 hover:-translate-y-0.5 hover:shadow-md">
      <CardHeader>
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <CardTitle className="flex items-center gap-2 text-slate-950">
              <span className="flex size-9 items-center justify-center rounded-2xl bg-gradient-to-br from-orange-500 to-amber-400 text-white shadow-lg shadow-orange-100">
                <Zap className="size-5" />
              </span>
              服务能量与免责卡
            </CardTitle>
            <CardDescription>送达 1 单 +10 能量，100 能量可兑换 1 张超时免责卡。</CardDescription>
          </div>
          <Button
            type="button"
            className="cursor-pointer bg-gradient-to-r from-orange-500 to-amber-500 text-white shadow-md shadow-orange-100 hover:brightness-105"
            disabled={!canRedeem}
            onClick={onRedeem}
          >
            <ShieldCheck className="size-4" />
            {canRedeem ? '兑换免责卡' : `还差 ${missingEnergy} 能量`}
          </Button>
        </div>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="grid gap-3 md:grid-cols-2">
          <div className="rounded-2xl border border-orange-100 bg-white/80 p-4">
            <p className="flex items-center gap-2 text-sm text-slate-500">
              <BatteryCharging className="size-4 text-orange-500" />
              当前能量
            </p>
            <p className="mt-2 text-3xl font-bold text-slate-950">{rider.energyPoints}</p>
          </div>
          <div className="rounded-2xl border border-orange-100 bg-white/80 p-4">
            <p className="flex items-center gap-2 text-sm text-slate-500">
              <ShieldCheck className="size-4 text-emerald-600" />
              超时免责卡
            </p>
            <p className="mt-2 text-3xl font-bold text-slate-950">{rider.timeoutCardCount}</p>
          </div>
        </div>
        <div className="space-y-2">
          <div className="flex items-center justify-between text-xs text-slate-500">
            <span>兑换进度</span>
            <span>{Math.min(rider.energyPoints, ENERGY_COST)} / {ENERGY_COST}</span>
          </div>
          <div className="h-3 overflow-hidden rounded-full bg-orange-100">
            <div
              className="h-full rounded-full bg-gradient-to-r from-orange-500 via-amber-400 to-emerald-400 transition-all duration-500"
              style={{ width: `${progress}%` }}
            />
          </div>
        </div>
        <div className="grid gap-3 text-sm text-slate-600 md:grid-cols-2">
          <p>累计超时：{rider.timeoutCount} 单</p>
          <p>已免责：{rider.timeoutExemptedCount} 单</p>
        </div>
      </CardContent>
    </Card>
  )
}
