import { BadgeAlert } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import type { CustomerServiceAgent } from '@/objects/admin'

interface CustomerServiceCardProps {
  agent: CustomerServiceAgent | undefined
  onResolveTicket: () => void
}

export function CustomerServiceCard({ agent, onResolveTicket }: CustomerServiceCardProps) {
  return (
    <Card className="border-orange-100 bg-white/95">
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <BadgeAlert className="size-5 text-orange-500" />
          客服信息
        </CardTitle>
        <CardDescription>
          {agent ? `${agent.name} · ${agent.department} · 渠道 ${agent.channel}` : '暂无数据'}
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-3">
        <Button onClick={onResolveTicket}>处理咨询 / 投诉</Button>
      </CardContent>
    </Card>
  )
}
