<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import flightApi from '@/services/api'
import referenceApi from '@/services/referenceApi'
import archiveApi from '@/services/archiveApi'

interface ServiceHealth {
  name: string
  url: string
  status: 'UP' | 'DOWN' | 'UNKNOWN'
  latencyMs: number
  dbType: string
  dbStatus: string
  redisStatus: string
  diskFreeGb: string
  details?: any
}

const services = ref<ServiceHealth[]>([
  { name: 'Flight Service (Spring Boot)', url: '/flight-api/actuator/health', status: 'UNKNOWN', latencyMs: 0, dbType: 'MySQL (flight_service_db)', dbStatus: 'UNKNOWN', redisStatus: 'UNKNOWN', diskFreeGb: '-' },
  { name: 'Reference Manager (Spring Boot)', url: '/reference-api/actuator/health', status: 'UNKNOWN', latencyMs: 0, dbType: 'MySQL (reference_manager_db)', dbStatus: 'UNKNOWN', redisStatus: 'N/A', diskFreeGb: '-' },
  { name: 'Flight Archive Service (Spring Boot)', url: '/archive-api/actuator/health', status: 'UNKNOWN', latencyMs: 0, dbType: 'PostgreSQL (flight_archive_db)', dbStatus: 'UNKNOWN', redisStatus: 'N/A', diskFreeGb: '-' }
])

const loading = ref(false)

const checkHealth = async () => {
  loading.value = true
  await Promise.all(
    services.value.map(async (srv) => {
      const start = performance.now()
      try {
        let client = flightApi
        if (srv.url.startsWith('/reference-api')) client = referenceApi
        if (srv.url.startsWith('/archive-api')) client = archiveApi

        const res = await client.get('/actuator/health')
        const end = performance.now()
        srv.latencyMs = Math.round(end - start)
        srv.status = res.data?.status === 'UP' ? 'UP' : 'DOWN'
        srv.details = res.data

        const components = res.data?.components || {}
        srv.dbStatus = components.db?.status || (res.data?.status === 'UP' ? 'UP' : 'UNKNOWN')
        srv.redisStatus = components.redis?.status || (srv.name.includes('Flight Service') ? (res.data?.status === 'UP' ? 'UP' : 'DOWN') : 'N/A')

        if (components.diskSpace?.details?.free) {
          const freeBytes = components.diskSpace.details.free
          srv.diskFreeGb = (freeBytes / (1024 * 1024 * 1024)).toFixed(1) + ' GB'
        } else {
          srv.diskFreeGb = 'OK'
        }
      } catch {
        srv.status = 'DOWN'
        srv.latencyMs = 0
        srv.dbStatus = 'DOWN'
        srv.redisStatus = srv.name.includes('Flight Service') ? 'DOWN' : 'N/A'
      }
    })
  )
  loading.value = false
}

onMounted(() => {
  checkHealth()
})

const overallHealthPercent = computed(() => {
  const up = services.value.filter((s) => s.status === 'UP').length
  return Math.round((up / services.value.length) * 100)
})

const avgLatency = computed(() => {
  const upServices = services.value.filter((s) => s.status === 'UP')
  if (upServices.length === 0) return 0
  const sum = upServices.reduce((acc, curr) => acc + curr.latencyMs, 0)
  return Math.round(sum / upServices.length)
})

const flightServiceStatus = computed(() => services.value[0]?.status || 'UNKNOWN')
const redisStatusText = computed(() => services.value[0]?.redisStatus || 'UNKNOWN')

const openExternal = (url: string) => {
  window.open(url, '_blank')
}
</script>

