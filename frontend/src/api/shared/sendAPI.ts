import type { TaskIO } from './TaskIO'
import type { APIMessage } from './APIMessage'
import { apiPostIO } from './client'
import { apiNameOf } from './apiNameOf'

export function sendAPI<Response>(message: APIMessage<Response>): TaskIO<Response> {
  return apiPostIO(`/${apiNameOf(message)}`, message)
}
