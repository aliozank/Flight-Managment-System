<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { useFlightStore } from '@/stores/flightStore'

const props = defineProps<{
  visible: boolean
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
}>()

const flightStore = useFlightStore()
const generating = ref(false)

const form = reactive({
  flightCount: 10,
  maximumFutureDays: 30
})

const handleGenerate = async () => {
  generating.value = true
  try {
    const flights = await flightStore.generateMockFlights({
      flightCount: form.flightCount,
      maximumFutureDays: form.maximumFutureDays
    })
    ElMessage.success(`${flights.length} adet mock uçuş başarıyla oluşturuldu!`)
    emit('update:visible', false)
  } catch {
    // Handled by axios interceptor
  } finally {
    generating.value = false
  }
}
</script>

<template>
  <el-dialog
    :model-value="visible"
    title="Simülasyon Verisi Üret (Mock Flight Generator)"
    width="500px"
    align-center
    :append-to-body="true"
    @update:model-value="emit('update:visible', $event)"
  >
    <el-form :model="form" label-position="top">
      <el-form-item label="Üretilecek Uçuş Sayısı (1 - 100)">
        <el-input-number
          v-model="form.flightCount"
          :min="1"
          :max="100"
          style="width: 100%"
        />
      </el-form-item>

      <el-form-item label="Maksimum Gelecek Gün Limiti (1 - 365 Gün)">
        <el-input-number
          v-model="form.maximumFutureDays"
          :min="1"
          :max="365"
          style="width: 100%"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="emit('update:visible', false)">Vazgeç</el-button>
      <el-button type="primary" :loading="generating" @click="handleGenerate">
        Uçuşları Üret
      </el-button>
    </template>
  </el-dialog>
</template>
