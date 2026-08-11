import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import referenceApi from '@/services/referenceApi'
import type {
  AirlineReference,
  AirportReference,
  AircraftReference,
  AircraftTypeReference,
  FlightTypeReference,
  RouteReference
} from '@/types/reference'

export const useReferenceStore = defineStore('reference', () => {
  const airlines = ref<AirlineReference[]>([])
  const airports = ref<AirportReference[]>([])
  const aircrafts = ref<AircraftReference[]>([])
  const aircraftTypes = ref<AircraftTypeReference[]>([])
  const flightTypes = ref<FlightTypeReference[]>([])
  const routes = ref<RouteReference[]>([])
  const loading = ref(false)
  const initialized = ref(false)

  const fetchAllReferences = async (force = false) => {
    if (initialized.value && !force) return
    loading.value = true
    try {
      const [
        airlinesRes,
        airportsRes,
        aircraftsRes,
        aircraftTypesRes,
        flightTypesRes,
        routesRes
      ] = await Promise.all([
        referenceApi.get<AirlineReference[]>('/api/airlines').catch(() => ({ data: [] })),
        referenceApi.get<AirportReference[]>('/api/airports').catch(() => ({ data: [] })),
        referenceApi.get<AircraftReference[]>('/api/aircrafts').catch(() => ({ data: [] })),
        referenceApi.get<AircraftTypeReference[]>('/api/aircraft-types').catch(() => ({ data: [] })),
        referenceApi.get<FlightTypeReference[]>('/api/flight-types').catch(() => ({ data: [] })),
        referenceApi.get<RouteReference[]>('/api/routes').catch(() => ({ data: [] }))
      ])

      airlines.value = airlinesRes.data || []
      airports.value = airportsRes.data || []
      aircrafts.value = aircraftsRes.data || []
      aircraftTypes.value = aircraftTypesRes.data || []
      flightTypes.value = flightTypesRes.data || []
      routes.value = routesRes.data || []
      initialized.value = true
    } finally {
      loading.value = false
    }
  }

  const getAirlineLabel = (id: number | null | undefined): string => {
    if (!id) return '-'
    const item = airlines.value.find((a) => a.airlineId === id)
    return item ? `${item.airlineName} (${item.airlineIataCode})` : `Airline #${id}`
  }

  const getAirportLabel = (id: number | null | undefined): string => {
    if (!id) return '-'
    const item = airports.value.find((a) => a.airportId === id)
    return item ? `${item.airportName} (${item.airportIataCode})` : `Airport #${id}`
  }

  const getAirportCode = (id: number | null | undefined): string => {
    if (!id) return '-'
    const item = airports.value.find((a) => a.airportId === id)
    return item ? item.airportIataCode : `#${id}`
  }

  const getAircraftLabel = (id: number | null | undefined): string => {
    if (id === null || id === undefined) return 'Atanmamış'
    const item = aircrafts.value.find((a) => a.aircraftId === id)
    return item ? (item.aircraftRegistrationNumber || `Aircraft #${item.aircraftId}`) : `Aircraft #${id}`
  }

  const getAircraftTypeLabel = (id: number | null | undefined): string => {
    if (!id) return '-'
    const item = aircraftTypes.value.find((a) => a.aircraftTypeId === id)
    return item ? `${item.aircraftTypeManufacturer} ${item.aircraftTypeModel}` : `Type #${id}`
  }

  const getFlightTypeLabel = (id: number | null | undefined): string => {
    if (!id) return '-'
    const item = flightTypes.value.find((f) => f.flightTypeId === id)
    if (!item) return `Type #${id}`
    const code = (item.flightTypeCode || item.flightTypeName || '').toUpperCase()
    if (code.includes('PASSENGER')) return 'Passenger'
    if (code.includes('CARGO')) return 'Cargo'
    if (code.includes('POSITION')) return 'Position'
    return item.flightTypeName || item.flightTypeCode
  }

  const findAircraftById = (id: number | null | undefined): AircraftReference | undefined => {
    if (!id) return undefined
    return aircrafts.value.find((a) => a.aircraftId === id)
  }

  return {
    airlines,
    airports,
    aircrafts,
    aircraftTypes,
    flightTypes,
    routes,
    loading,
    initialized,
    fetchAllReferences,
    getAirlineLabel,
    getAirportLabel,
    getAirportCode,
    getAircraftLabel,
    getAircraftTypeLabel,
    getFlightTypeLabel,
    findAircraftById
  }
})
