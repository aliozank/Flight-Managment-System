<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage, type UploadFile } from 'element-plus'
import { useFlightStore } from '@/stores/flightStore'
import type { FlightCsvImportResponse } from '@/types/flight'

const props = defineProps<{
  visible: boolean
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
}>()

const flightStore = useFlightStore()
const selectedFile = ref<File | null>(null)
const uploading = ref(false)
const result = ref<FlightCsvImportResponse | null>(null)

const handleFileChange = (uploadFile: UploadFile) => {
  if (uploadFile.raw) {
    selectedFile.value = uploadFile.raw
    result.value = null
  }
}

const handleUpload = async () => {
  if (!selectedFile.value) {
    ElMessage.warning('Lütfen bir CSV dosyası seçin')
    return
  }

  uploading.value = true
  try {
    const res = await flightStore.uploadCsv(selectedFile.value)
    result.value = res
    ElMessage.success(`CSV İşlendi: ${res.successfulRowCount} başarılı, ${res.failedRowCount} hatalı`)
  } catch {
    // Handled by axios interceptor
  } finally {
    uploading.value = false
  }
}

const downloadSampleCsv = () => {
  const link = document.createElement('a')
  link.href = '/sample_flights.csv'
  link.download = 'sample_flights.csv'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  ElMessage.info('Örnek CSV dosyası indirildi (sample_flights.csv)')
}

const autoLoadSample = async () => {
  uploading.value = true
  try {
    const response = await fetch('/sample_flights.csv')
    const blob = await response.blob()
    const file = new File([blob], 'sample_flights.csv', { type: 'text/csv' })
    selectedFile.value = file
    const res = await flightStore.uploadCsv(file)
    result.value = res
    ElMessage.success(`Örnek CSV Başarıyla Yüklendi! ${res.successfulRowCount} adet gerçek uçuş eklendi.`)
  } catch {
    ElMessage.error('Örnek CSV yüklenirken bir hata oluştu')
  } finally {
    uploading.value = false
  }
}

const handleClose = () => {
  selectedFile.value = null
  result.value = null
  emit('update:visible', false)
}
</script>

<template>
  <el-dialog
    :model-value="visible"
    title="Toplu CSV Uçuş Yükleme"
    width="680px"
    align-center
    :before-close="handleClose"
    :append-to-body="true"
    @update:model-value="emit('update:visible', $event)"
  >
    <div class="upload-container">
      <!-- Quick sample loader bar -->
      <div class="sample-bar">
        <div class="sample-info">
          <span>💡 Henüz elinizde CSV yok mu?</span>
          <p>Gerçek uçuş verileri içeren örnek şablonumuzu hemen kullanabilirsiniz.</p>
        </div>
        <div class="sample-actions">
          <el-button size="small" type="info" plain @click="downloadSampleCsv">
            📥 Şablon İndir
          </el-button>
          <el-button size="small" type="success" :loading="uploading" @click="autoLoadSample">
            ⚡ Örnek Verileri Yükle
          </el-button>
        </div>
      </div>

      <el-upload
        drag
        action="#"
        :auto-upload="false"
        :on-change="handleFileChange"
        :show-file-list="true"
        accept=".csv"
        :limit="1"
      >
        <div class="upload-area">
          <div class="upload-icon">📁</div>
          <div class="upload-text">
            <span>Kendi CSV dosyanızı buraya sürükleyin veya <em>tıklayıp seçin</em></span>
            <p class="file-hint">Desteklenen başlıklar: flightNumber, airlineId, aircraftId, aircraftTypeId, originAirportId, destinationAirportId, flightTypeId, flightDate, scheduledDepartureTime, scheduledArrivalTime, scheduledArrivalDate</p>
          </div>
        </div>
      </el-upload>

      <!-- Sonuç Raporu -->
      <div v-if="result" class="result-card fade-in">
        <h4 class="result-title">İşlem Özeti Raporu</h4>
        <div class="stats-grid">
          <div class="stat-box">
            <span class="stat-value">{{ result.totalRowCount }}</span>
            <span class="stat-label">Toplam Satır</span>
          </div>
          <div class="stat-box success">
            <span class="stat-value">{{ result.successfulRowCount }}</span>
            <span class="stat-label">Başarılı Yüklenen</span>
          </div>
          <div class="stat-box danger">
            <span class="stat-value">{{ result.failedRowCount }}</span>
            <span class="stat-label">Hatalı Satır</span>
          </div>
        </div>

        <div v-if="result.errors && result.errors.length > 0" class="errors-list">
          <h5>Hata Detayları:</h5>
          <ul>
            <li v-for="(err, idx) in result.errors" :key="idx">{{ err }}</li>
          </ul>
        </div>
      </div>
    </div>

    <template #footer>
      <el-button @click="handleClose">Kapat</el-button>
      <el-button
        type="primary"
        :loading="uploading"
        :disabled="!selectedFile"
        @click="handleUpload"
      >
        CSV'yi Yükle ve İşle ➔
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.upload-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.sample-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 18px;
  background-color: #f0f9ff;
  border: 1px solid #bae6fd;
  border-radius: 10px;
}

.sample-info span {
  font-size: 13px;
  font-weight: 700;
  color: #0369a1;
}

.sample-info p {
  margin: 2px 0 0 0;
  font-size: 12px;
  color: #0284c7;
}

.sample-actions {
  display: flex;
  gap: 8px;
}

.upload-area {
  padding: 20px;
  text-align: center;
}

.upload-icon {
  font-size: 36px;
  margin-bottom: 8px;
}

.file-hint {
  font-size: 11px;
  color: #94a3b8;
  margin-top: 6px;
  line-height: 1.4;
}

.result-card {
  padding: 18px;
  background-color: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.04);
}

.result-title {
  margin: 0 0 14px 0;
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-bottom: 16px;
}

.stat-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 12px;
  background-color: #f8fafc;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
}

.stat-value {
  font-size: 22px;
  font-weight: 800;
  color: #0f172a;
}

.stat-label {
  font-size: 12px;
  color: #64748b;
  font-weight: 500;
}

.stat-box.success .stat-value {
  color: #10b981;
}

.stat-box.danger .stat-value {
  color: #ef4444;
}

.errors-list {
  max-height: 160px;
  overflow-y: auto;
  background-color: #fef2f2;
  border: 1px solid #fecaca;
  padding: 12px;
  border-radius: 8px;
}

.errors-list h5 {
  margin: 0 0 8px 0;
  color: #991b1b;
  font-size: 13px;
}

.errors-list ul {
  margin: 0;
  padding-left: 18px;
  color: #7f1d1d;
  font-size: 12px;
}
</style>
