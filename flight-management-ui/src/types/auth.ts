export type RoleName = 'ADMIN' | 'OPERATIONS' | 'BI_ANALYST' | 'DEVOPS' | 'ROLE_ADMIN' | 'ROLE_OPERATIONS' | 'ROLE_BI_ANALYST' | 'ROLE_DEVOPS'

export interface LoginRequest {
    userName?: string
    userEmail?: string
    userPassword: string
}

export interface AuthResponse {
    accessToken: string
    tokenType: string
    expiresIn: number
    userId: number
    userName: string
    userRoleNames: string[]
}