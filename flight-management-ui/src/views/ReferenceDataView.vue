<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/authStore'
import { useReferenceStore } from '@/stores/referenceStore'
import referenceApi from '@/services/referenceApi'
import ReferenceFormModal, { type ReferenceTab } from '@/components/reference/ReferenceFormModal.vue'

const authStore = useAuthStore()
const referenceStore = useReferenceStore()

const activeTab = ref<ReferenceTab>('airlines')
const modalVisible = ref(false)
const itemToEdit = ref<any>(null)

onMounted(() => {
  referenceStore.fetchAllReferences()
})

const handleOpenCreate = () => {
  itemToEdit.value = null
  modalVisible.value = true
}

const handleOpenEdit = (row: any) => {
  itemToEdit.value = row
  modalVisible.value = true
}

const handleDelete = (row: any) => {
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

  const endpoint = endpointMap[activeTab.value]
  const idKey = idKeyMap[activeTab.value]
  const id = row[idKey]

  ElMessageBox.confirm('Bu kaydı silmek/deaktif etmek istediğinizden emin misiniz?', 'Uyarı', {
    confirmButtonText: 'Evet, Sil',
    cancelButtonText: 'Vazgeç',
    type: 'warning'
  })
    .then(async () => {
      try {
        await referenceApi.delete(`${endpoint}/${id}`)
        ElMessage.success('Kayıt silindi')
        await referenceStore.fetchAllReferences(true)
      } catch {
        // Interceptor notification
      }
    })
    .catch(() => {})
}
</script>

