import { APIMessage } from '@/apis/shared/APIMessage'
import type { TaskIO } from '@/apis/shared/TaskIO'
import { sendAPI } from '@/apis/shared/sendAPI'
import type { AIOrderProgressNarrativesRequest } from '@/objects/ai/apiTypes/AIOrderProgressNarrativesRequest'
import type { AIOrderProgressNarrativesResponse } from '@/objects/ai/apiTypes/AIOrderProgressNarrativesResponse'

class AIOrderProgressNarrativesAPI extends APIMessage<AIOrderProgressNarrativesResponse> {
  readonly apiName = 'aiorderprogressnarrativesapi'

  constructor(request: AIOrderProgressNarrativesRequest) {
    super()
    Object.assign(this, request)
  }
}

export function aiOrderProgressNarrativesIO(
  request: AIOrderProgressNarrativesRequest,
): TaskIO<AIOrderProgressNarrativesResponse> {
  return sendAPI(new AIOrderProgressNarrativesAPI(request))
}
