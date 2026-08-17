<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/authStore'
import { useReferenceStore } from '@/stores/referenceStore'
import { useFlightStore } from '@/stores/flightStore'
import StatusTag from '@/components/common/StatusTag.vue'
import FlightFormModal from '@/components/flight/FlightFormModal.vue'
import FlightCsvUploadModal from '@/components/flight/FlightCsvUploadModal.vue'
import MockGeneratorModal from '@/components/flight/MockGeneratorModal.vue'
import type { FlightResponse, FlightCreateRequest, FlightUpdateRequest, FlightStatus } from '@/types/flight'

const authStore = useAuthStore()
const referenceStore = useReferenceStore()
const flightStore = useFlightStore()

const formModalVisible = ref(false)
const formSaving = ref(false)
const selectedFlightToEdit = ref<FlightResponse | null>(null)
const csvModalVisible = ref(false)
const mockModalVisible = ref(false)

onMounted(async () => {
  await Promise.all([
    referenceStore.fetchAllReferences(),
    flightStore.fetchFlights()
  ])
})

const handleOpenCreateModal = () => {
  selectedFlightToEdit.value = null
  formModalVisible.value = true
}

const handleOpenEditModal = (flight: FlightResponse) => {
  selectedFlightToEdit.value = flight
  formModalVisible.value = true
}

const handleSaveCreate = async (payload: FlightCreateRequest) => {
  if (formSaving.value) return

  formSaving.value = true
  try {
    await flightStore.createFlight(payload)
    ElMessage.success('Yeni uçuş başarıyla oluşturuldu')
    formModalVisible.value = false
  } catch {
    // Handled in axios interceptor
  } finally {
    formSaving.value = false
  }
}

const handleSaveUpdate = async ({ id, data }: { id: number; data: FlightUpdateRequest }) => {
  if (formSaving.value) return

  formSaving.value = true
  try {
    await flightStore.updateFlight(id, data)
    ElMessage.success('Uçuş başarıyla güncellendi')
    formModalVisible.value = false
  } catch {
    // Handled in axios interceptor
  } finally {
    formSaving.value = false
  }
}

const statusLabels: Record<FlightStatus, string> = {
  SCHEDULED: 'Planlandı',
  DELAYED: 'Rötarlı',
  DEPARTED: 'Kalktı',
  ARRIVED: 'İndi',
  CANCELLED: 'İptal Edildi'
}

const getAllowedStatusTransitions = (status: FlightStatus): FlightStatus[] => {
  switch (status) {
    case 'SCHEDULED':
      return ['DELAYED', 'DEPARTED']
    case 'DELAYED':
      return ['SCHEDULED', 'DEPARTED']
    case 'DEPARTED':
      return ['ARRIVED']
    case 'ARRIVED':
    case 'CANCELLED':
      return []
  }
}

const canEditFlight = (status: FlightStatus) => status === 'SCHEDULED' || status === 'DELAYED'

const canCancelFlight = (status: FlightStatus) => status === 'SCHEDULED' || status === 'DELAYED'

const handleStatusCommand = async (flight: FlightResponse, command: string | number | object) => {
  const targetStatus = command as FlightStatus

  try {
    await ElMessageBox.confirm(
      `#${flight.flightNumber} uçuşu ${statusLabels[targetStatus]} durumuna geçirilsin mi?`,
      'Durum Güncelleme Onayı',
      {
        confirmButtonText: 'Güncelle',
        cancelButtonText: 'Vazgeç',
        type: 'warning'
      }
    )

    await flightStore.updateFlightStatus(flight.flightId, targetStatus)
    ElMessage.success(`Uçuş durumu ${statusLabels[targetStatus]} olarak güncellendi`)
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      // API errors are handled by the axios interceptor.
    }
  }
}

const handleCancelFlight = (flight: FlightResponse) => {
  ElMessageBox.confirm(
    `#${flight.flightNumber} numaralı uçuşu iptal etmek istediğinizden emin misiniz?`,
    'Uçuş İptal Onayı',
    {
      confirmButtonText: 'Evet, İptal Et',
      cancelButtonText: 'Vazgeç',
      type: 'warning',
      confirmButtonClass: 'el-button--danger'
    }
  )
    .then(async () => {
      try {
        await flightStore.cancelFlight(flight.flightId)
        ElMessage.success('Uçuş iptal edildi')
      } catch {
        // Handled in interceptor
      }
    })
    .catch(() => {})
}
</script>

