package com.alikaracor.learning.referencemanager.dto;

import com.alikaracor.learning.referencemanager.model.AircraftCategory;
import com.alikaracor.learning.referencemanager.model.AircraftStatus;
import com.alikaracor.learning.referencemanager.model.AircraftTypeStatus;
import com.alikaracor.learning.referencemanager.model.AirlineStatus;
import com.alikaracor.learning.referencemanager.model.AirportStatus;
import com.alikaracor.learning.referencemanager.model.FlightTypeStatus;
import com.alikaracor.learning.referencemanager.model.RouteStatus;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void shouldAcceptValidAirlineRequest() {
        assertTrue(validator.validate(validAirlineRequest()).isEmpty());
    }

    @Test
    void shouldRejectLowercaseAirlineIcaoCode() {
        AirlineRequest request = validAirlineRequest();
        request.setAirlineIcaoCode("thy");

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void shouldRejectAirlineIataCodeLongerThanTwoCharacters() {
        AirlineRequest request = validAirlineRequest();
        request.setAirlineIataCode("THY");

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void shouldAcceptValidAirportRequest() {
        assertTrue(validator.validate(validAirportRequest()).isEmpty());
    }

    @Test
    void shouldRejectInvalidAirportCodes() {
        AirportRequest request = validAirportRequest();
        request.setAirportIataCode("IS");
        request.setAirportIcaoCode("ltfm");

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void shouldAcceptValidAircraftTypeRequest() {
        AircraftTypeRequest request = new AircraftTypeRequest();
        request.setAircraftTypeManufacturer("Boeing");
        request.setAircraftTypeModel("737-800");
        request.setAircraftTypeIcaoCode("B738");
        request.setAircraftTypeCategory(AircraftCategory.NARROW_BODY);
        request.setAircraftTypeStatus(AircraftTypeStatus.ACTIVE);

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void shouldRejectInvalidAircraftTypeIcaoCode() {
        AircraftTypeRequest request = new AircraftTypeRequest();
        request.setAircraftTypeManufacturer("Boeing");
        request.setAircraftTypeModel("737-800");
        request.setAircraftTypeIcaoCode("B-738");
        request.setAircraftTypeCategory(AircraftCategory.NARROW_BODY);
        request.setAircraftTypeStatus(AircraftTypeStatus.ACTIVE);

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void shouldAcceptValidAircraftRequest() {
        assertTrue(validator.validate(validAircraftRequest()).isEmpty());
    }

    @Test
    void shouldRejectInvalidAircraftRegistrationAndNegativeCapacity() {
        AircraftRequest request = validAircraftRequest();
        request.setAircraftRegistrationNumber("tc_jaa");
        request.setAircraftCapacity(-1);

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void shouldAcceptValidRouteRequest() {
        RouteRequest request = new RouteRequest();
        request.setOriginAirportId(1L);
        request.setDestinationAirportId(2L);
        request.setRouteStatus(RouteStatus.ACTIVE);

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void shouldRejectNonPositiveRouteIds() {
        RouteRequest request = new RouteRequest();
        request.setOriginAirportId(0L);
        request.setDestinationAirportId(-1L);
        request.setRouteStatus(RouteStatus.ACTIVE);

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void shouldAcceptValidFlightTypeRequest() {
        assertTrue(validator.validate(validFlightTypeRequest()).isEmpty());
    }

    @Test
    void shouldRejectFlightTypeCodeWithSpaces() {
        FlightTypeRequest request = validFlightTypeRequest();
        request.setFlightTypeCode("SPECIAL FLIGHT");

        assertFalse(validator.validate(request).isEmpty());
    }

    private AirlineRequest validAirlineRequest() {
        AirlineRequest request = new AirlineRequest();
        request.setAirlineName("Turkish Airlines");
        request.setAirlineIataCode("TK");
        request.setAirlineIcaoCode("THY");
        request.setAirlineCountry("Türkiye");
        request.setAirlineStatus(AirlineStatus.ACTIVE);
        return request;
    }

    private AirportRequest validAirportRequest() {
        AirportRequest request = new AirportRequest();
        request.setAirportName("Istanbul Airport");
        request.setAirportCity("Istanbul");
        request.setAirportCountry("Türkiye");
        request.setAirportIataCode("IST");
        request.setAirportIcaoCode("LTFM");
        request.setAirportTimezone("Europe/Istanbul");
        request.setAirportStatus(AirportStatus.OPERATIONAL);
        return request;
    }

    private AircraftRequest validAircraftRequest() {
        AircraftRequest request = new AircraftRequest();
        request.setAircraftRegistrationNumber("TC-JAA");
        request.setOperatorAirlineId(null);
        request.setAircraftTypeId(1L);
        request.setAircraftCapacity(180);
        request.setAircraftManufactureYear(2020);
        request.setAircraftStatus(AircraftStatus.ACTIVE);
        return request;
    }

    private FlightTypeRequest validFlightTypeRequest() {
        FlightTypeRequest request = new FlightTypeRequest();
        request.setFlightTypeName("Special Flight");
        request.setFlightTypeCode("SPECIAL_FLIGHT");
        request.setFlightTypeStatus(FlightTypeStatus.ACTIVE);
        return request;
    }
}
