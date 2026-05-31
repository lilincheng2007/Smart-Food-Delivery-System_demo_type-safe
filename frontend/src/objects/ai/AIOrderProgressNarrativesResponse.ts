import type { OrderStatus } from '@/objects/shared/ids'

export interface AIOrderProgressNarrativeGroup {
  status: OrderStatus
  messages: string[]
}

export interface AIOrderProgressNarrativesResponse {
  groups: AIOrderProgressNarrativeGroup[]
  generatedAt: string
  generatedFor: string
}