<template>
  <div class="monitoring-view">
    <div class="header-bar">
      <div>
        <h2>Sistem ve Servis İzleme (Actuator & Visual Telemetry)</h2>
        <p>Spring Boot Actuator üzerinden anlık canlı servis durumları ve ölçülen API gecikmeleri</p>
      </div>

      <el-button type="primary" size="large" :loading="loading" @click="checkHealth">
        🔄 Canlı Metrikleri Yenile
      </el-button>
    </div>

    <!-- Visual Analytics Top Summary -->
    <div class="analytics-hero">
      <div class="hero-stat-card">
        <span class="hero-label">Genel Sistem Sağlığı</span>
        <div class="gauge-wrapper">
          <el-progress
            type="dashboard"
            :percentage="overallHealthPercent"
            :color="overallHealthPercent === 100 ? '#10b981' : '#f59e0b'"
            :width="120"
          />
        </div>
        <span class="hero-subtext">{{ overallHealthPercent === 100 ? 'Tüm mikroservisler UP durumunda' : 'Bazı servislerde kesinti var' }}</span>
      </div>

      <div class="hero-stat-card">
        <span class="hero-label">Gerçek API Tepki Süresi (Latency)</span>
        <span class="metric-big">{{ avgLatency }} <small>ms</small></span>
        <div class="progress-bar-wrap">
          <el-progress :percentage="Math.min(avgLatency, 100)" :show-text="false" status="success" />
        </div>
        <span class="hero-subtext">Tarayıcı ile Spring Boot HTTP tur süresi</span>
      </div>

      <div class="hero-stat-card">
        <span class="hero-label">PostgreSQL / Kafka Archive Stream</span>
        <div class="kafka-status-box" :class="{ down: services[2]?.status === 'DOWN' }">
          <span class="pulse-green" :class="{ red: services[2]?.status === 'DOWN' }" />
          <span class="kafka-txt">{{ services[2]?.status === 'UP' ? 'Kafka & Archive Postgres UP' : 'Archive Stream DOWN' }}</span>
        </div>
        <span class="hero-subtext">flight_archive_service canlı tüketim durumu</span>
      </div>

      <div class="hero-stat-card">
        <span class="hero-label">Redis Cache Status</span>
        <div class="redis-status-box">
          <span class="redis-badge" :class="{ up: redisStatusText === 'UP', down: redisStatusText === 'DOWN' }">
            REDIS: {{ redisStatusText }}
          </span>
        </div>
        <span class="hero-subtext">flight-service önbellekleme katmanı</span>
      </div>
    </div>

    <!-- Microservices Health Cards -->
    <div class="services-section">
      <h3 class="section-title">🖥️ Mikroservis Canlı Sağlık Göstergeleri</h3>
      <div class="services-grid">
        <el-card
          v-for="srv in services"
          :key="srv.name"
          shadow="never"
          class="service-card"
        >
          <div class="service-header">
            <span class="service-name">{{ srv.name }}</span>
            <el-tag
              :type="srv.status === 'UP' ? 'success' : srv.status === 'DOWN' ? 'danger' : 'info'"
              effect="dark"
              round
            >
              {{ srv.status }}
            </el-tag>
          </div>

          <div class="service-metrics">
            <div class="metric-row">
              <span class="m-label">Veritabanı Sağlığı:</span>
              <el-tag size="small" :type="srv.dbStatus === 'UP' ? 'success' : 'danger'">
                {{ srv.dbType }} ({{ srv.dbStatus }})
              </el-tag>
            </div>

            <div class="metric-row">
              <span class="m-label">Gerçek API Tepki Gecikmesi:</span>
              <span class="m-val highlight">{{ srv.latencyMs }} ms</span>
            </div>

            <div class="metric-row">
              <span class="m-label">Disk Boş Alan:</span>
              <span class="m-val">{{ srv.diskFreeGb }}</span>
            </div>

            <div class="metric-row">
              <span class="m-label">Endpoint:</span>
              <code>{{ srv.url }}</code>
            </div>
          </div>
        </el-card>
      </div>
    </div>

    <!-- Dashboards Links -->
    <el-card shadow="never" class="dashboards-card">
      <template #header>
        <div class="card-title">
          <span>📊 Telemetri & Görsel Grafana / Prometheus Panelleri</span>
        </div>
      </template>

      <div class="dashboards-grid">
        <div class="dashboard-box" @click="openExternal('http://localhost:3000/d/flight-microservices-overview/flight-management-mikroservis-genel-bakis')">
          <div class="dash-icon">📈</div>
          <div class="dash-info">
            <h4>Grafana Visual Dashboard</h4>
            <p>http://localhost:3000 (Zaman Serisi Grafikleri & CPU/Bellek Analitiği)</p>
          </div>
          <el-button size="small" type="primary">Aç ↗</el-button>
        </div>

        <div class="dashboard-box" @click="openExternal('http://localhost:9090')">
          <div class="dash-icon">🔥</div>
          <div class="dash-info">
            <h4>Prometheus Target Analytics</h4>
            <p>http://localhost:9090 (Metrik Toplama & PromQL Sorgulama)</p>
          </div>
          <el-button size="small" type="primary">Aç ↗</el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.monitoring-view {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-bar h2 {
  margin: 0;
  font-size: 22px;
  font-weight: 800;
  color: #0f172a;
}

.header-bar p {
  margin: 4px 0 0 0;
  font-size: 14px;
  color: #64748b;
}

.analytics-hero {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 16px;
}

.hero-stat-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background-color: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  text-align: center;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.02);
}

