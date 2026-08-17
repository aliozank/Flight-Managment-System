import axios from 'axios'
import { handleApiError } from '@/services/api'

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
  (error) => handleApiError(error, 'Arşiv servisine bağlanılamadı')
)

export default archiveApi
