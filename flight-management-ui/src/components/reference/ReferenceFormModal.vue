<script setup lang="ts">
import { ref, reactive, watch, computed } from 'vue'
import { ElMessage, type FormInstance } from 'element-plus'
import referenceApi from '@/services/referenceApi'
import { useReferenceStore } from '@/stores/referenceStore'

export type ReferenceTab = 'airlines' | 'airports' | 'aircraftTypes' | 'aircrafts' | 'routes' | 'flightTypes'

const props = defineProps<{
  visible: boolean
  tab: ReferenceTab
  itemToEdit?: any
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'saved'): void
}>()

const referenceStore = useReferenceStore()
const formRef = ref<FormInstance>()
const saving = ref(false)

const timezoneOptions = [
  'UTC',
  'Europe/Istanbul',
  'Europe/London',
  'Europe/Berlin',
  'Europe/Paris',
  'Asia/Dubai',
  'Asia/Singapore',
  'Asia/Tokyo',
  'America/New_York',
  'America/Chicago',
  'America/Los_Angeles',
  'Australia/Sydney'
]

const form = reactive<Record<string, any>>({})

const isEditMode = computed(() => !!props.itemToEdit)

const initForm = () => {
  Object.keys(form).forEach((k) => delete form[k])
  if (props.itemToEdit) {
    Object.assign(form, props.itemToEdit)
  } else {
    if (props.tab === 'airlines') {
      form.airlineName = ''
      form.airlineIataCode = ''
      form.airlineIcaoCode = ''
      form.airlineCountry = ''
      form.airlineStatus = 'ACTIVE'
    } else if (props.tab === 'airports') {
      form.airportName = ''
      form.airportCity = ''
      form.airportCountry = ''
      form.airportIataCode = ''
      form.airportIcaoCode = ''
      form.airportTimezone = 'Europe/Istanbul'
      form.airportStatus = 'OPERATIONAL'
    } else if (props.tab === 'aircraftTypes') {
      form.aircraftTypeManufacturer = ''
      form.aircraftTypeModel = ''
      form.aircraftTypeIcaoCode = ''
      form.aircraftTypeCategory = 'NARROW_BODY'
      form.aircraftTypeStatus = 'ACTIVE'
    } else if (props.tab === 'aircrafts') {
      form.aircraftRegistrationNumber = ''
      form.operatorAirlineId = null
      form.aircraftTypeId = null
      form.aircraftCapacity = 180
      form.aircraftManufactureYear = 2020
      form.aircraftStatus = 'ACTIVE'
    } else if (props.tab === 'routes') {
      form.originAirportId = null
      form.destinationAirportId = null
      form.routeStatus = 'ACTIVE'
    } else if (props.tab === 'flightTypes') {
      form.flightTypeName = ''
      form.flightTypeCode = ''
      form.flightTypeStatus = 'ACTIVE'
    }
  }
}

watch(
  () => props.visible,
  (val) => {
    if (val) initForm()
  }
)

