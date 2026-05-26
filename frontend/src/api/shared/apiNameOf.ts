import type { APIMessage } from './APIMessage'

export function apiNameOf(message: APIMessage<unknown>): string {
  return message.constructor.name.toLowerCase()
}
