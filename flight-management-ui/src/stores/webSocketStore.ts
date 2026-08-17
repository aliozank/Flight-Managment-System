import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { useAuthStore } from './authStore'
import { useFlightStore } from './flightStore'
import type { FlightResponse } from '@/types/flight'

export type WebSocketStatus = 'DISCONNECTED' | 'CONNECTING' | 'CONNECTED' | 'ERROR'

export interface NotificationItem {
  id: string
  title: string
  message: string
  type: 'info' | 'success' | 'warning' | 'danger'
  timestamp: string
  read: boolean
  flightNumber?: string
}

export const useWebSocketStore = defineStore('webSocket', () => {
  const status = ref<WebSocketStatus>('DISCONNECTED')
  const notifications = ref<NotificationItem[]>([])
  const unreadCount = computed(() => notifications.value.filter((n) => !n.read).length)

  let ws: WebSocket | null = null
  let reconnectTimer: ReturnType<typeof setTimeout> | null = null
  let connectionTimeoutTimer: ReturnType<typeof setTimeout> | null = null

  const addNotification = (flight: FlightResponse) => {
    const timeStr = new Date().toLocaleTimeString('tr-TR', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
    let notifType: 'info' | 'success' | 'warning' | 'danger' = 'info'
    let title = `Uçuş Olayı: ${flight.flightNumber}`
    let message = `Uçuş #${flight.flightNumber} güncellendi (v${flight.flightVersion}).`

    if (flight.flightVersion === 1) {
      notifType = 'success'
      title = `✨ Yeni Uçuş Oluşturuldu: ${flight.flightNumber}`
      message = `Uçuş #${flight.flightNumber} sisteme başarıyla kaydedildi ve yayınlandı.`
    } else if (flight.flightStatus === 'CANCELLED') {
      notifType = 'danger'
      title = `❌ Uçuş İptal Edildi: ${flight.flightNumber}`
      message = `Uçuş #${flight.flightNumber} iptal durumuna alındı.`
    } else if (flight.flightStatus === 'DEPARTED') {
      notifType = 'warning'
      title = `🛫 Uçuş Kalkış Yaptı: ${flight.flightNumber}`
      message = `Uçuş #${flight.flightNumber} havada / kalkış yaptı.`
    } else if (flight.flightStatus === 'ARRIVED') {
      notifType = 'success'
      title = `🛬 Uçuş İniş Yaptı: ${flight.flightNumber}`
      message = `Uçuş #${flight.flightNumber} varış havalimanına indi.`
    } else {
      notifType = 'info'
      title = `🔄 Uçuş Güncellendi: ${flight.flightNumber}`
      message = `Uçuş #${flight.flightNumber} detayları güncellendi (v${flight.flightVersion}).`
    }

    const item: NotificationItem = {
      id: Math.random().toString(36).substring(2, 9),
      title,
      message,
      type: notifType,
      timestamp: timeStr,
      read: false,
      flightNumber: flight.flightNumber
    }

    notifications.value.unshift(item)
    // Keep max 30 recent notifications
    if (notifications.value.length > 30) {
      notifications.value.pop()
    }
  }

  const markAllAsRead = () => {
    notifications.value.forEach((n) => (n.read = true))
  }

  const clearNotifications = () => {
    notifications.value = []
  }

  const connect = (): void => {
    const authStore = useAuthStore()
    if (!authStore.token) {
      status.value = 'DISCONNECTED'
      return
    }

    if (ws && (ws.readyState === WebSocket.OPEN || ws.readyState === WebSocket.CONNECTING)) {
      return
    }

    status.value = 'CONNECTING'

    // Use the same-origin proxy in both Vite development and the Nginx container.
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const wsUrl = `${protocol}//${window.location.host}/flight-api/ws`

    const trySocket = (url: string) => {
      try {
        const socket = new WebSocket(url)
        let wasConnected = false

        ws = socket

        if (connectionTimeoutTimer) clearTimeout(connectionTimeoutTimer)
        connectionTimeoutTimer = setTimeout(() => {
          if (ws === socket && !wasConnected) {
            socket.close()
          }
        }, 3000)

        socket.onopen = () => {
          if (ws !== socket) return

          const connectFrame =
            `CONNECT\n` +
            `accept-version:1.2,1.1,1.0\n` +
            `heart-beat:10000,10000\n` +
            `Authorization:Bearer ${authStore.token}\n\n` +
            `\x00`

          socket.send(connectFrame)
        }

        socket.onmessage = (event) => {
          if (ws !== socket) return

          const raw = event.data as string
          if (raw.startsWith('CONNECTED')) {
            wasConnected = true
            status.value = 'CONNECTED'
            if (connectionTimeoutTimer) {
              clearTimeout(connectionTimeoutTimer)
              connectionTimeoutTimer = null
            }
            const subscribeFrame =
              `SUBSCRIBE\n` +
              `id:sub-0\n` +
              `destination:/topic/flights\n\n` +
              `\x00`
            socket.send(subscribeFrame)
          } else if (raw.startsWith('MESSAGE')) {
            const bodyIndex = raw.indexOf('\n\n')
            if (bodyIndex !== -1) {
              let body = raw.substring(bodyIndex + 2)
              if (body.endsWith('\x00')) {
                body = body.substring(0, body.length - 1)
              }
              try {
                const flight: FlightResponse = JSON.parse(body)
                const flightStore = useFlightStore()
                const updated = flightStore.upsertFlightFromWebSocket(flight)

                if (updated) {
                  addNotification(flight)
                }
              } catch {
                // Silent parse fail
              }
            }
          }
        }

        socket.onerror = () => {
          if (ws !== socket) return

          socket.close()
        }

        socket.onclose = () => {
          if (ws !== socket) return

          if (connectionTimeoutTimer) {
            clearTimeout(connectionTimeoutTimer)
            connectionTimeoutTimer = null
          }

          ws = null

          status.value = 'DISCONNECTED'
          scheduleReconnect()
        }
      } catch {
        status.value = 'ERROR'
        scheduleReconnect()
      }
    }

    trySocket(wsUrl)
  }

  const scheduleReconnect = (): void => {
    if (reconnectTimer) clearTimeout(reconnectTimer)
    reconnectTimer = setTimeout(() => {
      const authStore = useAuthStore()
      if (authStore.isAuthenticated && status.value !== 'CONNECTED') {
        connect()
      }
    }, 10000)
  }

  const disconnect = (): void => {
    if (reconnectTimer) {
      clearTimeout(reconnectTimer)
      reconnectTimer = null
    }
    if (connectionTimeoutTimer) {
      clearTimeout(connectionTimeoutTimer)
      connectionTimeoutTimer = null
    }
    if (ws) {
      const socket = ws
      ws = null
      socket.close()
    }
    status.value = 'DISCONNECTED'
  }

  return {
    status,
    notifications,
    unreadCount,
    markAllAsRead,
    clearNotifications,
    connect,
    disconnect
  }
})
