import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import type { PromotionCampaign } from '@/objects/admin'

interface CampaignListProps {
  campaigns: PromotionCampaign[]
  onCreateCampaign: () => void
}

export function CampaignList({ campaigns, onCreateCampaign }: CampaignListProps) {
  return (
    <Card className="border-orange-100 bg-white/95">
      <CardHeader>
        <CardTitle>当前活动概况</CardTitle>
        <CardDescription>运营经理已规划的促销活动</CardDescription>
      </CardHeader>
      <CardContent className="space-y-3">
        {campaigns.map((campaign) => (
          <div
            key={campaign.id}
            className="flex items-center justify-between rounded-xl border border-orange-100 px-4 py-3"
          >
            <div>
              <p className="font-medium text-slate-900">{campaign.title}</p>
              <p className="text-sm text-slate-600">面向：{campaign.target}</p>
            </div>
            <Badge variant="outline">{campaign.status}</Badge>
          </div>
        ))}
        <Button variant="outline" onClick={onCreateCampaign}>
          新增活动
        </Button>
      </CardContent>
    </Card>
  )
}
