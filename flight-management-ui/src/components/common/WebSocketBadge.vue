<script setup lang="ts">
import { computed } from 'vue'
import { useWebSocketStore } from '@/stores/webSocketStore'

const wsStore = useWebSocketStore()

const badgeConfig = computed(() => {
  switch (wsStore.status) {
    case 'CONNECTED':
      return { type: 'success', label: 'Canlı Bağlantı' }
    case 'CONNECTING':
      return { type: 'warning', label: 'Bağlanıyor...' }
    case 'ERROR':
      return { type: 'danger', label: 'Bağlantı Hatası' }
    case 'DISCONNECTED':
    default:
      return { type: 'info', label: 'Bağlantı Yok' }
  }
})
</script>

<template>
  <div class="websocket-badge">
    <span class="status-dot" :class="wsStore.status.toLowerCase()" />
    <span class="status-label">{{ badgeConfig.label }}</span>
  </div>
</template>

<style scoped>
.websocket-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 4px 12px;
  background-color: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
  color: #e2e8f0;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #94a3b8;
}

.status-dot.connected {
  background-color: #10b981;
  box-shadow: 0 0 8px rgba(16, 185, 129, 0.6);
}

.status-dot.connecting {
  background-color: #f59e0b;
  animation: pulse 1.5s infinite;
}

.status-dot.error {
  background-color: #ef4444;
}

@keyframes pulse {
  0% {
    opacity: 0.4;
  }
  50% {
    opacity: 1;
  }
  100% {
    opacity: 0.4;
  }
}
</style>