<template>
  <div class="flights-view">
    <!-- Top Action Toolbar -->
    <div class="toolbar-header">
      <div class="header-titles">
        <h2>Uçuş Operasyon Yönetimi</h2>
        <p>Sistemdeki tüm aktif ve planlanan uçuşların anlık kontrolü</p>
      </div>

      <div class="action-buttons" v-if="authStore.canManageFlights">
        <el-button type="primary" size="large" @click="handleOpenCreateModal">
          ✈️ Yeni Uçuş Ekle
        </el-button>
        <el-button type="success" plain size="large" @click="csvModalVisible = true">
          📁 CSV Yükle
        </el-button>
        <el-button type="warning" plain size="large" @click="mockModalVisible = true">
          🎲 Mock Veri Üret
        </el-button>
      </div>
    </div>

    <!-- Filters Card -->
    <el-card shadow="never" class="filters-card">
      <div class="filters-grid">
        <div class="filter-item">
          <label class="filter-label">Uçuş Numarası</label>
          <el-input
            v-model="flightStore.searchQuery"
            placeholder="Örn: TK1234"
            clearable
          />
        </div>

        <div class="filter-item">
          <label class="filter-label">Havayolu</label>
          <el-select
            v-model="flightStore.selectedAirlineId"
            placeholder="Tüm Havayolları"
            clearable
            filterable
          >
            <el-option
              v-for="item in referenceStore.airlines"
              :key="item.airlineId"
              :label="referenceStore.getAirlineLabel(item.airlineId)"
              :value="item.airlineId"
            />
          </el-select>
        </div>

        <div class="filter-item">
          <label class="filter-label">Uçuş Durumu</label>
          <el-select
            v-model="flightStore.selectedStatus"
            placeholder="Tüm Durumlar"
            clearable
          >
            <el-option label="Planlandı (SCHEDULED)" value="SCHEDULED" />
            <el-option label="Rötarlı (DELAYED)" value="DELAYED" />
            <el-option label="Kalktı (DEPARTED)" value="DEPARTED" />
            <el-option label="İndi (ARRIVED)" value="ARRIVED" />
            <el-option label="İptal Edildi (CANCELLED)" value="CANCELLED" />
          </el-select>
        </div>

        <div class="filter-item">
          <label class="filter-label">Kalkış Havalimanı</label>
          <el-select
            v-model="flightStore.selectedOriginId"
            placeholder="Kalkış Noktası"
            clearable
            filterable
          >
            <el-option
              v-for="item in referenceStore.airports"
              :key="item.airportId"
              :label="referenceStore.getAirportLabel(item.airportId)"
              :value="item.airportId"
            />
          </el-select>
        </div>

        <div class="filter-item">
          <label class="filter-label">Varış Havalimanı</label>
          <el-select
            v-model="flightStore.selectedDestinationId"
            placeholder="Varış Noktası"
            clearable
            filterable
          >
            <el-option
              v-for="item in referenceStore.airports"
              :key="item.airportId"
              :label="referenceStore.getAirportLabel(item.airportId)"
              :value="item.airportId"
            />
          </el-select>
        </div>

        <div class="filter-item">
          <label class="filter-label">Tarih</label>
          <el-date-picker
            v-model="flightStore.selectedDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="Uçuş Tarihi"
            clearable
          />
        </div>

        <div class="filter-item reset-item">
          <el-button @click="flightStore.resetFilters">Filtreleri Sıfırla</el-button>
        </div>
      </div>
    </el-card>

    <!-- Table Card -->
    <el-card shadow="never" class="table-card">
      <el-table
        v-loading="flightStore.loading"
        :data="flightStore.paginatedFlights"
        stripe
        style="width: 100%"
        empty-text="Filtrelere uygun uçuş bulunamadı"
      >
        <el-table-column prop="flightNumber" label="Uçuş No" width="110">
          <template #default="{ row }">
            <span class="flight-number-badge">{{ row.flightNumber }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="flightDate" label="Tarih" width="105" />

        <el-table-column label="Havayolu" min-width="140">
          <template #default="{ row }">
            <span class="airline-text">{{ referenceStore.getAirlineLabel(row.airlineId) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="Güzergah & Saat (STD ➔ STA)" min-width="240">
          <template #default="{ row }">
            <div class="route-time-cell">
              <span class="route-codes">
                {{ referenceStore.getAirportCode(row.originAirportId) }} ➔ {{ referenceStore.getAirportCode(row.destinationAirportId) }}
              </span>
              <span class="time-range">
                {{ row.scheduledDepartureTime?.substring(0, 5) }} - {{ row.scheduledArrivalTime?.substring(0, 5) }}
              </span>
              <span v-if="row.scheduledArrivalDate !== row.flightDate" class="date-range">
                {{ row.flightDate }} ➔ {{ row.scheduledArrivalDate }}
              </span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="Uçak (Tescil / Tip)" min-width="150">
          <template #default="{ row }">
            <div class="aircraft-cell">
              <span class="ac-reg">{{ referenceStore.getAircraftLabel(row.aircraftId) }}</span>
              <span class="ac-type">{{ referenceStore.getAircraftTypeLabel(row.aircraftTypeId) }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="Uçuş Tipi" width="130">
          <template #default="{ row }">
            <el-tag size="small" type="info" effect="plain" class="flight-type-badge">
              {{ referenceStore.getFlightTypeLabel(row.flightTypeId) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="Durum" width="130">
          <template #default="{ row }">
            <StatusTag :status="row.flightStatus" />
          </template>
        </el-table-column>

        <!-- Actions -->
        <el-table-column
          v-if="authStore.canManageFlights"
          label="İşlemler"
          width="250"
          align="center"
        >
          <template #default="{ row }">
            <div class="action-buttons-group">
              <el-button
                size="small"
                type="primary"
                class="action-btn-edit"
                :disabled="!canEditFlight(row.flightStatus)"
                @click="handleOpenEditModal(row)"
              >
                Düzenle
              </el-button>

              <el-dropdown
                v-if="getAllowedStatusTransitions(row.flightStatus).length > 0"
                trigger="click"
                @command="handleStatusCommand(row, $event)"
              >
                <el-button size="small" type="warning" plain>
                  Durum
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item
                      v-for="status in getAllowedStatusTransitions(row.flightStatus)"
                      :key="status"
                      :command="status"
                    >
                      {{ statusLabels[status] }} ({{ status }})
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>

              <el-button
                v-if="authStore.canCancelFlight"
                size="small"
                type="danger"
                class="action-btn-cancel"
                :disabled="!canCancelFlight(row.flightStatus)"
                @click="handleCancelFlight(row)"
              >
                İptal
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- Pagination -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="flightStore.currentPage"
          v-model:page-size="flightStore.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="flightStore.totalFilteredCount"
        />
      </div>
    </el-card>

    <!-- Modals -->
    <FlightFormModal
      v-if="formModalVisible"
      v-model:visible="formModalVisible"
      :flight-to-edit="selectedFlightToEdit"
      :saving="formSaving"
      @save-create="handleSaveCreate"
      @save-update="handleSaveUpdate"
    />

    <FlightCsvUploadModal v-if="csvModalVisible" v-model:visible="csvModalVisible" />
    <MockGeneratorModal v-if="mockModalVisible" v-model:visible="mockModalVisible" />
  </div>
</template>

<style scoped>
.flights-view {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.toolbar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-titles h2 {
  margin: 0;
  font-size: 22px;
  font-weight: 800;
  color: #0f172a;
}

.header-titles p {
  margin: 4px 0 0 0;
  font-size: 14px;
  color: #64748b;
}

.action-buttons {
  display: flex;
  gap: 12px;
}

.filters-card {
  border: 1px solid #e2e8f0;
  background-color: #ffffff;
}

.filters-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 16px;
  align-items: end;
}

.filter-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.filter-label {
  font-size: 12px;
  font-weight: 600;
  color: #475569;
}

.reset-item {
  justify-content: flex-end;
}

.table-card {
  border: 1px solid #e2e8f0;
  background-color: #ffffff;
}

.flight-number-badge {
  font-weight: 800;
  color: #0284c7;
  letter-spacing: 0.5px;
}

.route-time-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.route-codes {
  font-weight: 700;
  color: #0f172a;
  font-size: 13px;
}

.time-range {
  font-size: 11px;
  color: #64748b;
  font-weight: 600;
}

.date-range {
  font-size: 10px;
  color: #b45309;
  font-weight: 600;
}

.aircraft-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.ac-reg {
  font-weight: 700;
  color: #334155;
  font-size: 12px;
}

.ac-type {
  font-size: 11px;
  color: #94a3b8;
}

.airline-text {
  font-weight: 600;
  color: #0f172a;
}

.flight-type-badge {
  white-space: nowrap !important;
  font-weight: 600 !important;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 24px;
}
</style>
