import axios from 'axios'
import { handleApiError } from '@/services/api'

const referenceApi = axios.create({
  baseURL: '/reference-api',
  headers: {
    'Content-Type': 'application/json'
  }
})

referenceApi.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

referenceApi.interceptors.response.use(
  (response) => response,
  (error) => handleApiError(error, 'Referans servisine bağlanılamadı')
)

export default referenceApi
