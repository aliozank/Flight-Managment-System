<script setup lang="ts">
import { ref, reactive, watch, onMounted, onUnmounted, computed } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useReferenceStore } from '@/stores/referenceStore'
import type { FlightResponse, FlightCreateRequest, FlightUpdateRequest } from '@/types/flight'

const props = withDefaults(defineProps<{
  visible: boolean
  flightToEdit?: FlightResponse | null
  saving?: boolean
}>(), {
  saving: false
})

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'save-create', payload: FlightCreateRequest): void
  (e: 'save-update', payload: { id: number; data: FlightUpdateRequest }): void
}>()

const referenceStore = useReferenceStore()
const formRef = ref<FormInstance>()
const isDirty = ref(false)
const TURKEY_TIMEZONE = 'Europe/Istanbul'

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
  scheduledArrivalDate: '',
  scheduledArrivalTime: ''
})

const hasSelectedAircraft = computed(() => form.aircraftId !== null)
const selectedAirline = computed(() =>
  referenceStore.airlines.find((airline) => airline.airlineId === form.airlineId)
)
const originTimezone = computed(() =>
  referenceStore.airports.find((airport) => airport.airportId === form.originAirportId)?.airportTimezone
)
const destinationTimezone = computed(() =>
  referenceStore.airports.find((airport) => airport.airportId === form.destinationAirportId)?.airportTimezone
)
const originAirportCode = computed(() =>
  referenceStore.airports.find((airport) => airport.airportId === form.originAirportId)?.airportIataCode
)
const destinationAirportCode = computed(() =>
  referenceStore.airports.find((airport) => airport.airportId === form.destinationAirportId)?.airportIataCode
)
const departureTimeLabel = computed(() =>
  originAirportCode.value ? `Kalkış Saati — ${originAirportCode.value} yerel` : 'Kalkış Saati'
)
const arrivalTimeLabel = computed(() =>
  destinationAirportCode.value ? `Varış Saati — ${destinationAirportCode.value} yerel` : 'Varış Saati'
)

interface DateTimeParts {
  year: number
  month: number
  day: number
  hour: number
  minute: number
  second: number
}

const readDateTimeParts = (date: Date, timeZone: string): DateTimeParts | null => {
  try {
    const formatter = new Intl.DateTimeFormat('en-CA', {
      timeZone,
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      hourCycle: 'h23'
    })
    const values = Object.fromEntries(
      formatter.formatToParts(date)
        .filter((part) => part.type !== 'literal')
        .map((part) => [part.type, Number(part.value)])
    ) as Record<string, number | undefined>

    const year = values.year
    const month = values.month
    const day = values.day
    const hour = values.hour
    const minute = values.minute
    const second = values.second
    if ([year, month, day, hour, minute, second].some((value) => value === undefined)) return null

    return {
      year: year!,
      month: month!,
      day: day!,
      hour: hour!,
      minute: minute!,
      second: second!
    }
  } catch {
    return null
  }
}

const partsMatch = (left: DateTimeParts | null, right: DateTimeParts): boolean => {
  return left !== null
    && left.year === right.year
    && left.month === right.month
    && left.day === right.day
    && left.hour === right.hour
    && left.minute === right.minute
    && left.second === right.second
}

