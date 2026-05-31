import { APIMessage } from '@/api/shared/APIMessage'
import type { TaskIO } from '@/api/shared/TaskIO'
import { sendAPI } from '@/api/shared/sendAPI'
import type { AISearchRequest } from '@/objects/ai/AISearchRequest'
import type { AISearchResponse } from '@/objects/ai/AISearchResponse'

class AISearchAPI extends APIMessage<AISearchResponse> {
  readonly apiName = 'aisearchapi'
  readonly query: string

  constructor(request: AISearchRequest) {
    super()
    this.query = request.query
  }
}

export function aiSearchIO(request: AISearchRequest): TaskIO<AISearchResponse> {
  return sendAPI(new AISearchAPI(request))
}
