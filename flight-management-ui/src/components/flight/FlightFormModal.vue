<script setup lang="ts">
import { ref, reactive, watch, onMounted, onUnmounted, computed } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useReferenceStore } from '@/stores/referenceStore'
import type { FlightResponse, FlightCreateRequest, FlightUpdateRequest, FlightStatus } from '@/types/flight'

const props = defineProps<{
  visible: boolean
  flightToEdit?: FlightResponse | null
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'save-create', payload: FlightCreateRequest): void
  (e: 'save-update', payload: { id: number; data: FlightUpdateRequest }): void
}>()

const referenceStore = useReferenceStore()
const formRef = ref<FormInstance>()
const saving = ref(false)
const isDirty = ref(false)

const isEditMode = computed(() => !!props.flightToEdit)

const form = reactive({
  flightNumber: '',
  airlineId: null as number | null,
  aircraftId: null as number | null,
  aircraftTypeId: null as number | null,
  originAirportId: null as number | null,
  destinationAirportId: null as number | null,
  flightTypeId: null as number | null,
  flightDate: '',
  scheduledDepartureTime: '',
  scheduledArrivalTime: '',
  flightStatus: 'SCHEDULED' as FlightStatus
})

// Regex: 2 uppercase letters/digits + 4 digits (e.g. TK1234, 8A9999)
const flightNumberRegex = /^[A-Z0-9]{2}\d{4}$/

const validateFlightNumber = (_rule: any, value: string, callback: any) => {
  if (!value) {
    callback(new Error('Uçuş numarası zorunludur'))
  } else if (!flightNumberRegex.test(value)) {
    callback(new Error('Uçuş numarası 2 harf/rakam ve 4 rakamdan oluşmalıdır (Örn: TK1234)'))
  } else {
    callback()
  }
}

const validateAirports = (_rule: any, _value: any, callback: any) => {
  if (form.originAirportId && form.destinationAirportId && form.originAirportId === form.destinationAirportId) {
    callback(new Error('Kalkış ve varış havalimanı aynı olamaz'))
  } else {
    callback()
  }
}

const validateTimes = (_rule: any, _value: any, callback: any) => {
  if (form.scheduledDepartureTime && form.scheduledArrivalTime) {
    if (form.scheduledArrivalTime <= form.scheduledDepartureTime) {
      callback(new Error('Varış saati (STA) kalkış saatinden (STD) sonra olmalıdır'))
      return
    }
  }
  callback()
}

const rules = reactive<FormRules>({
  flightNumber: [{ validator: validateFlightNumber, trigger: 'blur' }],
  airlineId: [{ required: true, message: 'Havayolu seçimi zorunludur', trigger: 'change' }],
  aircraftTypeId: [{ required: true, message: 'Uçak tipi seçimi zorunludur', trigger: 'change' }],
  originAirportId: [
    { required: true, message: 'Kalkış havalimanı zorunludur', trigger: 'change' },
    { validator: validateAirports, trigger: 'change' }
  ],
  destinationAirportId: [
    { required: true, message: 'Varış havalimanı zorunludur', trigger: 'change' },
    { validator: validateAirports, trigger: 'change' }
  ],
  flightTypeId: [{ required: true, message: 'Uçuş tipi seçimi zorunludur', trigger: 'change' }],
  flightDate: [{ required: true, message: 'Uçuş tarihi zorunludur', trigger: 'change' }],
  scheduledDepartureTime: [{ required: true, message: 'Kalkış saati zorunludur', trigger: 'change' }],
  scheduledArrivalTime: [
    { required: true, message: 'Varış saati zorunludur', trigger: 'change' },
    { validator: validateTimes, trigger: 'change' }
  ]
})

const resetForm = () => {
  if (props.flightToEdit) {
    form.flightNumber = props.flightToEdit.flightNumber
    form.airlineId = props.flightToEdit.airlineId
    form.aircraftId = props.flightToEdit.aircraftId
    form.aircraftTypeId = props.flightToEdit.aircraftTypeId
    form.originAirportId = props.flightToEdit.originAirportId
    form.destinationAirportId = props.flightToEdit.destinationAirportId
    form.flightTypeId = props.flightToEdit.flightTypeId
    form.flightDate = props.flightToEdit.flightDate
    form.scheduledDepartureTime = props.flightToEdit.scheduledDepartureTime
    form.scheduledArrivalTime = props.flightToEdit.scheduledArrivalTime
    form.flightStatus = props.flightToEdit.flightStatus
  } else {
    form.flightNumber = ''
    form.airlineId = null
    form.aircraftId = null
    form.aircraftTypeId = null
    form.originAirportId = null
    form.destinationAirportId = null
    form.flightTypeId = null
    form.flightDate = new Date().toISOString().split('T')[0] || ''
    form.scheduledDepartureTime = '12:00:00'
    form.scheduledArrivalTime = '14:00:00'
    form.flightStatus = 'SCHEDULED'
  }
  isDirty.value = false
}

