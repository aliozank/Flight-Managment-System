import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import flightApi from '@/services/api'
import type { AuthResponse, LoginRequest, RoleName } from '@/types/auth'

export interface UserInfo {
  userId: number
  userName: string
  roles: string[]
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem('accessToken'))
  const getInitialUser = (): UserInfo | null => {
    const saved = localStorage.getItem('authUser')
    if (saved) {
      try {
        return JSON.parse(saved)
      } catch {
        return null
      }
    }
    return null
  }

  const user = ref<UserInfo | null>(getInitialUser())

  const isAuthenticated = computed(() => !!token.value)
  const roles = computed<string[]>(() => user.value?.roles || [])

  const hasRole = (role: RoleName | string): boolean => {
    if (!roles.value || roles.value.length === 0) return false
    const cleanTarget = role.startsWith('ROLE_') ? role.substring(5) : role
    return roles.value.some((r) => {
      const cleanRole = r.startsWith('ROLE_') ? r.substring(5) : r
      return cleanRole === cleanTarget
    })
  }

  const isAdmin = computed(() => hasRole('ADMIN'))
  const isOperations = computed(() => hasRole('OPERATIONS'))
  const isBiAnalyst = computed(() => hasRole('BI_ANALYST'))
  const isDevops = computed(() => hasRole('DEVOPS'))

  const canManageFlights = computed(() => isAdmin.value || isOperations.value)
  const canCancelFlight = computed(() => isAdmin.value)
  const canManageReferenceData = computed(() => isAdmin.value)
  const canViewReferenceData = computed(() => isAdmin.value || isOperations.value || isBiAnalyst.value)
  const canViewMonitoring = computed(() => isAdmin.value || isDevops.value)
  const canManageUsers = computed(() => isAdmin.value)

  const login = async (loginRequest: LoginRequest): Promise<void> => {
    const response = await flightApi.post<AuthResponse>('/api/auth/login', loginRequest)
    const data = response.data

    token.value = data.accessToken
    localStorage.setItem('accessToken', data.accessToken)

    const userInfo: UserInfo = {
      userId: data.userId,
      userName: data.userName,
      roles: data.userRoleNames || []
    }

    user.value = userInfo
    localStorage.setItem('authUser', JSON.stringify(userInfo))
  }

  const logout = (): void => {
    token.value = null
    user.value = null
    localStorage.removeItem('accessToken')
    localStorage.removeItem('authUser')
  }

  return {
    token,
    user,
    isAuthenticated,
    roles,
    isAdmin,
    isOperations,
    isBiAnalyst,
    isDevops,
    canManageFlights,
    canCancelFlight,
    canManageReferenceData,
    canViewReferenceData,
    canViewMonitoring,
    canManageUsers,
    hasRole,
    login,
    logout
  }
})
