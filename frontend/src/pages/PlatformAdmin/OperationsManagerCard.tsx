import { Handshake, Megaphone, UserCog } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import type { OperationsManager } from '@/objects/admin'

interface OperationsManagerCardProps {
  manager: OperationsManager | undefined
  onReviewMerchant: () => void
  onCreateCampaign: () => void
}

export function OperationsManagerCard({
  manager,
  onReviewMerchant,
  onCreateCampaign,
}: OperationsManagerCardProps) {
  return (
    <Card className="border-orange-100 bg-white/95">
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <UserCog className="size-5 text-orange-500" />
          运营经理信息
        </CardTitle>
        <CardDescription>
          {manager
            ? `${manager.name} · 区域 ${manager.region} · 管辖商家 ${manager.managedMerchantIds.length}`
            : '暂无数据'}
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-3">
        <Button onClick={onReviewMerchant}>
          <Handshake className="size-4" />
          审核入驻申请
        </Button>
        <Button variant="outline" onClick={onCreateCampaign}>
          <Megaphone className="size-4" />
          发放活动
        </Button>
      </CardContent>
    </Card>
  )
}
