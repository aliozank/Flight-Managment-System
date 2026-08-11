export type FlightStatus = 'SCHEDULED' | 'DELAYED' | 'DEPARTED' | 'ARRIVED' | 'CANCELLED'

export interface FlightResponse {
    flightId: number
    flightNumber: string
    airlineId: number
    aircraftId: number | null
    aircraftTypeId: number
    originAirportId: number
    destinationAirportId: number
    flightTypeId: number
    flightDate: string
    scheduledDepartureTime: string
    scheduledArrivalTime: string
    flightStatus: FlightStatus
    flightVersion: number
    flightCreatedAt?: string
    flightUpdatedAt?: string
}

export interface FlightCreateRequest {
    flightNumber: string
    airlineId: number | null
    aircraftId?: number | null
    aircraftTypeId: number | null
    originAirportId: number | null
    destinationAirportId: number | null
    flightTypeId: number | null
    flightDate: string
    scheduledDepartureTime: string
    scheduledArrivalTime: string
}

export interface FlightUpdateRequest {
    flightNumber: string
    airlineId: number
    aircraftId?: number | null
    aircraftTypeId: number
    originAirportId: number
    destinationAirportId: number
    flightTypeId: number
    flightDate: string
    scheduledDepartureTime: string
    scheduledArrivalTime: string
    flightStatus: FlightStatus
}

export interface FlightCsvImportResponse {
    totalRowCount: number
    successfulRowCount: number
    failedRowCount: number
    successfulFlights: FlightResponse[]
    errors: string[]
}

export interface MockFlightGenerationRequest {
    flightCount: number
    maximumFutureDays: number
}