const handleSave = async () => {
  saving.value = true
  try {
    Object.keys(form).forEach((key) => {
      if (typeof form[key] === 'string') {
        form[key] = form[key].trim()
      }
    })

    const uppercaseFields = [
      'airlineIataCode',
      'airlineIcaoCode',
      'airportIataCode',
      'airportIcaoCode',
      'aircraftTypeIcaoCode',
      'aircraftRegistrationNumber',
      'flightTypeCode'
    ]

    uppercaseFields.forEach((key) => {
      if (typeof form[key] === 'string') {
        form[key] = form[key].toUpperCase()
      }
    })

    const endpointMap: Record<ReferenceTab, string> = {
      airlines: '/api/airlines',
      airports: '/api/airports',
      aircraftTypes: '/api/aircraft-types',
      aircrafts: '/api/aircrafts',
      routes: '/api/routes',
      flightTypes: '/api/flight-types'
    }

    const idKeyMap: Record<ReferenceTab, string> = {
      airlines: 'airlineId',
      airports: 'airportId',
      aircraftTypes: 'aircraftTypeId',
      aircrafts: 'aircraftId',
      routes: 'routeId',
      flightTypes: 'flightTypeId'
    }

    const endpoint = endpointMap[props.tab]
    const idKey = idKeyMap[props.tab]

    if (isEditMode.value) {
      const id = props.itemToEdit[idKey]
      await referenceApi.put(`${endpoint}/${id}`, form)
      ElMessage.success('Kayıt güncellendi')
    } else {
      await referenceApi.post(endpoint, form)
      ElMessage.success('Yeni kayıt eklendi')
    }

    await referenceStore.fetchAllReferences(true)
    emit('saved')
    emit('update:visible', false)
  } catch {
    // Interceptor notification
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <el-dialog
    :model-value="visible"
    :title="isEditMode ? 'Kayıt Düzenle' : 'Yeni Kayıt Ekle'"
    width="560px"
    align-center
    :append-to-body="true"
    @update:model-value="emit('update:visible', $event)"
    destroy-on-close
  >
    <el-form ref="formRef" :model="form" label-position="top">
      <!-- Airlines -->
      <template v-if="tab === 'airlines'">
        <el-form-item label="Havayolu Adı" required>
          <el-input v-model="form.airlineName" placeholder="Örn: Turkish Airlines" />
        </el-form-item>
        <el-form-item label="IATA Kodu" required>
          <el-input v-model="form.airlineIataCode" placeholder="Örn: TK" maxlength="3" />
        </el-form-item>
        <el-form-item label="ICAO Kodu" required>
          <el-input v-model="form.airlineIcaoCode" placeholder="Örn: THY" maxlength="3" />
        </el-form-item>
        <el-form-item label="Ülke" required>
          <el-input v-model="form.airlineCountry" placeholder="Örn: Turkey" />
        </el-form-item>
        <el-form-item label="Durum" required>
          <el-select v-model="form.airlineStatus" style="width: 100%">
            <el-option label="Aktif" value="ACTIVE" />
            <el-option label="Pasif" value="INACTIVE" />
            <el-option label="Askıya Alındı" value="SUSPENDED" />
            <el-option label="Faaliyetini Durdurdu" value="CEASED_OPERATIONS" />
          </el-select>
        </el-form-item>
      </template>

      <!-- Airports -->
      <template v-else-if="tab === 'airports'">
        <el-form-item label="Havalimanı Adı" required>
          <el-input v-model="form.airportName" placeholder="Örn: Istanbul Airport" />
        </el-form-item>
        <el-form-item label="Şehir" required>
          <el-input v-model="form.airportCity" placeholder="Örn: Istanbul" />
        </el-form-item>
        <el-form-item label="Ülke" required>
          <el-input v-model="form.airportCountry" placeholder="Örn: Turkey" />
        </el-form-item>
        <div style="display: flex; gap: 12px">
          <el-form-item label="IATA Kodu" required style="flex: 1">
            <el-input v-model="form.airportIataCode" placeholder="Örn: IST" maxlength="3" />
          </el-form-item>
          <el-form-item label="ICAO Kodu" required style="flex: 1">
            <el-input v-model="form.airportIcaoCode" placeholder="Örn: LTFM" maxlength="4" />
          </el-form-item>
        </div>
        <el-form-item label="Saat Dilimi" required>
          <el-select
            v-model="form.airportTimezone"
            filterable
            placeholder="Saat dilimi seçin"
            style="width: 100%"
          >
            <el-option
              v-for="timezone in timezoneOptions"
              :key="timezone"
              :label="timezone"
              :value="timezone"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="Durum" required>
          <el-select v-model="form.airportStatus" style="width: 100%">
            <el-option label="Operasyonel" value="OPERATIONAL" />
            <el-option label="Geçici Kapalı" value="TEMPORARILY_CLOSED" />
            <el-option label="Kalıcı Kapalı" value="PERMANENTLY_CLOSED" />
          </el-select>
        </el-form-item>
      </template>

      <!-- Aircraft Types -->
      <template v-else-if="tab === 'aircraftTypes'">
        <el-form-item label="Üretici (Manufacturer)" required>
          <el-input v-model="form.aircraftTypeManufacturer" placeholder="Örn: Airbus" />
        </el-form-item>
        <el-form-item label="Model" required>
          <el-input v-model="form.aircraftTypeModel" placeholder="Örn: A320neo" />
        </el-form-item>
        <el-form-item label="ICAO Kodu" required>
          <el-input v-model="form.aircraftTypeIcaoCode" placeholder="Örn: A20N" />
        </el-form-item>
        <el-form-item label="Kategori" required>
          <el-select v-model="form.aircraftTypeCategory" style="width: 100%">
            <el-option label="Dar Gövde" value="NARROW_BODY" />
            <el-option label="Geniş Gövde" value="WIDE_BODY" />
            <el-option label="Bölgesel" value="REGIONAL" />
            <el-option label="Turboprop" value="TURBOPROP" />
          </el-select>
        </el-form-item>
        <el-form-item label="Durum" required>
          <el-select v-model="form.aircraftTypeStatus" style="width: 100%">
            <el-option label="Aktif" value="ACTIVE" />
            <el-option label="Pasif" value="INACTIVE" />
          </el-select>
        </el-form-item>
      </template>

      <!-- Aircrafts -->
      <template v-else-if="tab === 'aircrafts'">
        <el-form-item label="Tescil Numarası (Registration)" required>
          <el-input v-model="form.aircraftRegistrationNumber" placeholder="Örn: TC-LPA" />
        </el-form-item>
        <el-form-item label="İşleten Havayolu" required>
          <el-select v-model="form.operatorAirlineId" filterable style="width: 100%">
            <el-option
              v-for="item in referenceStore.airlines"
              :key="item.airlineId"
              :label="referenceStore.getAirlineLabel(item.airlineId)"
              :value="item.airlineId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="Uçak Tipi" required>
          <el-select v-model="form.aircraftTypeId" filterable style="width: 100%">
            <el-option
              v-for="item in referenceStore.aircraftTypes"
              :key="item.aircraftTypeId"
              :label="referenceStore.getAircraftTypeLabel(item.aircraftTypeId)"
              :value="item.aircraftTypeId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="Kapasite" required>
          <el-input-number v-model="form.aircraftCapacity" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="Üretim Yılı" required>
          <el-input-number v-model="form.aircraftManufactureYear" :min="1923" :max="new Date().getFullYear()" style="width: 100%" />
        </el-form-item>
        <el-form-item label="Durum" required>
          <el-select v-model="form.aircraftStatus" style="width: 100%">
            <el-option label="Aktif" value="ACTIVE" />
            <el-option label="Bakımda" value="MAINTENANCE" />
            <el-option label="Yerde" value="GROUNDED" />
            <el-option label="Emekli" value="RETIRED" />
          </el-select>
        </el-form-item>
      </template>

      <!-- Routes -->
      <template v-else-if="tab === 'routes'">
        <el-form-item label="Kalkış Havalimanı" required>
          <el-select v-model="form.originAirportId" filterable style="width: 100%">
            <el-option
              v-for="item in referenceStore.airports"
              :key="item.airportId"
              :label="referenceStore.getAirportLabel(item.airportId)"
              :value="item.airportId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="Varış Havalimanı" required>
          <el-select v-model="form.destinationAirportId" filterable style="width: 100%">
            <el-option
              v-for="item in referenceStore.airports"
              :key="item.airportId"
              :label="referenceStore.getAirportLabel(item.airportId)"
              :value="item.airportId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="Durum" required>
          <el-select v-model="form.routeStatus" style="width: 100%">
            <el-option label="Aktif" value="ACTIVE" />
            <el-option label="Pasif" value="INACTIVE" />
          </el-select>
        </el-form-item>
      </template>

      <!-- Flight Types -->
      <template v-else-if="tab === 'flightTypes'">
        <el-form-item label="Uçuş Tipi Adı" required>
          <el-input v-model="form.flightTypeName" placeholder="Örn: Passenger" />
        </el-form-item>
        <el-form-item label="Kod" required>
          <el-input v-model="form.flightTypeCode" placeholder="Örn: PAX" />
        </el-form-item>
        <el-form-item label="Durum" required>
          <el-select v-model="form.flightTypeStatus" style="width: 100%">
            <el-option label="Aktif" value="ACTIVE" />
            <el-option label="Pasif" value="INACTIVE" />
          </el-select>
        </el-form-item>
      </template>
    </el-form>

    <template #footer>
      <el-button @click="emit('update:visible', false)">Vazgeç</el-button>
      <el-button type="primary" :loading="saving" @click="handleSave">Kaydet</el-button>
    </template>
  </el-dialog>
</template>
