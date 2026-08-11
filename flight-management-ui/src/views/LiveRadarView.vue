<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed, watch } from 'vue'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { useFlightStore } from '@/stores/flightStore'
import { useReferenceStore } from '@/stores/referenceStore'
import StatusTag from '@/components/common/StatusTag.vue'
import type { FlightResponse } from '@/types/flight'

const flightStore = useFlightStore()
const referenceStore = useReferenceStore()

const mapContainer = ref<HTMLElement | null>(null)
let map: L.Map | null = null
const markersMap = new Map<number, L.Marker>()
let currentPolyline: L.Polyline | null = null

// Pure 1x Real-Time Ticker
const followSelectedPlane = ref(false)
const selectedFlight = ref<FlightResponse | null>(null)
const nowTimeMs = ref(Date.now())
let clockTimer: ReturnType<typeof setInterval> | null = null

// Real GPS Coordinates + Real Runway Threshold Endpoints (Lat, Lng) for Exact OSM Overlay
const airportData: Record<string, {
  lat: number
  lng: number
  code: string
  name: string
  rwyStart: [number, number]
  rwyEnd: [number, number]
}> = {
  IST: {
    lat: 41.2753,
    lng: 28.7519,
    code: 'IST',
    name: 'İstanbul Havalimanı',
    rwyStart: [41.2600, 28.7450],
    rwyEnd: [41.2900, 28.7550]
  },
  SAW: {
    lat: 40.8986,
    lng: 29.3092,
    code: 'SAW',
    name: 'Sabiha Gökçen',
    rwyStart: [40.8940, 29.2980],
    rwyEnd: [40.9030, 29.3200]
  },
  ESB: {
    lat: 40.1281,
    lng: 32.9951,
    code: 'ESB',
    name: 'Ankara Esenboğa',
    rwyStart: [40.1170, 32.9880],
    rwyEnd: [40.1390, 33.0020]
  },
  ADB: {
    lat: 38.2924,
    lng: 27.1570,
    code: 'ADB',
    name: 'İzmir Adnan Menderes',
    rwyStart: [38.2840, 27.1530],
    rwyEnd: [38.3000, 27.1610]
  },
  AYT: {
    lat: 36.8987,
    lng: 30.8005,
    code: 'AYT',
    name: 'Antalya Havalimanı',
    rwyStart: [36.8880, 30.8000],
    rwyEnd: [36.9080, 30.8010]
  },
  LHR: {
    lat: 51.4700,
    lng: -0.4543,
    code: 'LHR',
    name: 'London Heathrow',
    rwyStart: [51.4770, -0.4750],
    rwyEnd: [51.4770, -0.4350]
  },
  CDG: {
    lat: 49.0097,
    lng: 2.5479,
    code: 'CDG',
    name: 'Paris Charles de Gaulle',
    rwyStart: [49.0060, 2.5300],
    rwyEnd: [49.0060, 2.5650]
  },
  AMS: {
    lat: 52.3105,
    lng: 4.7683,
    code: 'AMS',
    name: 'Amsterdam Schiphol',
    rwyStart: [52.3000, 4.7600],
    rwyEnd: [52.3200, 4.7650]
  },
  FRA: {
    lat: 50.0379,
    lng: 8.5622,
    code: 'FRA',
    name: 'Frankfurt Airport',
    rwyStart: [50.0340, 8.5450],
    rwyEnd: [50.0360, 8.5800]
  }
}

const getAirportGps = (airportId: number) => {
  const airport = referenceStore.airports.find((a) => a.airportId === airportId)
  if (airport && airport.airportIataCode && airportData[airport.airportIataCode]) {
    return airportData[airport.airportIataCode]!
  }
  return {
    lat: 41.0,
    lng: 29.0,
    code: 'APT',
    name: 'Havalimanı',
    rwyStart: [41.0, 29.0] as [number, number],
    rwyEnd: [41.01, 29.01] as [number, number]
  }
}

