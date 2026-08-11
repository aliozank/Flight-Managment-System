import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

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
  (response) => response,
  async (error) => {
    if (axios.isAxiosError(error) && error.response) {
      const status = error.response.status
      const data = error.response.data

      if (status === 401) {
        localStorage.removeItem('accessToken')
        localStorage.removeItem('authUser')
        ElMessage.error('Oturum süreniz doldu, lütfen tekrar giriş yapın.')
        if (router.currentRoute.value.path !== '/login') {
          await router.push('/login')
        }
        return Promise.reject(error)
      }

      if (status === 403) {
        ElMessage.error('Bu işlem için gerekli yetkiniz bulunmamaktadır (403 Forbidden).')
        return Promise.reject(error)
      }

      const msg = data?.message || data?.error || `İşlem başarısız (${status})`
      ElMessage.error(msg)
    } else {
      ElMessage.error('Sunucuya bağlanılamadı')
    }
    return Promise.reject(error)
  }
)

export default flightApi