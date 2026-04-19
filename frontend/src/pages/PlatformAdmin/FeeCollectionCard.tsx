import { CircleDollarSign } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'

interface FeeCollectionCardProps {
  merchantApplicationCount: number
}

export function FeeCollectionCard({ merchantApplicationCount }: FeeCollectionCardProps) {
  return (
    <Card className="border-orange-100 bg-white/95">
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <CircleDollarSign className="size-5 text-orange-500" />
          商家加盟费收取
        </CardTitle>
      </CardHeader>
      <CardContent className="space-y-3">
        <p className="text-sm text-slate-700">当前待缴纳加盟费商家：{merchantApplicationCount} 家</p>
        <Button variant="outline">发起收款</Button>
      </CardContent>
    </Card>
  )
}
