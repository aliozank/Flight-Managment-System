<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'
import { useReferenceStore } from '@/stores/referenceStore'
import { useFlightStore } from '@/stores/flightStore'
import StatusTag from '@/components/common/StatusTag.vue'

const router = useRouter()
const authStore = useAuthStore()
const referenceStore = useReferenceStore()
const flightStore = useFlightStore()
const currentTime = ref(Date.now())
let clockInterval: ReturnType<typeof setInterval> | undefined

onMounted(async () => {
  clockInterval = setInterval(() => {
    currentTime.value = Date.now()
  }, 60_000)

  await Promise.all([
    referenceStore.fetchAllReferences(),
    flightStore.fetchFlights()
  ])
})

onUnmounted(() => {
  if (clockInterval) clearInterval(clockInterval)
})

const todayFlights = computed(() => {
  const start = new Date(currentTime.value)
  start.setHours(0, 0, 0, 0)
  const end = new Date(start)
  end.setDate(end.getDate() + 1)

  return flightStore.flights.filter((flight) => {
    const departureTime = new Date(flight.scheduledDepartureAt).getTime()
    return departureTime >= start.getTime() && departureTime < end.getTime()
  })
})

const totalFlights = computed(() => todayFlights.value.length)
const scheduledCount = computed(() => todayFlights.value.filter(
  (flight) => flight.flightStatus === 'SCHEDULED' || flight.flightStatus === 'DELAYED'
).length)
const departedCount = computed(() => todayFlights.value.filter((f) => f.flightStatus === 'DEPARTED').length)
const arrivedCount = computed(() => todayFlights.value.filter((f) => f.flightStatus === 'ARRIVED').length)
const cancelledCount = computed(() => todayFlights.value.filter((f) => f.flightStatus === 'CANCELLED').length)

const todayOperations = computed(() => [...todayFlights.value]
  .sort((first, second) => {
    const terminalStatuses = ['ARRIVED', 'CANCELLED']
    const firstIsTerminal = terminalStatuses.includes(first.flightStatus)
    const secondIsTerminal = terminalStatuses.includes(second.flightStatus)

    if (firstIsTerminal !== secondIsTerminal) return firstIsTerminal ? 1 : -1
    if (firstIsTerminal) {
      return new Date(second.scheduledDepartureAt).getTime() - new Date(first.scheduledDepartureAt).getTime()
    }
    return new Date(first.scheduledDepartureAt).getTime() - new Date(second.scheduledDepartureAt).getTime()
  })
  .slice(0, 8))

const upcomingFlights = computed(() => {
  const endTime = currentTime.value + 24 * 60 * 60 * 1000

  return flightStore.flights
    .filter((flight) => {
      const departureTime = new Date(flight.scheduledDepartureAt).getTime()
      const isAwaitingDeparture = flight.flightStatus === 'SCHEDULED' || flight.flightStatus === 'DELAYED'
      return isAwaitingDeparture && departureTime >= currentTime.value && departureTime <= endTime
    })
    .sort((first, second) => (
      new Date(first.scheduledDepartureAt).getTime() - new Date(second.scheduledDepartureAt).getTime()
    ))
    .slice(0, 8)
})

const formatTimeUntilDeparture = (departureAt: string): string => {
  const remainingMinutes = Math.max(
    0,
    Math.ceil((new Date(departureAt).getTime() - currentTime.value) / 60_000)
  )
  const hours = Math.floor(remainingMinutes / 60)
  const minutes = remainingMinutes % 60

  return hours === 0 ? `${minutes} dk` : `${hours} sa ${minutes} dk`
}

const goToFlights = () => router.push('/flights')
</script>

