package com.alikaracor.learning.referencemanager.repository;

import com.alikaracor.learning.referencemanager.model.Aircraft;
import com.alikaracor.learning.referencemanager.model.AircraftCategory;
import com.alikaracor.learning.referencemanager.model.AircraftStatus;
import com.alikaracor.learning.referencemanager.model.AircraftType;
import com.alikaracor.learning.referencemanager.model.AircraftTypeStatus;
import com.alikaracor.learning.referencemanager.model.Airline;
import com.alikaracor.learning.referencemanager.model.AirlineStatus;
import com.alikaracor.learning.referencemanager.model.Airport;
import com.alikaracor.learning.referencemanager.model.AirportStatus;
import com.alikaracor.learning.referencemanager.model.FlightTypeStatus;
import com.alikaracor.learning.referencemanager.model.Route;
import com.alikaracor.learning.referencemanager.model.RouteStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ReferenceRepositoryIntegrationTest {

    @Container
    @ServiceConnection
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4");

    @Autowired
    private AirlineRepository airlineRepository;

    @Autowired
    private AirportRepository airportRepository;

    @Autowired
    private AircraftTypeRepository aircraftTypeRepository;

    @Autowired
    private AircraftRepository aircraftRepository;

    @Autowired
    private RouteRepository routeRepository;

    @Autowired
    private FlightTypeRepository flightTypeRepository;

    @Test
    void shouldRunLiquibaseAndLoadDefaultFlightTypes() {
        assertTrue(flightTypeRepository.existsByFlightTypeCodeIgnoreCase("PASSENGER"));
        assertTrue(flightTypeRepository.existsByFlightTypeCodeIgnoreCase("CARGO"));
        assertTrue(flightTypeRepository.existsByFlightTypeCodeIgnoreCase("POSITION"));
        assertEquals(
                3,
                flightTypeRepository.findAllByFlightTypeStatusOrderByFlightTypeNameAsc(
                        FlightTypeStatus.ACTIVE
                ).size()
        );
    }

    @Test
    void shouldGenerateAirlineIdAndTimestamps() {
        Airline saved = airlineRepository.saveAndFlush(airline("Test Airline", "T1", "TST"));

        assertNotNull(saved.getAirlineId());
        assertNotNull(saved.getAirlineCreatedAt());
        assertNotNull(saved.getAirlineUpdatedAt());
    }

    @Test
    void shouldEnforceUniqueAirlineIataCode() {
        airlineRepository.saveAndFlush(airline("First Airline", "U1", "UA1"));

        assertThrows(
                DataIntegrityViolationException.class,
                () -> airlineRepository.saveAndFlush(airline("Second Airline", "U1", "UA2"))
        );
    }

    @Test
    void shouldFindAirportCodesIgnoringCase() {
        Airport saved = airportRepository.saveAndFlush(
                airport("Test Airport", "TST", "LTST")
        );

        assertEquals(
                saved.getAirportId(),
                airportRepository.findByAirportIataCodeIgnoreCase("tst").orElseThrow().getAirportId()
        );
        assertEquals(
                saved.getAirportId(),
                airportRepository.findByAirportIcaoCodeIgnoreCase("ltst").orElseThrow().getAirportId()
        );
    }

    @Test
    void shouldAllowAircraftWithoutOperatorAirline() {
        AircraftType type = aircraftTypeRepository.saveAndFlush(aircraftType("TST1"));
        Aircraft aircraft = new Aircraft();
        aircraft.setAircraftRegistrationNumber("TC-TST");
        aircraft.setAircraftType(type);
        aircraft.setOperatorAirline(null);
        aircraft.setAircraftCapacity(100);
        aircraft.setAircraftManufactureYear(2020);
        aircraft.setAircraftStatus(AircraftStatus.ACTIVE);

        Aircraft saved = aircraftRepository.saveAndFlush(aircraft);

        assertNotNull(saved.getAircraftId());
        assertNull(saved.getOperatorAirline());
        assertEquals(type.getAircraftTypeId(), saved.getAircraftType().getAircraftTypeId());
    }

    @Test
    void shouldRequireAircraftTypeForeignKey() {
        Aircraft aircraft = new Aircraft();
        aircraft.setAircraftRegistrationNumber("TC-NOTYPE");
        aircraft.setAircraftCapacity(100);
        aircraft.setAircraftManufactureYear(2020);
        aircraft.setAircraftStatus(AircraftStatus.ACTIVE);

        assertThrows(
                DataIntegrityViolationException.class,
                () -> aircraftRepository.saveAndFlush(aircraft)
        );
    }

    @Test
    void shouldEnforceUniqueOriginAndDestinationRoute() {
        Airport origin = airportRepository.saveAndFlush(airport("Origin", "ORG", "LTOR"));
        Airport destination = airportRepository.saveAndFlush(airport("Destination", "DST", "LTDS"));
        routeRepository.saveAndFlush(route(origin, destination));

        assertThrows(
                DataIntegrityViolationException.class,
                () -> routeRepository.saveAndFlush(route(origin, destination))
        );
    }

    private Airline airline(String name, String iata, String icao) {
        Airline airline = new Airline();
        airline.setAirlineName(name);
        airline.setAirlineIataCode(iata);
        airline.setAirlineIcaoCode(icao);
        airline.setAirlineCountry("Türkiye");
        airline.setAirlineStatus(AirlineStatus.ACTIVE);
        return airline;
    }

    private Airport airport(String name, String iata, String icao) {
        Airport airport = new Airport();
        airport.setAirportName(name);
        airport.setAirportCity("Istanbul");
        airport.setAirportCountry("Türkiye");
        airport.setAirportIataCode(iata);
        airport.setAirportIcaoCode(icao);
        airport.setAirportTimezone("Europe/Istanbul");
        airport.setAirportStatus(AirportStatus.OPERATIONAL);
        return airport;
    }

    private AircraftType aircraftType(String icaoCode) {
        AircraftType aircraftType = new AircraftType();
        aircraftType.setAircraftTypeManufacturer("Test Manufacturer");
        aircraftType.setAircraftTypeModel("Test Model");
        aircraftType.setAircraftTypeIcaoCode(icaoCode);
        aircraftType.setAircraftTypeCategory(AircraftCategory.NARROW_BODY);
        aircraftType.setAircraftTypeStatus(AircraftTypeStatus.ACTIVE);
        return aircraftType;
    }

    private Route route(Airport origin, Airport destination) {
        Route route = new Route();
        route.setOriginAirport(origin);
        route.setDestinationAirport(destination);
        route.setRouteStatus(RouteStatus.ACTIVE);
        return route;
    }
}
