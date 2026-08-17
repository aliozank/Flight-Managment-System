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
  let fetchPromise: Promise<void> | null = null

  const fetchAllReferences = async (force = false) => {
    if (initialized.value && !force) return
    if (fetchPromise) return fetchPromise

    const request = (async () => {
      loading.value = true
      try {
        const [
          airlinesResult,
          airportsResult,
          aircraftsResult,
          aircraftTypesResult,
          flightTypesResult,
          routesResult
        ] = await Promise.allSettled([
          referenceApi.get<AirlineReference[]>('/api/airlines'),
          referenceApi.get<AirportReference[]>('/api/airports'),
          referenceApi.get<AircraftReference[]>('/api/aircrafts'),
          referenceApi.get<AircraftTypeReference[]>('/api/aircraft-types'),
          referenceApi.get<FlightTypeReference[]>('/api/flight-types'),
          referenceApi.get<RouteReference[]>('/api/routes')
        ])

        airlines.value = airlinesResult.status === 'fulfilled' ? airlinesResult.value.data || [] : []
        airports.value = airportsResult.status === 'fulfilled' ? airportsResult.value.data || [] : []
        aircrafts.value = aircraftsResult.status === 'fulfilled' ? aircraftsResult.value.data || [] : []
        aircraftTypes.value = aircraftTypesResult.status === 'fulfilled' ? aircraftTypesResult.value.data || [] : []
        flightTypes.value = flightTypesResult.status === 'fulfilled' ? flightTypesResult.value.data || [] : []
        routes.value = routesResult.status === 'fulfilled' ? routesResult.value.data || [] : []

        initialized.value = [
          airlinesResult,
          airportsResult,
          aircraftsResult,
          aircraftTypesResult,
          flightTypesResult,
          routesResult
        ].every((result) => result.status === 'fulfilled')
      } finally {
        loading.value = false
      }
    })()

    fetchPromise = request
    try {
      await request
    } finally {
      if (fetchPromise === request) {
        fetchPromise = null
      }
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