// Convert flight date & time string to Date timestamp (ms)
const getFlightTimestamp = (flightDate: string, timeStr: string): number => {
  try {
    const cleanTime = timeStr.length === 5 ? `${timeStr}:00` : timeStr
    const isoStr = `${flightDate}T${cleanTime}`
    const dt = new Date(isoStr)
    return dt.getTime()
  } catch {
    return Date.now()
  }
}

// PURE 1X REAL-TIME TELEMETRY ENGINE
const calculateFlightTelemetry = (flight: FlightResponse) => {
  const origin = getAirportGps(flight.originAirportId)
  const dest = getAirportGps(flight.destinationAirportId)

  const depMs = getFlightTimestamp(flight.flightDate, flight.scheduledDepartureTime)
  let arrMs = getFlightTimestamp(flight.flightDate, flight.scheduledArrivalTime)

  if (arrMs <= depMs) {
    arrMs += 24 * 60 * 60 * 1000
  }

  const totalDuration = arrMs - depMs
  const currentClock = nowTimeMs.value
  const elapsed = currentClock - depMs

  let progressFraction = 0
  let isAirborne = false

  if (flight.flightStatus === 'CANCELLED') {
    isAirborne = false
  } else if (currentClock < depMs) {
    progressFraction = 0
    isAirborne = false
  } else if (currentClock >= arrMs) {
    progressFraction = 1
    isAirborne = false
  } else {
    progressFraction = Math.max(0, Math.min(1, elapsed / totalDuration))
    isAirborne = true
  }

  // Exact Real GPS Position along line vector directly to Airport Runway Threshold
  const currentLat = origin.rwyEnd[0] + (dest.rwyStart[0] - origin.rwyEnd[0]) * progressFraction
  const currentLng = origin.rwyEnd[1] + (dest.rwyStart[1] - origin.rwyEnd[1]) * progressFraction

  const dLng = dest.rwyStart[1] - origin.rwyEnd[1]
  const dLat = dest.rwyStart[0] - origin.rwyEnd[0]
  const angleDeg = (Math.atan2(dLng, dLat) * 180) / Math.PI

  const isCruising = progressFraction > 0.1 && progressFraction < 0.9
  const altitude = isAirborne ? (isCruising ? 34000 : Math.round(progressFraction * 34000)) : 0
  const speed = isAirborne ? (isCruising ? 460 : Math.round(progressFraction * 460)) : 0

  const depTimeFormatted = new Date(depMs).toLocaleTimeString('tr-TR', { hour: '2-digit', minute: '2-digit' })
  const arrTimeFormatted = new Date(arrMs).toLocaleTimeString('tr-TR', { hour: '2-digit', minute: '2-digit' })

  return {
    lat: currentLat,
    lng: currentLng,
    angle: Math.round(angleDeg),
    progress: Math.round(progressFraction * 100),
    isAirborne,
    altitude,
    speed,
    depTimeFormatted,
    arrTimeFormatted,
    originGps: origin,
    destGps: dest
  }
}

// Pure 1x Real-Time Airborne Flights
const activeFlightsOnMap = computed(() => {
  return flightStore.flights.filter((f) => {
    const tele = calculateFlightTelemetry(f)
    return tele.isAirborne || f.flightStatus === 'DEPARTED'
  })
})