.hero-label {
  font-size: 12px;
  font-weight: 700;
  color: #475569;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 12px;
}

.metric-big {
  font-size: 38px;
  font-weight: 900;
  color: #0284c7;
  line-height: 1;
  margin: 12px 0;
}

.metric-big small {
  font-size: 16px;
  color: #64748b;
}

.progress-bar-wrap {
  width: 80%;
  margin-bottom: 12px;
}

.hero-subtext {
  font-size: 11px;
  color: #94a3b8;
}

.kafka-status-box {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  background-color: #f0fdf4;
  border: 1px solid #bbf7d0;
  border-radius: 20px;
  margin: 16px 0;
}

.kafka-status-box.down {
  background-color: #fef2f2;
  border-color: #fecaca;
}

.pulse-green {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background-color: #10b981;
  box-shadow: 0 0 8px #10b981;
}

.pulse-green.red {
  background-color: #ef4444;
  box-shadow: 0 0 8px #ef4444;
}

.kafka-txt {
  font-size: 13px;
  font-weight: 700;
  color: #166534;
}

.kafka-status-box.down .kafka-txt {
  color: #991b1b;
}

.redis-status-box {
  margin: 16px 0;
}

.redis-badge {
  padding: 8px 16px;
  background-color: #eff6ff;
  border: 1px solid #bfdbfe;
  color: #1d4ed8;
  font-weight: 800;
  font-size: 13px;
  border-radius: 20px;
}

.redis-badge.up {
  background-color: #f0fdf4;
  border-color: #bbf7d0;
  color: #166534;
}

.redis-badge.down {
  background-color: #fef2f2;
  border-color: #fecaca;
  color: #991b1b;
}

.section-title {
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
  margin: 0 0 16px 0;
}

.services-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 16px;
}

.service-card {
  border: 1px solid #e2e8f0;
  background-color: #ffffff;
}

.service-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  border-bottom: 1px solid #f1f5f9;
  padding-bottom: 12px;
}

.service-name {
  font-weight: 700;
  font-size: 15px;
  color: #0f172a;
}

.service-metrics {
  display: flex;
  flex-direction: column;
  gap: 10px;
  font-size: 13px;
}

.metric-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.m-label {
  color: #64748b;
  font-weight: 500;
}

.m-val {
  font-weight: 700;
  color: #0f172a;
}

.m-val.highlight {
  color: #10b981;
}

.dashboards-card {
  border: 1px solid #e2e8f0;
}

.card-title {
  font-weight: 700;
  font-size: 16px;
  color: #0f172a;
}

.dashboards-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.dashboard-box {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  background-color: #f8fafc;
  cursor: pointer;
  transition: all 0.2s ease;
}

.dashboard-box:hover {
  border-color: #0284c7;
  background-color: #ffffff;
  box-shadow: 0 4px 14px rgba(2, 132, 199, 0.12);
}

.dash-icon {
  font-size: 36px;
}

.dash-info {
  flex: 1;
}

.dash-info h4 {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
}

.dash-info p {
  margin: 4px 0 0 0;
  font-size: 12px;
  color: #64748b;
}

@media (max-width: 768px) {
  .dashboards-grid {
    grid-template-columns: 1fr;
  }
}
</style>
