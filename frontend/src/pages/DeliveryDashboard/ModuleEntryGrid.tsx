import { useNavigate } from 'react-router-dom'

import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'

import { dashboardEntries } from './constants'

export function ModuleEntryGrid() {
  const navigate = useNavigate()

  return (
    <section className="grid gap-4 md:grid-cols-2">
      {dashboardEntries.map((entry) => {
        const Icon = entry.icon
        return (
          <Card key={entry.title} className="border-orange-100 bg-white/95 py-0">
            <CardContent className="space-y-4 p-5">
              <div className="flex items-center justify-between gap-2">
                <div className="flex items-center gap-3">
                  <span className="inline-flex size-10 items-center justify-center rounded-xl bg-orange-50 text-orange-600">
                    <Icon className="size-5" />
                  </span>
                  <div>
                    <h2 className="font-semibold text-slate-900">{entry.title}</h2>
                    <p className="text-sm text-slate-600">{entry.description}</p>
                  </div>
                </div>
                <Badge variant="outline" className="border-orange-100 text-orange-700">
                  核心模块
                </Badge>
              </div>
              <Button className="w-full" onClick={() => navigate(entry.path)}>
                进入模块
              </Button>
            </CardContent>
          </Card>
        )
      })}
    </section>
  )
}
