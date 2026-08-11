import type { RoleName } from './auth'

export type UserStatus = 'ACTIVE' | 'INACTIVE' | 'LOCKED'

export interface UserResponse {
  userId: number
  userName: string
  userEmail: string
  userStatus: UserStatus
  userRoleNames: RoleName[]
  userCreatedAt?: string
  userLastLoginAt?: string
}

export interface RegisterRequest {
  userName: string
  userEmail: string
  userPassword: string
  userRoleNames: RoleName[]
}

export interface UserUpdateRequest {
  userStatus: UserStatus
  userRoleNames: RoleName[]
}