<template>
  <div class="dashboard-view">
    <!-- Welcome Hero Header -->
    <div class="welcome-card glow-banner">
      <div class="welcome-text">
        <div class="welcome-badge">
          <span class="pulse-emerald" />
          <span>CANLI OPERASYON RADARI</span>
        </div>
        <h2>Hoş Geldin, {{ authStore.user?.userName || 'Operatör' }} 👋</h2>
        <p>Real-time WebSocket & STOMP uçuş takip paneli canlı sistem göstergeleri</p>
      </div>
      <div class="quick-actions">
        <el-button type="primary" size="large" class="emerald-btn" @click="goToFlights">
          ✈️ Uçuşları Yönet ➔
        </el-button>
      </div>
    </div>

    <!-- Operations Metrics Cards -->
    <div class="metrics-grid">
      <div class="metric-card emerald-glow">
        <div class="metric-icon-box emerald">✈️</div>
        <div class="metric-info">
          <span class="metric-value">{{ totalFlights }}</span>
          <span class="metric-label">Bugünkü Uçuş</span>
        </div>
      </div>

      <div class="metric-card cyan-glow">
        <div class="metric-icon-box cyan">📅</div>
        <div class="metric-info">
          <span class="metric-value">{{ scheduledCount }}</span>
          <span class="metric-label">Planlanan / Rötarlı</span>
        </div>
      </div>

      <div class="metric-card warning-glow">
        <div class="metric-icon-box amber">🛫</div>
        <div class="metric-info">
          <span class="metric-value">{{ departedCount }}</span>
          <span class="metric-label">Havada / Kalktı</span>
        </div>
      </div>

      <div class="metric-card success-glow">
        <div class="metric-icon-box green">🛬</div>
        <div class="metric-info">
          <span class="metric-value">{{ arrivedCount }}</span>
          <span class="metric-label">İndi (Arrived)</span>
        </div>
      </div>

      <div class="metric-card danger-glow">
        <div class="metric-icon-box red">❌</div>
        <div class="metric-info">
          <span class="metric-value">{{ cancelledCount }}</span>
          <span class="metric-label">İptal Edilen</span>
        </div>
      </div>
    </div>

    <!-- Today's Flights Overview -->
    <el-card shadow="never" class="overview-card">
      <template #header>
        <div class="card-header">
          <span>📋 Bugünün Uçuş Operasyonu</span>
          <el-button type="primary" link @click="goToFlights">Tüm Uçuşları Gör ➔</el-button>
        </div>
      </template>

      <el-table
        v-loading="flightStore.loading"
        :data="todayOperations"
        stripe
        style="width: 100%"
        empty-text="Bugün için uçuş bulunmuyor"
      >
        <el-table-column prop="flightNumber" label="Uçuş No" width="130">
          <template #default="{ row }">
            <span class="flight-badge">{{ row.flightNumber }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="flightDate" label="Tarih" width="120" />

        <el-table-column label="Kalkış Havalimanı" min-width="180">
          <template #default="{ row }">
            {{ referenceStore.getAirportLabel(row.originAirportId) }}
          </template>
        </el-table-column>

        <el-table-column label="Varış Havalimanı" min-width="180">
          <template #default="{ row }">
            {{ referenceStore.getAirportLabel(row.destinationAirportId) }}
          </template>
        </el-table-column>

        <el-table-column prop="scheduledDepartureTime" label="STD" width="110" />
        <el-table-column prop="scheduledArrivalTime" label="STA" width="110" />

        <el-table-column label="Durum" width="140">
          <template #default="{ row }">
            <StatusTag :status="row.flightStatus" />
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card shadow="never" class="overview-card upcoming-card">
      <template #header>
        <div class="card-header">
          <div class="upcoming-title">
            <span>⏱️ Önümüzdeki 24 Saatte Kalkacak Uçuşlar</span>
            <small>Planlanan ve rötarlı uçuşlar, kalkış zamanına göre sıralanır.</small>
          </div>
          <el-button type="primary" link @click="goToFlights">Operasyonu Yönet ➔</el-button>
        </div>
      </template>

      <el-table
        v-loading="flightStore.loading"
        :data="upcomingFlights"
        stripe
        style="width: 100%"
        empty-text="Önümüzdeki 24 saat içinde kalkacak uçuş bulunmuyor"
      >
        <el-table-column prop="flightNumber" label="Uçuş No" width="130">
          <template #default="{ row }">
            <span class="flight-badge">{{ row.flightNumber }}</span>
          </template>
        </el-table-column>

        <el-table-column label="Güzergah" min-width="230">
          <template #default="{ row }">
            {{ referenceStore.getAirportLabel(row.originAirportId) }}
            ➔
            {{ referenceStore.getAirportLabel(row.destinationAirportId) }}
          </template>
        </el-table-column>

        <el-table-column prop="flightDate" label="Tarih" width="120" />
        <el-table-column prop="scheduledDepartureTime" label="Kalkış" width="100" />

        <el-table-column label="Kalkışa Kalan" width="140">
          <template #default="{ row }">
            <span class="countdown-badge">{{ formatTimeUntilDeparture(row.scheduledDepartureAt) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="Durum" width="140">
          <template #default="{ row }">
            <StatusTag :status="row.flightStatus" />
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.dashboard-view {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.welcome-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 30px 36px;
  background: linear-gradient(135deg, #090d16 0%, #0f172a 60%, #064e3b 100%);
  border-radius: 16px;
  color: #ffffff;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2);
  border: 1px solid rgba(16, 185, 129, 0.25);
  position: relative;
  overflow: hidden;
}

.welcome-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 4px 12px;
  background: rgba(16, 185, 129, 0.15);
  border: 1px solid rgba(16, 185, 129, 0.3);
  border-radius: 20px;
  font-size: 10px;
  font-weight: 800;
  color: #34d399;
  letter-spacing: 1px;
  margin-bottom: 10px;
}

.pulse-emerald {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background-color: #10b981;
  box-shadow: 0 0 10px #10b981;
}

.welcome-text h2 {
  margin: 0;
  font-size: 24px;
  font-weight: 800;
  color: #ffffff;
}

.welcome-text p {
  margin: 6px 0 0 0;
  font-size: 14px;
  color: #94a3b8;
}

.emerald-btn {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%) !important;
  border: none !important;
  font-weight: 800 !important;
  box-shadow: 0 6px 20px rgba(16, 185, 129, 0.4) !important;

  padding: 12px 24px !important;
  border-radius: 10px !important;
  transition: all 0.25s ease !important;
}

.emerald-btn:hover {
  transform: translateY(-2px) !important;
  box-shadow: 0 10px 28px rgba(16, 185, 129, 0.6) !important;
}

.metrics-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
}

.metric-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 22px;
  background-color: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.02);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.metric-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.1);
}

