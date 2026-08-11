<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  status: string | undefined | null
}>()

const tagConfig = computed(() => {
  const s = props.status?.toUpperCase() || ''
  switch (s) {
    case 'SCHEDULED':
      return { type: 'primary', label: 'Planlandı', effect: 'light' }
    case 'DELAYED':
      return { type: 'warning', label: 'Rötarlı', effect: 'light' }
    case 'DEPARTED':
      return { type: 'info', label: 'Kalktı', effect: 'dark' }
    case 'ARRIVED':
      return { type: 'success', label: 'İndi', effect: 'dark' }
    case 'CANCELLED':
      return { type: 'danger', label: 'İptal Edildi', effect: 'dark' }
    case 'ACTIVE':
      return { type: 'success', label: 'Aktif', effect: 'light' }
    case 'INACTIVE':
      return { type: 'info', label: 'Pasif', effect: 'light' }
    case 'LOCKED':
      return { type: 'danger', label: 'Kilitli', effect: 'light' }
    default:
      return { type: 'info', label: props.status || '-', effect: 'plain' }
  }
})
</script>

<template>
  <el-tag
    :type="tagConfig.type as any"
    :effect="tagConfig.effect as any"
    size="small"
    round
  >
    {{ tagConfig.label }}
  </el-tag>
</template>