const initMap = () => {
  if (!mapContainer.value) return

  map = L.map(mapContainer.value, {
    center: [44.0, 18.0],
    zoom: 5,
    zoomControl: true,
    fadeAnimation: true,
    markerZoomAnimation: true
  })

  // CartoDB Dark Matter tile layer
  L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
    attribution: '&copy; OpenStreetMap contributors &copy; CARTO',
    subdomains: 'abcd',
    maxZoom: 19
  }).addTo(map)

  // CLEAN PLAIN TEXT AIRPORT LABELS & REAL OVERLAY RUNWAY STRIPS
  for (const item of Object.values(airportData)) {
    const cleanLabelIcon = L.divIcon({
      className: 'plain-airport-label-icon',
      html: `<div class="plain-label-box"><span class="dot"></span><span class="text">${item.code} - ${item.name}</span></div>`,
      iconSize: [160, 20],
      iconAnchor: [80, 10]
    })
    L.marker([item.lat, item.lng], { icon: cleanLabelIcon }).addTo(map)

    // Real Runway Overlay Polyline on OpenStreetMap Asphalt
    L.polyline([item.rwyStart, item.rwyEnd], {
      color: '#10b981',
      weight: 6,
      opacity: 0.9
    }).addTo(map).bindTooltip(`Pist / Runway ${item.code}`)
  }

  updateMapElements()
}

const updateMapElements = () => {
  if (!map) return
  const activeMap = map

  const activeIds = new Set(activeFlightsOnMap.value.map((f) => f.flightId))

  // Clean up markers no longer airborne
  markersMap.forEach((marker, id) => {
    if (!activeIds.has(id)) {
      marker.remove()
      markersMap.delete(id)
    }
  })

  // Polyline for selected flight
  if (currentPolyline) {
    currentPolyline.remove()
    currentPolyline = null
  }

  if (selectedFlight.value && activeIds.has(selectedFlight.value.flightId)) {
    const pos = calculateFlightTelemetry(selectedFlight.value)
    currentPolyline = L.polyline(
      [
        [pos.originGps.rwyEnd[0], pos.originGps.rwyEnd[1]],
        [pos.lat, pos.lng],
        [pos.destGps.rwyStart[0], pos.destGps.rwyStart[1]]
      ],
      {
        color: '#10b981',
        weight: 4,
        opacity: 0.95,
        dashArray: '8, 8'
      }
    ).addTo(activeMap)

    if (followSelectedPlane.value) {
      activeMap.panTo([pos.lat, pos.lng], { animate: true, duration: 0.5 })
    }
  }

  // Update Airplane Markers
  activeFlightsOnMap.value.forEach((flight) => {
    const pos = calculateFlightTelemetry(flight)
    const isSelected = selectedFlight.value?.flightId === flight.flightId

    const innerHtml = `
      <div class="fr24-plane-wrapper ${isSelected ? 'selected' : ''}">
        <div class="fr24-plane-icon" style="transform: rotate(${pos.angle}deg);">✈️</div>
        <span class="fr24-plane-label">${flight.flightNumber} (%${pos.progress})</span>
      </div>
    `

    if (!markersMap.has(flight.flightId)) {
      const icon = L.divIcon({
        className: 'fr24-div-icon',
        html: innerHtml,
        iconSize: [40, 40],
        iconAnchor: [20, 20]
      })
      const marker = L.marker([pos.lat, pos.lng], { icon }).addTo(activeMap)
      marker.on('click', () => selectFlight(flight))
      markersMap.set(flight.flightId, marker)
    } else {
      const marker = markersMap.get(flight.flightId)!
      marker.setLatLng([pos.lat, pos.lng])
      const icon = L.divIcon({
        className: 'fr24-div-icon',
        html: innerHtml,
        iconSize: [40, 40],
        iconAnchor: [20, 20]
      })
      marker.setIcon(icon)
    }
  })
}

watch(nowTimeMs, () => {
  updateMapElements()
})

watch(selectedFlight, () => {
  updateMapElements()
})

onMounted(async () => {
  await Promise.all([
    referenceStore.fetchAllReferences(),
    flightStore.fetchFlights()
  ])

  if (activeFlightsOnMap.value.length > 0) {
    selectedFlight.value = activeFlightsOnMap.value[0] ?? null
  }

  initMap()

  // Real 1x Clock Ticker (ticks every 1s)
  clockTimer = setInterval(() => {
    nowTimeMs.value = Date.now()
  }, 1000)
})

