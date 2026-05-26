import { APIMessage } from '@/api/shared/APIMessage'
import type { TaskIO } from '@/api/shared/TaskIO'
import { sendAPI } from '@/api/shared/sendAPI'
import type { LoginResponse } from '@/objects/user/LoginResponse'
import type { UserRole } from '@/objects/shared/ids'

class LoginAPI extends APIMessage<LoginResponse> {
  readonly role: UserRole
  readonly username: string
  readonly password: string

  constructor(role: UserRole, username: string, password: string) {
    super()
    this.role = role
    this.username = username
    this.password = password
  }
}

export function loginIO(input: { role: UserRole; username: string; password: string }): TaskIO<LoginResponse> {
  return sendAPI(new LoginAPI(input.role, input.username, input.password))
}