.metric-card.emerald-glow:hover {
  border-color: #10b981;
  box-shadow: 0 10px 24px rgba(16, 185, 129, 0.2);
}

.metric-card.cyan-glow:hover {
  border-color: #0284c7;
  box-shadow: 0 10px 24px rgba(2, 132, 199, 0.2);
}

.metric-icon-box {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
}

.metric-icon-box.emerald { background: #ecfdf5; border: 1px solid #a7f3d0; }
.metric-icon-box.cyan { background: #f0f9ff; border: 1px solid #bae6fd; }
.metric-icon-box.amber { background: #fffbeb; border: 1px solid #fde68a; }
.metric-icon-box.green { background: #f0fdf4; border: 1px solid #bbf7d0; }
.metric-icon-box.red { background: #fef2f2; border: 1px solid #fecaca; }

.metric-info {
  display: flex;
  flex-direction: column;
}

.metric-value {
  font-size: 28px;
  font-weight: 900;
  color: #0f172a;
  line-height: 1;
}

.metric-label {
  font-size: 12px;
  color: #64748b;
  font-weight: 600;
  margin-top: 4px;
}

.overview-card {
  border: 1px solid #e2e8f0;
  border-radius: 14px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 700;
  font-size: 15px;
  color: #0f172a;
}

.flight-badge {
  font-weight: 800;
  color: #0284c7;
}

.upcoming-title {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.upcoming-title small {
  color: #64748b;
  font-size: 12px;
  font-weight: 500;
}

.upcoming-card {
  border-color: #bae6fd;
}

.countdown-badge {
  display: inline-flex;
  padding: 4px 9px;
  border-radius: 999px;
  background: #eff6ff;
  color: #1d4ed8;
  font-size: 12px;
  font-weight: 700;
}
</style>
