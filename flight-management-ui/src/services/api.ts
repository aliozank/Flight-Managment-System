import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

let authFailureHandled = false
let lastErrorKey = ''
let lastErrorAt = 0

const showErrorOnce = (key: string, message: string): void => {
  const now = Date.now()
  if (key === lastErrorKey && now - lastErrorAt < 2000) return

  lastErrorKey = key
  lastErrorAt = now
  ElMessage.error(message)
}

export const handleApiError = async (
  error: unknown,
  fallbackMessage = 'İşlem başarısız'
): Promise<never> => {
  if (axios.isAxiosError(error) && error.response) {
    const status = error.response.status
    const data = error.response.data
    const backendStatus = data?.status || status
    const backendMessage = data?.message || data?.error || fallbackMessage

    if (status === 401) {
      if (error.config?.url === '/api/auth/login') {
        showErrorOnce('login-401', `[${backendStatus}] ${backendMessage}`)
        return Promise.reject(error)
      }

      if (!authFailureHandled) {
        authFailureHandled = true
        const redirectPath = router.currentRoute.value.fullPath
        localStorage.removeItem('accessToken')
        localStorage.removeItem('authUser')
        ElMessage.closeAll()
        ElMessage.error('[401] Oturum süreniz doldu, lütfen tekrar giriş yapın.')

        if (router.currentRoute.value.path !== '/login') {
          await router.replace({
            path: '/login',
            query: { redirect: redirectPath }
          })
        }
      }
      return Promise.reject(error)
    }

    if (status === 403) {
      showErrorOnce('403', '[403] Bu işlem için gerekli yetkiniz bulunmamaktadır.')
      return Promise.reject(error)
    }

    showErrorOnce(`${backendStatus}-${backendMessage}`, `[${backendStatus}] ${backendMessage}`)
  } else {
    showErrorOnce('network-error', fallbackMessage)
  }

  return Promise.reject(error)
}

const flightApi = axios.create({
  baseURL: '/flight-api',
  headers: {
    'Content-Type': 'application/json'
  }
})

flightApi.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

flightApi.interceptors.response.use(
  (response) => {
    if (response.config.url === '/api/auth/login') {
      authFailureHandled = false
    }
    return response
  },
  (error) => handleApiError(error, 'Sunucuya bağlanılamadı')
)

export default flightApi
