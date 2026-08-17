import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import flightApi from '@/services/api'
import type {
  FlightResponse,
  FlightCreateRequest,
  FlightUpdateRequest,
  FlightStatus,
  FlightStatusUpdateRequest,
  FlightCsvImportResponse,
  MockFlightGenerationRequest,
  MockFlightGenerationResponse
} from '@/types/flight'

export const useFlightStore = defineStore('flight', () => {
  const flights = ref<FlightResponse[]>([])
  const loading = ref(false)
  const searchQuery = ref('')
  const selectedAirlineId = ref<number | null>(null)
  const selectedStatus = ref<string | null>(null)
  const selectedOriginId = ref<number | null>(null)
  const selectedDestinationId = ref<number | null>(null)
  const selectedDate = ref<string | null>(null)

  const currentPage = ref(1)
  const pageSize = ref(10)

  const fetchFlights = async (): Promise<void> => {
    loading.value = true
    try {
      const response = await flightApi.get<FlightResponse[]>('/api/flights')
      flights.value = response.data || []
    } finally {
      loading.value = false
    }
  }

  const filteredFlights = computed(() => {
    return flights.value.filter((flight) => {
      if (searchQuery.value) {
        const query = searchQuery.value.toLowerCase()
        if (!flight.flightNumber.toLowerCase().includes(query)) {
          return false
        }
      }
      if (selectedAirlineId.value !== null && flight.airlineId !== selectedAirlineId.value) {
        return false
      }
      if (selectedStatus.value && flight.flightStatus !== selectedStatus.value) {
        return false
      }
      if (selectedOriginId.value !== null && flight.originAirportId !== selectedOriginId.value) {
        return false
      }
      if (selectedDestinationId.value !== null && flight.destinationAirportId !== selectedDestinationId.value) {
        return false
      }
      if (selectedDate.value && flight.flightDate !== selectedDate.value) {
        return false
      }
      return true
    })
  })

  const totalFilteredCount = computed(() => filteredFlights.value.length)

  const paginatedFlights = computed(() => {
    const start = (currentPage.value - 1) * pageSize.value
    return filteredFlights.value.slice(start, start + pageSize.value)
  })

  const resetFilters = (): void => {
    searchQuery.value = ''
    selectedAirlineId.value = null
    selectedStatus.value = null
    selectedOriginId.value = null
    selectedDestinationId.value = null
    selectedDate.value = null
    currentPage.value = 1
  }

  const createFlight = async (payload: FlightCreateRequest): Promise<FlightResponse> => {
    const response = await flightApi.post<FlightResponse>('/api/flights', payload)
    await fetchFlights()
    return response.data
  }

  const updateFlight = async (flightId: number, payload: FlightUpdateRequest): Promise<FlightResponse> => {
    const response = await flightApi.put<FlightResponse>(`/api/flights/${flightId}`, payload)
    await fetchFlights()
    return response.data
  }

  const updateFlightStatus = async (flightId: number, flightStatus: FlightStatus): Promise<FlightResponse> => {
    const payload: FlightStatusUpdateRequest = { flightStatus }
    const response = await flightApi.patch<FlightResponse>(`/api/flights/${flightId}/status`, payload)
    await fetchFlights()
    return response.data
  }

  const cancelFlight = async (flightId: number): Promise<void> => {
    await flightApi.delete(`/api/flights/${flightId}`)
    await fetchFlights()
  }

  const uploadCsv = async (file: File): Promise<FlightCsvImportResponse> => {
    const formData = new FormData()
    formData.append('file', file)
    const response = await flightApi.post<FlightCsvImportResponse>('/api/flights/csv/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    await fetchFlights()
    return response.data
  }

  const generateMockFlights = async (request: MockFlightGenerationRequest): Promise<MockFlightGenerationResponse> => {
    const response = await flightApi.post<MockFlightGenerationResponse>('/api/flights/mock', request)
    await fetchFlights()
    return response.data
  }

  const upsertFlightFromWebSocket = (updatedFlight: FlightResponse): boolean => {
    const index = flights.value.findIndex((f) => f.flightId === updatedFlight.flightId)
    if (index !== -1) {
      const currentFlight = flights.value[index]

      if (currentFlight && updatedFlight.flightVersion <= currentFlight.flightVersion) {
        return false
      }

      flights.value[index] = updatedFlight
    } else {
      flights.value.unshift(updatedFlight)
    }

    return true
  }

  return {
    flights,
    loading,
    searchQuery,
    selectedAirlineId,
    selectedStatus,
    selectedOriginId,
    selectedDestinationId,
    selectedDate,
    currentPage,
    pageSize,
    filteredFlights,
    totalFilteredCount,
    paginatedFlights,
    resetFilters,
    fetchFlights,
    createFlight,
    updateFlight,
    updateFlightStatus,
    cancelFlight,
    uploadCsv,
    generateMockFlights,
    upsertFlightFromWebSocket
  }
})
