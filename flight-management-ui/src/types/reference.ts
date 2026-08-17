export interface AirportReference {
    airportId: number
    airportName: string
    airportCity: string
    airportCountry: string
    airportIataCode: string
    airportIcaoCode: string
    airportTimezone: string
    airportStatus: string
}

export interface AirportRequest {
    airportName: string
    airportCity: string
    airportCountry: string
    airportIataCode: string
    airportIcaoCode: string
    airportTimezone: string
}

export interface AirlineReference {
    airlineId: number
    airlineName: string
    airlineIataCode: string
    airlineIcaoCode?: string
    airlineCountry?: string
    airlineStatus?: string
}

export interface AirlineRequest {
    airlineName: string
    airlineIataCode: string
    airlineIcaoCode?: string
    airlineCountry?: string
}

export interface AircraftReference {
    aircraftId: number
    aircraftRegistrationNumber: string | null
    operatorAirlineId: number | null
    aircraftTypeId: number
    aircraftCapacity?: number
    aircraftManufactureYear?: number
    aircraftStatus?: string
}

export interface AircraftRequest {
    aircraftRegistrationNumber: string
    operatorAirlineId: number
    aircraftTypeId: number
    aircraftCapacity?: number
    aircraftManufactureYear?: number
}

export interface AircraftTypeReference {
    aircraftTypeId: number
    aircraftTypeManufacturer: string
    aircraftTypeModel: string
    aircraftTypeIcaoCode: string
}

export interface AircraftTypeRequest {
    aircraftTypeManufacturer: string
    aircraftTypeModel: string
    aircraftTypeIcaoCode: string
}

export interface FlightTypeReference {
    flightTypeId: number
    flightTypeName: string
    flightTypeCode: string
}

export interface FlightTypeRequest {
    flightTypeName: string
    flightTypeCode: string
}

export interface RouteReference {
    routeId: number
    originAirportId: number
    destinationAirportId: number
    routeStatus?: string
    distanceKm?: number
    estimatedDurationMinutes?: number
}

export interface RouteRequest {
    originAirportId: number
    destinationAirportId: number
    distanceKm?: number
    estimatedDurationMinutes?: number
}

