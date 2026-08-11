<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import archiveApi from '@/services/archiveApi'
import { useReferenceStore } from '@/stores/referenceStore'
import StatusTag from '@/components/common/StatusTag.vue'
import type { ArchivedFlightResponse } from '@/types/archive'

const referenceStore = useReferenceStore()
const archivedFlights = ref<ArchivedFlightResponse[]>([])
const loading = ref(false)
const searchQuery = ref('')
const selectedArchiveItem = ref<ArchivedFlightResponse | null>(null)
const drawerVisible = ref(false)

const fetchArchive = async () => {
  loading.value = true
  try {
    const res = await archiveApi.get<ArchivedFlightResponse[]>('/api/archived-flights')
    archivedFlights.value = res.data || []
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  referenceStore.fetchAllReferences()
  fetchArchive()
})

const filteredArchive = computed(() => {
  if (!searchQuery.value) return archivedFlights.value
  const q = searchQuery.value.toLowerCase()
  return archivedFlights.value.filter(
    (item) =>
      item.flightNumber.toLowerCase().includes(q) ||
      item.flightId.toString().includes(q) ||
      item.archiveId.toString().includes(q)
  )
})

const handleViewDetail = (item: ArchivedFlightResponse) => {
  selectedArchiveItem.value = item
  drawerVisible.value = true
}
</script>

<template>
  <div class="archive-view">
    <div class="header-bar">
      <div>
        <h2>Uçuş Arşiv Geçmişi (Flight Audit Log)</h2>
        <p>PostgreSQL ve Kafka olayları üzerinden otomatik arşivlenen son durum kayıtları</p>
      </div>

      <el-input
        v-model="searchQuery"
        placeholder="Uçuş No veya ID ile Ara..."
        clearable
        style="width: 280px"
      />
    </div>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="filteredArchive" stripe style="width: 100%">
        <el-table-column prop="archiveId" label="Arşiv ID" width="100" />
        <el-table-column prop="flightNumber" label="Uçuş No" width="120">
          <template #default="{ row }">
            <span class="flight-badge">{{ row.flightNumber }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="flightDate" label="Tarih" width="120" />

        <el-table-column label="Güzergah" min-width="200">
          <template #default="{ row }">
            {{ referenceStore.getAirportCode(row.originAirportId) }} ➔ {{ referenceStore.getAirportCode(row.destinationAirportId) }}
          </template>
        </el-table-column>

        <el-table-column label="Durum" width="140">
          <template #default="{ row }">
            <StatusTag :status="row.flightStatus" />
          </template>
        </el-table-column>

        <el-table-column prop="flightVersion" label="Versiyon" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" type="info">v{{ row.flightVersion }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="Olay Zamanı (Event Occurred)" min-width="180">
          <template #default="{ row }">
            {{ row.eventOccurredAt ? new Date(row.eventOccurredAt).toLocaleString('tr-TR') : '-' }}
          </template>
        </el-table-column>

        <el-table-column label="İşlem Yapan User ID" width="150" align="center">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">User #{{ row.changedByUserId }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="Detay" width="100" align="center">
          <template #default="{ row }">
            <el-button size="small" type="primary" class="action-btn-edit" @click="handleViewDetail(row)">
              İncele
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- Detail Drawer -->
    <el-drawer
      v-if="drawerVisible"
      v-model="drawerVisible"
      title="Arşiv Kayıt Detayı"
      size="500px"
      :append-to-body="true"
      destroy-on-close
    >
      <div v-if="selectedArchiveItem" class="detail-container">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="Archive Record ID">{{ selectedArchiveItem.archiveId }}</el-descriptions-item>
          <el-descriptions-item label="Kafka Event UUID">{{ selectedArchiveItem.eventId }}</el-descriptions-item>
          <el-descriptions-item label="Original Flight ID">{{ selectedArchiveItem.flightId }}</el-descriptions-item>
          <el-descriptions-item label="Flight Number">{{ selectedArchiveItem.flightNumber }}</el-descriptions-item>
          <el-descriptions-item label="Havayolu">{{ referenceStore.getAirlineLabel(selectedArchiveItem.airlineId) }}</el-descriptions-item>
          <el-descriptions-item label="Uçak (Registration)">{{ referenceStore.getAircraftLabel(selectedArchiveItem.aircraftId) }}</el-descriptions-item>
          <el-descriptions-item label="Uçak Tipi">{{ referenceStore.getAircraftTypeLabel(selectedArchiveItem.aircraftTypeId) }}</el-descriptions-item>
          <el-descriptions-item label="Kalkış Havalimanı">{{ referenceStore.getAirportLabel(selectedArchiveItem.originAirportId) }}</el-descriptions-item>
          <el-descriptions-item label="Varış Havalimanı">{{ referenceStore.getAirportLabel(selectedArchiveItem.destinationAirportId) }}</el-descriptions-item>
          <el-descriptions-item label="Uçuş Tarihi">{{ selectedArchiveItem.flightDate }}</el-descriptions-item>
          <el-descriptions-item label="STD / STA">{{ selectedArchiveItem.scheduledDepartureTime }} - {{ selectedArchiveItem.scheduledArrivalTime }}</el-descriptions-item>
          <el-descriptions-item label="Flight Status">
            <StatusTag :status="selectedArchiveItem.flightStatus" />
          </el-descriptions-item>
          <el-descriptions-item label="Flight Version">v{{ selectedArchiveItem.flightVersion }}</el-descriptions-item>
          <el-descriptions-item label="Changed By User">User #{{ selectedArchiveItem.changedByUserId }}</el-descriptions-item>
          <el-descriptions-item label="Archived At">{{ new Date(selectedArchiveItem.archivedAt).toLocaleString('tr-TR') }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.archive-view {
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

.flight-badge {
  font-weight: 700;
  color: #0284c7;
}

.detail-container {
  padding: 16px;
}
</style>