<template>
  <div class="reference-view">
    <div class="header-bar">
      <div>
        <h2>Referans Veriler Katalogu</h2>
        <p>Sistem genelinde kullanılan standart havacılık referans verileri</p>
      </div>

      <el-button
        v-if="authStore.canManageReferenceData"
        type="primary"
        @click="handleOpenCreate"
      >
        ➕ Yeni Kayıt Ekle
      </el-button>
    </div>

    <el-card shadow="never">
      <el-tabs v-model="activeTab" type="card">
        <!-- Airlines Tab -->
        <el-tab-pane label="Havayolları (Airlines)" name="airlines">
          <el-table :data="referenceStore.airlines" stripe style="width: 100%">
            <el-table-column prop="airlineId" label="ID" width="80" />
            <el-table-column prop="airlineName" label="Havayolu Adı" min-width="200" />
            <el-table-column prop="airlineIataCode" label="IATA Kodu" width="120" />
            <el-table-column
              v-if="authStore.canManageReferenceData"
              label="İşlem"
              width="160"
              align="center"
            >
              <template #default="{ row }">
                <el-button size="small" type="primary" class="action-btn-edit" @click="handleOpenEdit(row)">Düzenle</el-button>
                <el-button size="small" type="danger" plain @click="handleDelete(row)">Sil</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- Airports Tab -->
        <el-tab-pane label="Havalimanları (Airports)" name="airports">
          <el-table :data="referenceStore.airports" stripe style="width: 100%">
            <el-table-column prop="airportId" label="ID" width="80" />
            <el-table-column prop="airportName" label="Havalimanı Adı" min-width="220" />
            <el-table-column prop="airportCity" label="Şehir" min-width="140" />
            <el-table-column prop="airportCountry" label="Ülke" min-width="140" />
            <el-table-column prop="airportIataCode" label="IATA" width="100" />
            <el-table-column prop="airportIcaoCode" label="ICAO" width="100" />
            <el-table-column
              v-if="authStore.canManageReferenceData"
              label="İşlem"
              width="160"
              align="center"
            >
              <template #default="{ row }">
                <el-button size="small" type="primary" class="action-btn-edit" @click="handleOpenEdit(row)">Düzenle</el-button>
                <el-button size="small" type="danger" plain @click="handleDelete(row)">Sil</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- Aircraft Types Tab -->
        <el-tab-pane label="Uçak Tipleri (Aircraft Types)" name="aircraftTypes">
          <el-table :data="referenceStore.aircraftTypes" stripe style="width: 100%">
            <el-table-column prop="aircraftTypeId" label="ID" width="80" />
            <el-table-column prop="aircraftTypeManufacturer" label="Üretici" min-width="160" />
            <el-table-column prop="aircraftTypeModel" label="Model" min-width="160" />
            <el-table-column prop="aircraftTypeIcaoCode" label="ICAO Kod" width="120" />
            <el-table-column
              v-if="authStore.canManageReferenceData"
              label="İşlem"
              width="160"
              align="center"
            >
              <template #default="{ row }">
                <el-button size="small" type="primary" class="action-btn-edit" @click="handleOpenEdit(row)">Düzenle</el-button>
                <el-button size="small" type="danger" plain @click="handleDelete(row)">Sil</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- Aircrafts Tab -->
        <el-tab-pane label="Uçaklar (Aircrafts)" name="aircrafts">
          <el-table :data="referenceStore.aircrafts" stripe style="width: 100%">
            <el-table-column prop="aircraftId" label="ID" width="80" />
            <el-table-column prop="aircraftRegistrationNumber" label="Tescil Kodu" min-width="140" />
            <el-table-column label="İşleten Havayolu" min-width="180">
              <template #default="{ row }">
                {{ referenceStore.getAirlineLabel(row.operatorAirlineId) }}
              </template>
            </el-table-column>
            <el-table-column label="Uçak Tipi" min-width="180">
              <template #default="{ row }">
                {{ referenceStore.getAircraftTypeLabel(row.aircraftTypeId) }}
              </template>
            </el-table-column>
            <el-table-column
              v-if="authStore.canManageReferenceData"
              label="İşlem"
              width="160"
              align="center"
            >
              <template #default="{ row }">
                <el-button size="small" type="primary" class="action-btn-edit" @click="handleOpenEdit(row)">Düzenle</el-button>
                <el-button size="small" type="danger" plain @click="handleDelete(row)">Sil</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- Routes Tab -->
        <el-tab-pane label="Rotalar (Routes)" name="routes">
          <el-table :data="referenceStore.routes" stripe style="width: 100%">
            <el-table-column prop="routeId" label="ID" width="80" />
            <el-table-column label="Kalkış Havalimanı" min-width="200">
              <template #default="{ row }">
                {{ referenceStore.getAirportLabel(row.originAirportId) }}
              </template>
            </el-table-column>
            <el-table-column label="Varış Havalimanı" min-width="200">
              <template #default="{ row }">
                {{ referenceStore.getAirportLabel(row.destinationAirportId) }}
              </template>
            </el-table-column>
            <el-table-column
              v-if="authStore.canManageReferenceData"
              label="İşlem"
              width="160"
              align="center"
            >
              <template #default="{ row }">
                <el-button size="small" type="primary" plain @click="handleOpenEdit(row)">Düzenle</el-button>
                <el-button size="small" type="danger" plain @click="handleDelete(row)">Sil</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- Flight Types Tab -->
        <el-tab-pane label="Uçuş Tipleri (Flight Types)" name="flightTypes">
          <el-table :data="referenceStore.flightTypes" stripe style="width: 100%">
            <el-table-column prop="flightTypeId" label="ID" width="80" />
            <el-table-column prop="flightTypeName" label="Uçuş Tipi Adı" min-width="180" />
            <el-table-column prop="flightTypeCode" label="Kod" width="120" />
            <el-table-column
              v-if="authStore.canManageReferenceData"
              label="İşlem"
              width="160"
              align="center"
            >
              <template #default="{ row }">
                <el-button size="small" type="primary" plain @click="handleOpenEdit(row)">Düzenle</el-button>
                <el-button size="small" type="danger" plain @click="handleDelete(row)">Sil</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <ReferenceFormModal
      v-if="modalVisible"
      v-model:visible="modalVisible"
      :tab="activeTab"
      :item-to-edit="itemToEdit"
    />
  </div>
</template>

<style scoped>
.reference-view {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-bar h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
}

.header-bar p {
  margin: 4px 0 0 0;
  font-size: 13px;
  color: #64748b;
}
</style>
