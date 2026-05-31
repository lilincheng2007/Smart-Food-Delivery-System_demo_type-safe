package delivery.ai.objects

import delivery.shared.objects.OrderStatus

final case class AIOrderProgressNarrativeGroup(
    status: OrderStatus,
    messages: List[String]
)

final case class AIOrderProgressNarrativesResponse(
    groups: List[AIOrderProgressNarrativeGroup],
    generatedAt: String,
    generatedFor: String
)
