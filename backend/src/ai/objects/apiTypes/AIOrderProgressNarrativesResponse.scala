package delivery.ai.objects.apiTypes

import delivery.domain.OrderStatus

final case class AIOrderProgressNarrativeGroup(
    status: OrderStatus,
    messages: List[String]
)

final case class AIOrderProgressNarrativesResponse(
    groups: List[AIOrderProgressNarrativeGroup],
    generatedAt: String,
    generatedFor: String
)