const localAirportTimeToInstant = (
  dateValue: string,
  timeValue: string,
  timeZone: string
): Date | null => {
  const dateMatch = /^(\d{4})-(\d{2})-(\d{2})$/.exec(dateValue)
  const timeMatch = /^(\d{2}):(\d{2})(?::(\d{2}))?$/.exec(timeValue)
  if (!dateMatch || !timeMatch) return null

  const desired: DateTimeParts = {
    year: Number(dateMatch[1]),
    month: Number(dateMatch[2]),
    day: Number(dateMatch[3]),
    hour: Number(timeMatch[1]),
    minute: Number(timeMatch[2]),
    second: Number(timeMatch[3] ?? 0)
  }
  const desiredAsUtc = Date.UTC(
    desired.year,
    desired.month - 1,
    desired.day,
    desired.hour,
    desired.minute,
    desired.second
  )

  let candidate = desiredAsUtc
  for (let attempt = 0; attempt < 4; attempt += 1) {
    const actual = readDateTimeParts(new Date(candidate), timeZone)
    if (!actual) return null
    const actualAsUtc = Date.UTC(
      actual.year,
      actual.month - 1,
      actual.day,
      actual.hour,
      actual.minute,
      actual.second
    )
    candidate -= actualAsUtc - desiredAsUtc
  }

  if (!partsMatch(readDateTimeParts(new Date(candidate), timeZone), desired)) return null

  // Java ZonedDateTime chooses the earlier offset during a daylight-saving overlap.
  for (let minutes = 15; minutes <= 180; minutes += 15) {
    const earlierCandidate = candidate - minutes * 60_000
    if (partsMatch(readDateTimeParts(new Date(earlierCandidate), timeZone), desired)) {
      candidate = earlierCandidate
    }
  }

  return new Date(candidate)
}

const departureInstant = computed(() => {
  if (!form.flightDate || !form.scheduledDepartureTime || !originTimezone.value) return null
  return localAirportTimeToInstant(form.flightDate, form.scheduledDepartureTime, originTimezone.value)
})

const arrivalInstant = computed(() => {
  if (!form.scheduledArrivalDate || !form.scheduledArrivalTime || !destinationTimezone.value) return null
  return localAirportTimeToInstant(
    form.scheduledArrivalDate,
    form.scheduledArrivalTime,
    destinationTimezone.value
  )
})

const scheduleFieldsComplete = computed(() => Boolean(
  form.flightDate
  && form.scheduledDepartureTime
  && form.scheduledArrivalDate
  && form.scheduledArrivalTime
  && form.originAirportId
  && form.destinationAirportId
))

const getScheduleValidationError = (): string | null => {
  if (!scheduleFieldsComplete.value) return null
  if (!originTimezone.value || !destinationTimezone.value) {
    return 'Seçilen havalimanlarından birinin saat dilimi tanımlı değil.'
  }
  if (!departureInstant.value || !arrivalInstant.value) {
    return 'Seçilen tarih/saat, havalimanının saat diliminde geçerli değil.'
  }
  if (departureInstant.value.getTime() <= Date.now()) {
    return 'Kalkış zamanı geçmişte olamaz. Havalimanının yerel saatini kontrol edin.'
  }
  if (arrivalInstant.value.getTime() <= departureInstant.value.getTime()) {
    return 'Varış anı kalkıştan sonra olmalıdır. İki havalimanının saat farkını kontrol edin.'
  }
  return null
}

const scheduleValidationError = computed(getScheduleValidationError)

const turkeyTimeFormatter = new Intl.DateTimeFormat('tr-TR', {
  timeZone: TURKEY_TIMEZONE,
  hour: '2-digit',
  minute: '2-digit'
})

const formatTurkeyTime = (date: Date | null): string => {
  return date ? turkeyTimeFormatter.format(date) : '-'
}

const isForeignTimezone = (timeZone?: string): boolean => Boolean(
  timeZone && timeZone !== TURKEY_TIMEZONE
)
const availableDestinationAirports = computed(() => {
  if (form.originAirportId === null) return []

  const destinationIds = new Set(
    referenceStore.routes
      .filter((route) =>
        route.originAirportId === form.originAirportId &&
        (route.routeStatus === undefined || route.routeStatus === 'ACTIVE')
      )
      .map((route) => route.destinationAirportId)
  )

  return referenceStore.airports.filter((airport) => destinationIds.has(airport.airportId))
})

watch(
  () => form.originAirportId,
  () => {
    if (
      form.destinationAirportId !== null &&
      !availableDestinationAirports.value.some(
        (airport) => airport.airportId === form.destinationAirportId
      )
    ) {
      form.destinationAirportId = null
    }
  }
)

