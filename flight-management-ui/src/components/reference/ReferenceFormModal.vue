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
    } else if (props.tab === 'airports') {
      form.airportName = ''
      form.airportCity = ''
      form.airportCountry = ''
      form.airportIataCode = ''
      form.airportIcaoCode = ''
      form.airportTimezone = 'UTC'
    } else if (props.tab === 'aircraftTypes') {
      form.aircraftTypeManufacturer = ''
      form.aircraftTypeModel = ''
      form.aircraftTypeIcaoCode = ''
    } else if (props.tab === 'aircrafts') {
      form.aircraftRegistrationNumber = ''
      form.operatorAirlineId = null
      form.aircraftTypeId = null
      form.aircraftCapacity = 180
      form.aircraftManufactureYear = 2020
    } else if (props.tab === 'routes') {
      form.originAirportId = null
      form.destinationAirportId = null
      form.distanceKm = 500
      form.estimatedDurationMinutes = 60
    } else if (props.tab === 'flightTypes') {
      form.flightTypeName = ''
      form.flightTypeCode = ''
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
      </template>

      <!-- Flight Types -->
      <template v-else-if="tab === 'flightTypes'">
        <el-form-item label="Uçuş Tipi Adı" required>
          <el-input v-model="form.flightTypeName" placeholder="Örn: Passenger" />
        </el-form-item>
        <el-form-item label="Kod" required>
          <el-input v-model="form.flightTypeCode" placeholder="Örn: PAX" />
        </el-form-item>
      </template>
    </el-form>

    <template #footer>
      <el-button @click="emit('update:visible', false)">Vazgeç</el-button>
      <el-button type="primary" :loading="saving" @click="handleSave">Kaydet</el-button>
    </template>
  </el-dialog>
</template>
