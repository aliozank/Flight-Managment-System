import type { FlightStatus } from './flight'

export interface ArchivedFlightResponse {
  archiveId: number
  eventId: string
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
  changedByUserId: number
  eventOccurredAt: string
  archivedAt: string
}