// Regex: 2 uppercase letters/digits + 4 digits (e.g. TK1234, 8A9999)
const flightNumberRegex = /^[A-Z0-9]{2}\d{4}$/

const validateFlightNumber = (_rule: any, value: string, callback: any) => {
  if (!value) {
    callback(new Error('Uçuş numarası zorunludur'))
  } else if (!flightNumberRegex.test(value)) {
    callback(new Error('Uçuş numarası 2 harf/rakam ve 4 rakamdan oluşmalıdır (Örn: TK1234)'))
  } else if (
    selectedAirline.value?.airlineIataCode &&
    !value.toUpperCase().startsWith(selectedAirline.value.airlineIataCode.toUpperCase())
  ) {
    callback(new Error(`Uçuş numarası ${selectedAirline.value.airlineIataCode.toUpperCase()} ile başlamalıdır`))
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

const validateSchedule = (_rule: any, _value: any, callback: any) => {
  const validationError = getScheduleValidationError()
  if (validationError) {
    callback(new Error(validationError))
  } else {
    callback()
  }
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
  scheduledArrivalDate: [{ required: true, message: 'Varış tarihi zorunludur', trigger: 'change' }],
  scheduledArrivalTime: [
    { required: true, message: 'Varış saati zorunludur', trigger: 'change' },
    { validator: validateSchedule, trigger: 'change' }
  ]
})

const getTodayInTurkey = (): string => {
  const parts = readDateTimeParts(new Date(), TURKEY_TIMEZONE)
  if (!parts) return ''
  return `${parts.year}-${String(parts.month).padStart(2, '0')}-${String(parts.day).padStart(2, '0')}`
}

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
    form.scheduledArrivalDate = props.flightToEdit.scheduledArrivalDate
    form.scheduledArrivalTime = props.flightToEdit.scheduledArrivalTime
  } else {
    form.flightNumber = ''
    form.airlineId = null
    form.aircraftId = null
    form.aircraftTypeId = null
    form.originAirportId = null
    form.destinationAirportId = null
    form.flightTypeId = null
    form.flightDate = getTodayInTurkey()
    form.scheduledDepartureTime = ''
    form.scheduledArrivalDate = form.flightDate
    form.scheduledArrivalTime = ''
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
  () => form.flightDate,
  (newDate, previousDate) => {
    if (newDate && (!form.scheduledArrivalDate || form.scheduledArrivalDate === previousDate)) {
      form.scheduledArrivalDate = newDate
    }
  }
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

const syncFlightNumberPrefix = () => {
  const iataCode = selectedAirline.value?.airlineIataCode?.toUpperCase()
  if (!iataCode) return

  const numericSuffix = form.flightNumber.replace(/\D/g, '').slice(-4)
  form.flightNumber = `${iataCode}${numericSuffix}`
  formRef.value?.clearValidate('flightNumber')
}

const handleAircraftChange = (aircraftId: number | null) => {
  if (!aircraftId) return
  const aircraft = referenceStore.findAircraftById(aircraftId)
  if (aircraft) {
    if (aircraft.operatorAirlineId) {
      form.airlineId = aircraft.operatorAirlineId
      syncFlightNumberPrefix()
    }
    if (aircraft.aircraftTypeId) {
      form.aircraftTypeId = aircraft.aircraftTypeId
    }
  }
}

const handleBeforeClose = (done: () => void) => {
  if (props.saving) return

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
  if (!formRef.value || props.saving) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  const validationError = getScheduleValidationError()
  if (validationError) {
    ElMessage.error(validationError)
    return
  }

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
      scheduledArrivalDate: form.scheduledArrivalDate,
      scheduledArrivalTime: form.scheduledArrivalTime
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
      scheduledArrivalDate: form.scheduledArrivalDate,
      scheduledArrivalTime: form.scheduledArrivalTime
    }
    emit('save-create', createPayload)
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
    :close-on-click-modal="!saving"
    :close-on-press-escape="!saving"
    :show-close="!saving"
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
              @input="form.flightNumber = form.flightNumber.toUpperCase()"
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
                :disabled="item.operatorAirlineId === null || (item.aircraftStatus !== undefined && item.aircraftStatus !== 'ACTIVE')"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="Havayolu (Airline)" prop="airlineId">
            <el-select
              v-model="form.airlineId"
              filterable
              placeholder="Havayolu seçin"
              style="width: 100%"
              :disabled="hasSelectedAircraft"
              @change="syncFlightNumberPrefix"
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
              :disabled="hasSelectedAircraft"
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
                :placeholder="form.originAirportId === null ? 'Önce kalkış seçin' : 'Varış'"
                :disabled="form.originAirportId === null"
                style="width: 100%"
              >
                <el-option
                  v-for="item in availableDestinationAirports"
                  :key="item.airportId"
                  :label="`${item.airportName} (${item.airportIataCode})`"
                  :value="item.airportId"
                />
              </el-select>
              <div
                v-if="form.originAirportId !== null && availableDestinationAirports.length === 0"
                class="route-warning"
              >
                Bu kalkış havalimanından aktif rota bulunamadı.
              </div>
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

          <el-alert
            class="timezone-info"
            title="Saatleri ilgili havalimanının yerel saatine göre girin."
            type="info"
            :closable="false"
            show-icon
          />

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

          <el-form-item :label="departureTimeLabel" prop="scheduledDepartureTime">
            <div class="time-input-row">
              <el-time-picker
                v-model="form.scheduledDepartureTime"
                value-format="HH:mm:ss"
                placeholder="HH:mm:ss"
                class="time-picker"
                :teleported="true"
              />
              <span
                v-if="departureInstant && isForeignTimezone(originTimezone)"
                class="turkey-time-chip"
              >
                TR {{ formatTurkeyTime(departureInstant) }}
              </span>
            </div>
          </el-form-item>

          <el-form-item label="Varış Tarihi" prop="scheduledArrivalDate">
            <el-date-picker
              v-model="form.scheduledArrivalDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="Varış tarihini seçin"
              style="width: 100%"
              :teleported="true"
            />
          </el-form-item>

          <el-form-item :label="arrivalTimeLabel" prop="scheduledArrivalTime">
            <div class="time-input-row">
              <el-time-picker
                v-model="form.scheduledArrivalTime"
                value-format="HH:mm:ss"
                placeholder="HH:mm:ss"
                class="time-picker"
                :teleported="true"
              />
              <span
                v-if="arrivalInstant && isForeignTimezone(destinationTimezone)"
                class="turkey-time-chip"
              >
                TR {{ formatTurkeyTime(arrivalInstant) }}
              </span>
            </div>
          </el-form-item>

          <div v-if="scheduleValidationError" class="schedule-error">
            {{ scheduleValidationError }}
          </div>

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

        </el-card>
      </div>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <span class="shortcut-tip">💡 Kısayol: <b>Ctrl + S</b> ile hızlı kaydet</span>
        <div>
          <el-button :disabled="saving" @click="emit('update:visible', false)">Vazgeç</el-button>
          <el-button type="primary" :loading="saving" :disabled="saving" @click="handleSubmit">
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

.timezone-info {
  margin-bottom: 16px;
}

.route-warning {
  margin-top: 6px;
  color: #e6a23c;
  font-size: 12px;
  line-height: 1.4;
}

.time-input-row {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}

.time-picker {
  flex: 1;
}

.turkey-time-chip {
  flex: none;
  padding: 5px 8px;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  background: #f8fafc;
  color: #475569;
  font-size: 11px;
  font-weight: 700;
  white-space: nowrap;
}

.schedule-error {
  margin: -4px 0 14px;
  padding: 7px 9px;
  border-radius: 6px;
  background: #fef2f2;
  color: #b91c1c;
  font-size: 12px;
}
</style>