watch(
  () => props.visible,
  async (val) => {
    if (val) {
      await referenceStore.fetchAllReferences(true)
      resetForm()
    }
  },
  { immediate: true }
)

watch(
  form,
  () => {
    if (props.visible) {
      isDirty.value = true
    }
  },
  { deep: true }
)

const handleAircraftChange = (aircraftId: number | null) => {
  if (!aircraftId) return
  const aircraft = referenceStore.findAircraftById(aircraftId)
  if (aircraft) {
    if (aircraft.operatorAirlineId) {
      form.airlineId = aircraft.operatorAirlineId
    }
    if (aircraft.aircraftTypeId) {
      form.aircraftTypeId = aircraft.aircraftTypeId
    }
  }
}

const handleBeforeClose = (done: () => void) => {
  if (isDirty.value) {
    ElMessageBox.confirm('Kaydedilmemiş değişiklikler var. Çıkmak istediğinize emin misiniz?', 'Uyarı', {
      confirmButtonText: 'Çık',
      cancelButtonText: 'Vazgeç',
      type: 'warning'
    })
      .then(() => done())
      .catch(() => {})
  } else {
    done()
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  saving.value = true
  try {
    if (isEditMode.value && props.flightToEdit) {
      const updatePayload: FlightUpdateRequest = {
        flightNumber: form.flightNumber.toUpperCase(),
        airlineId: form.airlineId!,
        aircraftId: form.aircraftId,
        aircraftTypeId: form.aircraftTypeId!,
        originAirportId: form.originAirportId!,
        destinationAirportId: form.destinationAirportId!,
        flightTypeId: form.flightTypeId!,
        flightDate: form.flightDate,
        scheduledDepartureTime: form.scheduledDepartureTime,
        scheduledArrivalTime: form.scheduledArrivalTime,
        flightStatus: form.flightStatus
      }
      emit('save-update', { id: props.flightToEdit.flightId, data: updatePayload })
    } else {
      const createPayload: FlightCreateRequest = {
        flightNumber: form.flightNumber.toUpperCase(),
        airlineId: form.airlineId,
        aircraftId: form.aircraftId,
        aircraftTypeId: form.aircraftTypeId,
        originAirportId: form.originAirportId,
        destinationAirportId: form.destinationAirportId,
        flightTypeId: form.flightTypeId,
        flightDate: form.flightDate,
        scheduledDepartureTime: form.scheduledDepartureTime,
        scheduledArrivalTime: form.scheduledArrivalTime
      }
      emit('save-create', createPayload)
    }
    isDirty.value = false
    emit('update:visible', false)
  } finally {
    saving.value = false
  }
}

// Ctrl + S / Cmd + S shortcut
const handleKeyDown = (e: KeyboardEvent) => {
  if (props.visible && (e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 's') {
    e.preventDefault()
    handleSubmit()
  }
}

onMounted(async () => {
  await referenceStore.fetchAllReferences(true)
  window.addEventListener('keydown', handleKeyDown)
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeyDown)
})
</script>

<template>
  <el-dialog
    :model-value="visible"
    :title="isEditMode ? 'Uçuş Düzenle (Ctrl+S)' : 'Yeni Uçuş Ekle (Ctrl+S)'"
    width="840px"
    align-center
    :before-close="handleBeforeClose"
    :append-to-body="true"
    @update:model-value="emit('update:visible', $event)"
    destroy-on-close
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
      <div class="form-grid">
        <!-- SOL SÜTUN: Uçuş Bilgileri -->
        <el-card shadow="never" class="form-section-card">
          <template #header>
            <div class="card-header-title">
              <span>✈️ Uçuş & Ekipman Bilgileri</span>
            </div>
          </template>

          <el-form-item label="Uçuş Numarası (AA9999)" prop="flightNumber">
            <el-input
              v-model="form.flightNumber"
              placeholder="Örn: TK1234"
              maxlength="6"
              style="text-transform: uppercase"
            />
          </el-form-item>

          <el-form-item label="Uçak (Aircraft)" prop="aircraftId">
            <el-select
              v-model="form.aircraftId"
              clearable
              filterable
              placeholder="Uçak seçin"
              style="width: 100%"
              @change="handleAircraftChange"
            >
              <el-option
                v-for="item in referenceStore.aircrafts"
                :key="item.aircraftId"
                :label="item.aircraftRegistrationNumber ? `${item.aircraftRegistrationNumber} (ID #${item.aircraftId})` : `Aircraft #${item.aircraftId}`"
                :value="item.aircraftId"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="Havayolu (Airline)" prop="airlineId">
            <el-select
              v-model="form.airlineId"
              filterable
              placeholder="Havayolu seçin"
              style="width: 100%"
            >
              <el-option
                v-for="item in referenceStore.airlines"
                :key="item.airlineId"
                :label="`${item.airlineName} (${item.airlineIataCode})`"
                :value="item.airlineId"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="Uçak Tipi (Aircraft Type)" prop="aircraftTypeId">
            <el-select
              v-model="form.aircraftTypeId"
              filterable
              placeholder="Uçak tipi seçin"
              style="width: 100%"
            >
              <el-option
                v-for="item in referenceStore.aircraftTypes"
                :key="item.aircraftTypeId"
                :label="`${item.aircraftTypeManufacturer} ${item.aircraftTypeModel} (${item.aircraftTypeIcaoCode})`"
                :value="item.aircraftTypeId"
              />
            </el-select>
          </el-form-item>

          <div class="airports-row">
            <el-form-item label="Kalkış Havalimanı (Origin)" prop="originAirportId" style="flex: 1">
              <el-select
                v-model="form.originAirportId"
                filterable
                placeholder="Kalkış"
                style="width: 100%"
              >
                <el-option
                  v-for="item in referenceStore.airports"
                  :key="item.airportId"
                  :label="`${item.airportName} (${item.airportIataCode})`"
                  :value="item.airportId"
                />
              </el-select>
            </el-form-item>

            <el-form-item label="Varış Havalimanı (Dest)" prop="destinationAirportId" style="flex: 1">
              <el-select
                v-model="form.destinationAirportId"
                filterable
                placeholder="Varış"
                style="width: 100%"
              >
                <el-option
                  v-for="item in referenceStore.airports"
                  :key="item.airportId"
                  :label="`${item.airportName} (${item.airportIataCode})`"
                  :value="item.airportId"
                />
              </el-select>
            </el-form-item>
          </div>
        </el-card>

        <!-- SAĞ SÜTUN: Zaman & Tür Bilgileri -->
        <el-card shadow="never" class="form-section-card">
          <template #header>
            <div class="card-header-title">
              <span>🕒 Zaman & Tür Bilgileri</span>
            </div>
          </template>

          <el-form-item label="Uçuş Tarihi (Flight Date)" prop="flightDate">
            <el-date-picker
              v-model="form.flightDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="Tarih seçin"
              style="width: 100%"
              :teleported="true"
            />
          </el-form-item>

          <el-form-item label="Kalkış Saati (STD)" prop="scheduledDepartureTime">
            <el-time-picker
              v-model="form.scheduledDepartureTime"
              value-format="HH:mm:ss"
              placeholder="HH:mm:ss"
              style="width: 100%"
              :teleported="true"
            />
          </el-form-item>

          <el-form-item label="Varış Saati (STA)" prop="scheduledArrivalTime">
            <el-time-picker
              v-model="form.scheduledArrivalTime"
              value-format="HH:mm:ss"
              placeholder="HH:mm:ss"
              style="width: 100%"
              :teleported="true"
            />
          </el-form-item>

          <el-form-item label="Uçuş Tipi (Flight Type)" prop="flightTypeId">
            <el-select
              v-model="form.flightTypeId"
              placeholder="Uçuş tipi seçin"
              style="width: 100%"
            >
              <el-option
                v-for="item in referenceStore.flightTypes"
                :key="item.flightTypeId"
                :label="item.flightTypeName || item.flightTypeCode"
                :value="item.flightTypeId"
              />
            </el-select>
          </el-form-item>

          <el-form-item v-if="isEditMode" label="Uçuş Durumu (Flight Status)" prop="flightStatus">
            <el-select v-model="form.flightStatus" style="width: 100%">
              <el-option label="Planlandı (SCHEDULED)" value="SCHEDULED" />
              <el-option label="Rötarlı (DELAYED)" value="DELAYED" />
              <el-option label="Kalktı (DEPARTED)" value="DEPARTED" />
              <el-option label="İndi (ARRIVED)" value="ARRIVED" />
              <el-option label="İptal Edildi (CANCELLED)" value="CANCELLED" />
            </el-select>
          </el-form-item>
        </el-card>
      </div>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <span class="shortcut-tip">💡 Kısayol: <b>Ctrl + S</b> ile hızlı kaydet</span>
        <div>
          <el-button @click="emit('update:visible', false)">Vazgeç</el-button>
          <el-button type="primary" :loading="saving" @click="handleSubmit">
            {{ isEditMode ? 'Güncelle' : 'Kaydet' }}
          </el-button>
        </div>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped>
.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.form-section-card {
  border: 1px solid #e2e8f0;
  background-color: #fafafa;
}

.card-header-title {
  font-weight: 600;
  font-size: 14px;
  color: #0f172a;
}

.airports-row {
  display: flex;
  gap: 12px;
}

.dialog-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.shortcut-tip {
  font-size: 12px;
  color: #64748b;
}
</style>
