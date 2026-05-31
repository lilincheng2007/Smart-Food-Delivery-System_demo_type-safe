import { APIMessage } from '@/api/shared/APIMessage'
import type { TaskIO } from '@/api/shared/TaskIO'
import { sendAPI } from '@/api/shared/sendAPI'
import type { AIOrderProgressNarrativesRequest } from '@/objects/ai/AIOrderProgressNarrativesRequest'
import type { AIOrderProgressNarrativesResponse } from '@/objects/ai/AIOrderProgressNarrativesResponse'

class AIOrderProgressNarrativesAPI extends APIMessage<AIOrderProgressNarrativesResponse> {
  readonly apiName = 'aiorderprogressnarrativesapi'

  constructor(_request: AIOrderProgressNarrativesRequest) {
    super()
  }
}

export function aiOrderProgressNarrativesIO(
  request: AIOrderProgressNarrativesRequest,
): TaskIO<AIOrderProgressNarrativesResponse> {
  return sendAPI(new AIOrderProgressNarrativesAPI(request))
}
