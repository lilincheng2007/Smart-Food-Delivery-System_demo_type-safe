import { APIMessage } from '@/api/shared/APIMessage'
import type { TaskIO } from '@/api/shared/TaskIO'
import { sendAPI } from '@/api/shared/sendAPI'
import type { CatalogResponse } from '@/objects/merchant/CatalogResponse'

class CatalogAPI extends APIMessage<CatalogResponse> {}

export function fetchCatalogIO(): TaskIO<CatalogResponse> {
  return sendAPI(new CatalogAPI())
}