onUnmounted(() => {
  if (clockTimer) clearInterval(clockTimer)
  if (map) map.remove()
})

const selectFlight = (flight: FlightResponse) => {
  selectedFlight.value = flight
  if (map) {
    const pos = calculateFlightTelemetry(flight)
    map.panTo([pos.lat, pos.lng], { animate: true, duration: 0.8 })
  }
}
</script>

<template>
  <div class="radar-view">
    <!-- Radar Top Control Bar -->
    <div class="radar-header-card">
      <div class="header-info">
        <div class="live-pill">
          <span class="pulse-dot" />
          <span>REALTIME GERÇEK SAAT • {{ new Date(nowTimeMs).toLocaleTimeString('tr-TR') }}</span>
        </div>
        <h2>🛰️ Canlı Uçuş Radarı & Gerçek Pist İniş Görünümü</h2>
        <p>Havalimanlarına zoom yaparak pist üzerindeki teker koyma anlarını izleyin</p>
      </div>

      <div class="header-controls">
        <div class="toggle-box">
          <span class="control-label">Kamera Uçağa Kilitlensin:</span>
          <el-switch v-model="followSelectedPlane" active-color="#10b981" />
        </div>
        <el-button type="primary" plain @click="flightStore.fetchFlights()">
          🔄 Verileri Yenile
        </el-button>
      </div>
    </div>

    <div class="radar-layout">
      <!-- LEAFLET INTERACTIVE MAP CONTAINER -->
      <div class="map-wrapper">
        <div ref="mapContainer" class="leaflet-map" />
      </div>

      <!-- SIDEBAR: SELECTED FLIGHT TELEMETRY CARD -->
      <div class="telemetry-sidebar">
        <!-- ALERT BANNER WHEN NO AIRBORNE FLIGHTS IN REAL TIME -->
        <div v-if="activeFlightsOnMap.length === 0" class="no-departed-alert">
          <div class="alert-icon">⏰</div>
          <h4>Gerçek Saat Diliminde ({{ new Date(nowTimeMs).toLocaleTimeString('tr-TR') }}) Havada Olan Uçuş Bulunmuyor</h4>
          <p>Uçuş Yönetimi ekranından kalkış saatini <b>şu anki saatinize yakın</b> kurarak yeni bir uçuş oluşturabilir veya uçuş durumunu <b>"DEPARTED (Kalktı)"</b> yapabilirsiniz.</p>
        </div>

        <div v-else-if="selectedFlight" class="telemetry-card fade-in">
          <div class="card-top">
            <div class="plane-badge">
              <span class="badge-icon">✈️</span>
              <span class="flight-no">{{ selectedFlight.flightNumber }}</span>
            </div>
            <StatusTag :status="selectedFlight.flightStatus" />
          </div>

          <div class="route-banner">
            <div class="hub-box">
              <span class="iata">{{ referenceStore.getAirportCode(selectedFlight.originAirportId) }}</span>
              <span class="city">{{ referenceStore.getAirportLabel(selectedFlight.originAirportId) }}</span>
            </div>
            <div class="vector-arrow">
              <span>➔</span>
              <span class="progress-txt">%{{ calculateFlightTelemetry(selectedFlight).progress }}</span>
            </div>
            <div class="hub-box right">
              <span class="iata">{{ referenceStore.getAirportCode(selectedFlight.destinationAirportId) }}</span>
              <span class="city">{{ referenceStore.getAirportLabel(selectedFlight.destinationAirportId) }}</span>
            </div>
          </div>

          <el-progress
            :percentage="calculateFlightTelemetry(selectedFlight).progress"
            :color="'#10b981'"
            :stroke-width="10"
            class="progress-bar"
          />

          <div class="stats-grid">
            <div class="stat-item">
              <span class="stat-label">YÜKSEKLİK (ALTITUDE)</span>
              <span class="stat-val emerald">{{ calculateFlightTelemetry(selectedFlight).altitude }} FT</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">HIZ (GROUND SPEED)</span>
              <span class="stat-val cyan">{{ calculateFlightTelemetry(selectedFlight).speed }} KTS</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">KALKIŞ (STD)</span>
              <span class="stat-val">{{ calculateFlightTelemetry(selectedFlight).depTimeFormatted }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">TAHMİNİ İNİŞ (STA)</span>
              <span class="stat-val">{{ calculateFlightTelemetry(selectedFlight).arrTimeFormatted }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">UÇAK TESCİLİ</span>
              <span class="stat-val">{{ referenceStore.getAircraftLabel(selectedFlight.aircraftId) }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">UÇAK TİPİ</span>
              <span class="stat-val">{{ referenceStore.getAircraftTypeLabel(selectedFlight.aircraftTypeId) }}</span>
            </div>
          </div>
        </div>

        <div v-else class="empty-telemetry">
          <div class="empty-icon">🛰️</div>
          <p>Haritadaki bir uçağa tıklayarak canlı telemetri verilerini inceleyin.</p>
        </div>

        <!-- ACTIVE FLIGHTS LIST SUMMARY -->
        <div class="active-list-card">
          <div class="list-title">
            <span>📡 Havada Olan Canlı Uçuşlar ({{ activeFlightsOnMap.length }})</span>
          </div>
          <div class="flights-mini-list">
            <div
              v-for="flight in activeFlightsOnMap"
              :key="'mini-' + flight.flightId"
              class="mini-item"
              :class="{ selected: selectedFlight?.flightId === flight.flightId }"
              @click="selectFlight(flight)"
            >
              <div class="mini-left">
                <span class="mini-no">{{ flight.flightNumber }} (%{{ calculateFlightTelemetry(flight).progress }})</span>
                <span class="mini-route">
                  {{ referenceStore.getAirportCode(flight.originAirportId) }} ➔ {{ referenceStore.getAirportCode(flight.destinationAirportId) }} ({{ flight.scheduledDepartureTime }})
                </span>
              </div>
              <StatusTag :status="flight.flightStatus" />
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.radar-view {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.radar-header-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px 30px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
}

.live-pill {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 4px 12px;
  background: #ecfdf5;
  border: 1px solid #a7f3d0;
  border-radius: 20px;
  font-size: 10px;
  font-weight: 800;
  color: #059669;
  letter-spacing: 1px;
  margin-bottom: 8px;
}

.pulse-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #10b981;
  box-shadow: 0 0 10px #10b981;
}

.header-info h2 {
  margin: 0;
  font-size: 22px;
  font-weight: 800;
  color: #0f172a;
}

.header-info p {
  margin: 4px 0 0 0;
  font-size: 14px;
  color: #64748b;
}

.header-controls {
  display: flex;
  align-items: center;
  gap: 20px;
}

.toggle-box {
  display: flex;
  align-items: center;
  gap: 10px;
}

.control-label {
  font-size: 13px;
  font-weight: 600;
  color: #475569;
}

.radar-layout {
  display: grid;
  grid-template-columns: 1fr 380px;
  gap: 20px;
}

.map-wrapper {
  width: 100%;
  height: 640px;
  border-radius: 16px;
  overflow: hidden;
  border: 1px solid #cbd5e1;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.15);
}

.leaflet-map {
  width: 100%;
  height: 100%;
  background-color: #090d16;
}

/* CLEAN PLAIN AIRPORT LABELS */
:deep(.plain-airport-label-icon) {
  display: flex;
  align-items: center;
  justify-content: center;
}

:deep(.plain-label-box) {
  display: flex;
  align-items: center;
  gap: 6px;
  background: rgba(15, 23, 42, 0.75);
  border: 1px solid rgba(56, 189, 248, 0.4);
  padding: 2px 8px;
  border-radius: 10px;
  backdrop-filter: blur(4px);
}

:deep(.plain-label-box .dot) {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background-color: #38bdf8;
  box-shadow: 0 0 6px #38bdf8;
}

:deep(.plain-label-box .text) {
  font-size: 10px;
  font-weight: 800;
  color: #f8fafc;
  white-space: nowrap;
}

/* LEAFLET PLANE STYLES */
:deep(.fr24-plane-wrapper) {
  position: relative;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

:deep(.fr24-plane-icon) {
  font-size: 22px;
  filter: drop-shadow(0 0 8px rgba(16, 185, 129, 0.9));
  transition: transform 0.3s ease;
}

:deep(.fr24-plane-wrapper.selected .fr24-plane-icon) {
  font-size: 28px;
  filter: drop-shadow(0 0 16px #10b981);
}

:deep(.fr24-plane-label) {
  position: absolute;
  top: -10px;
  left: 22px;
  font-size: 9px;
  font-weight: 900;
  background: #0f172a;
  color: #34d399;
  border: 1px solid #10b981;
  padding: 1px 5px;
  border-radius: 4px;
  white-space: nowrap;
}

/* TELEMETRY SIDEBAR */
.telemetry-sidebar {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.no-departed-alert {
  padding: 24px;
  background: #fffbeb;
  border: 1px solid #fde68a;
  border-radius: 14px;
  text-align: center;
}

.no-departed-alert .alert-icon {
  font-size: 32px;
  margin-bottom: 8px;
}

.no-departed-alert h4 {
  margin: 0;
  font-size: 15px;
  font-weight: 800;
  color: #b45309;
}

.no-departed-alert p {
  margin: 8px 0 0 0;
  font-size: 12px;
  color: #78350f;
  line-height: 1.5;
}

.telemetry-card {
  padding: 24px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  box-shadow: 0 4px 14px rgba(15, 23, 42, 0.05);
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.card-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.plane-badge {
  display: flex;
  align-items: center;
  gap: 8px;
}

.badge-icon {
  font-size: 22px;
}

.flight-no {
  font-size: 20px;
  font-weight: 900;
  color: #0f172a;
}

.route-banner {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px;
  background: #f8fafc;
  border-radius: 10px;
  border: 1px solid #e2e8f0;
}

.hub-box {
  display: flex;
  flex-direction: column;
}

.hub-box.right {
  align-items: flex-end;
}

.hub-box .iata {
  font-size: 18px;
  font-weight: 900;
  color: #0f172a;
}

.hub-box .city {
  font-size: 11px;
  color: #64748b;
}

.vector-arrow {
  display: flex;
  flex-direction: column;
  align-items: center;
  font-weight: 800;
  color: #10b981;
}

.progress-txt {
  font-size: 11px;
}

.stats-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  padding: 10px;
  background: #f8fafc;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
}

.stat-label {
  font-size: 9px;
  font-weight: 800;
  color: #64748b;
  letter-spacing: 0.5px;
}

.stat-val {
  font-size: 13px;
  font-weight: 800;
  color: #0f172a;
  margin-top: 2px;
}

.stat-val.emerald { color: #059669; }
.stat-val.cyan { color: #0284c7; }

.empty-telemetry {
  padding: 30px;
  background: #ffffff;
  border: 1px dashed #cbd5e1;
  border-radius: 14px;
  text-align: center;
  color: #64748b;
}

.empty-icon {
  font-size: 36px;
  margin-bottom: 8px;
}

.active-list-card {
  padding: 18px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.list-title {
  font-size: 13px;
  font-weight: 800;
  color: #0f172a;
}

.flights-mini-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 200px;
  overflow-y: auto;
}

.mini-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.mini-item:hover,
.mini-item.selected {
  border-color: #10b981;
  background: #f0fdf4;
}

.mini-left {
  display: flex;
  flex-direction: column;
}

.mini-no {
  font-weight: 800;
  color: #0284c7;
  font-size: 13px;
}

.mini-route {
  font-size: 11px;
  color: #64748b;
}
</style>
