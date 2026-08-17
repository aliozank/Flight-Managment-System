import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const archiveApi = axios.create({
  baseURL: '/archive-api',
  headers: {
    'Content-Type': 'application/json'
  }
})

archiveApi.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

archiveApi.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (axios.isAxiosError(error) && error.response) {
      const status = error.response.status
      const data = error.response.data

      if (status === 401) {
        localStorage.removeItem('accessToken')
        localStorage.removeItem('authUser')
        ElMessage.error('[401] Oturum süreniz doldu, lütfen tekrar giriş yapın.')
        if (router.currentRoute.value.path !== '/login') {
          await router.push('/login')
        }
        return Promise.reject(error)
      }

      if (status === 403) {
        ElMessage.error('[403] Bu işlem için gerekli yetkiniz bulunmamaktadır.')
        return Promise.reject(error)
      }

      const backendStatus = data?.status || status
      const backendMessage = data?.message || data?.error || 'Arşiv servisi işlemi başarısız'
      ElMessage.error(`[${backendStatus}] ${backendMessage}`)
    }
    return Promise.reject(error)
  }
)

export default archiveApi
