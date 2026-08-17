package com.alikaracor.learning.flightservice.controller;

import com.alikaracor.learning.flightservice.config.SecurityConfig;
import com.alikaracor.learning.flightservice.dto.FlightCreateRequest;
import com.alikaracor.learning.flightservice.dto.FlightCsvImportResponse;
import com.alikaracor.learning.flightservice.dto.FlightResponse;
import com.alikaracor.learning.flightservice.dto.FlightStatusUpdateRequest;
import com.alikaracor.learning.flightservice.dto.FlightUpdateRequest;
import com.alikaracor.learning.flightservice.model.FlightStatus;
import com.alikaracor.learning.flightservice.service.FlightCsvImportService;
import com.alikaracor.learning.flightservice.service.FlightService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FlightController.class)
@Import(SecurityConfig.class)
class FlightControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private FlightService flightService;

    @MockitoBean
    private FlightCsvImportService flightCsvImportService;

    private FlightResponse sampleResponse;

    private String validCreateJson;
    private String invalidCreateJson;
    private String validUpdateJson;

    @BeforeEach
    void setUp() {
        sampleResponse = new FlightResponse();
        sampleResponse.setFlightId(1L);
        sampleResponse.setFlightNumber("TK1234");
        sampleResponse.setFlightStatus(FlightStatus.SCHEDULED);

        validCreateJson = """
                {
                  "flightNumber": "TK1234",
                  "airlineId": 10,
                  "aircraftTypeId": 20,
                  "originAirportId": 1,
                  "destinationAirportId": 2,
                  "flightTypeId": 5,
                  "flightDate": "2026-10-01",
                  "scheduledDepartureTime": "10:00:00",
                  "scheduledArrivalTime": "12:00:00",
                  "scheduledArrivalDate": "2026-10-01"
                }
                """;

        invalidCreateJson = """
                {
                  "flightNumber": "INVALID_FORMAT",
                  "airlineId": 10,
                  "aircraftTypeId": 20,
                  "originAirportId": 1,
                  "destinationAirportId": 2,
                  "flightTypeId": 5,
                  "flightDate": "2026-10-01",
                  "scheduledDepartureTime": "10:00:00",
                  "scheduledArrivalTime": "12:00:00",
                  "scheduledArrivalDate": "2026-10-01"
                }
                """;

        validUpdateJson = """
                {
                  "flightNumber": "TK1234",
                  "airlineId": 10,
                  "aircraftTypeId": 20,
                  "originAirportId": 1,
                  "destinationAirportId": 2,
                  "flightTypeId": 5,
                  "flightDate": "2026-10-01",
                  "scheduledDepartureTime": "11:00:00",
                  "scheduledArrivalTime": "13:00:00",
                  "scheduledArrivalDate": "2026-10-01"
                }
                """;
    }

    // ==================== GET /api/flights Tests ====================

    @Test
    @DisplayName("GET /api/flights - ADMIN yetkili kullanıcı 200 OK ve uçuş listesini almalıdır")
    void getFlights_shouldReturn200_whenUserIsAdmin() throws Exception {
        when(flightService.getAllFlights()).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/flights")
                        .with(jwt().jwt(j -> j.subject("100")).authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].flightId").value(1L))
                .andExpect(jsonPath("$[0].flightNumber").value("TK1234"));
    }

    @Test
    @DisplayName("GET /api/flights - Yetkisiz (Unauthenticated) istek 401 Unauthorized almalıdır")
    void getFlights_shouldReturn401_whenUserIsUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/flights"))
                .andExpect(status().isUnauthorized());
    }

    // ==================== GET /api/flights/{id} Tests ====================

    @Test
    @DisplayName("GET /api/flights/{id} - OPERATIONS yetkili kullanıcı 200 OK ve uçuş detayını almalıdır")
    void getFlightById_shouldReturn200_whenUserIsOperations() throws Exception {
        when(flightService.getFlightById(1L)).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/flights/1")
                        .with(jwt().jwt(j -> j.subject("100")).authorities(new SimpleGrantedAuthority("ROLE_OPERATIONS"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flightId").value(1L));
    }

    @Test
    @DisplayName("GET /api/flights/{id} - Yetkisiz istek 401 Unauthorized almalıdır")
    void getFlightById_shouldReturn401_whenUserIsUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/flights/1"))
                .andExpect(status().isUnauthorized());
    }

    // ==================== POST /api/flights Tests ====================

    @Test
    @DisplayName("POST /api/flights - Geçerli istek ve ADMIN yetkisi ile 201 Created dönmelidir")
    void createFlight_shouldReturn201_whenRequestIsValidAndUserIsAdmin() throws Exception {
        when(flightService.addFlight(any(FlightCreateRequest.class), eq(100L), any()))
                .thenReturn(sampleResponse);

        mockMvc.perform(post("/api/flights")
                        .with(jwt().jwt(j -> j.subject("100")).authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.flightId").value(1L));
    }

    @Test
    @DisplayName("POST /api/flights - Geçersiz gövde (Validation Hatası) ile 400 Bad Request dönmelidir")
    void createFlight_shouldReturn400_whenRequestBodyIsInvalid() throws Exception {
        mockMvc.perform(post("/api/flights")
                        .with(jwt().jwt(j -> j.subject("100")).authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidCreateJson))
                .andExpect(status().isBadRequest());
    }

    // ==================== PUT /api/flights/{id} Tests ====================

    @Test
    @DisplayName("PUT /api/flights/{id} - Geçerli istek ve OPERATIONS yetkisi ile 200 OK dönmelidir")
    void updateFlightById_shouldReturn200_whenRequestIsValidAndUserIsOperations() throws Exception {
        when(flightService.updateFlight(eq(1L), any(FlightUpdateRequest.class), eq(100L), any()))
                .thenReturn(sampleResponse);

        mockMvc.perform(put("/api/flights/1")
                        .with(jwt().jwt(j -> j.subject("100")).authorities(new SimpleGrantedAuthority("ROLE_OPERATIONS")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUpdateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flightId").value(1L));
    }

    // ==================== DELETE /api/flights/{id} Tests ====================

    @Test
    @DisplayName("DELETE /api/flights/{id} - ADMIN yetkisi ile 204 No Content dönmelidir")
    void cancelFlight_shouldReturn204_whenUserIsAdmin() throws Exception {
        mockMvc.perform(delete("/api/flights/1")
                        .with(jwt().jwt(j -> j.subject("100")).authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isNoContent());

        verify(flightService).cancelFlight(eq(1L), eq(100L), any());
    }

    @Test
    @DisplayName("DELETE /api/flights/{id} - OPERATIONS yetkisi ile (Sadece ADMIN izinli) 403 Forbidden dönmelidir")
    void cancelFlight_shouldReturn403_whenUserIsOperations() throws Exception {
        mockMvc.perform(delete("/api/flights/1")
                        .with(jwt().jwt(j -> j.subject("100")).authorities(new SimpleGrantedAuthority("ROLE_OPERATIONS"))))
                .andExpect(status().isForbidden());
    }

    // ==================== POST /api/flights/csv/upload Tests ====================

    @Test
    @DisplayName("POST /api/flights/csv/upload - CSV dosyası ve yetkili kullanıcı ile 200 OK dönmelidir")
    void uploadFlightCsv_shouldReturn200_whenUserIsAdmin() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", "content".getBytes());

        FlightCsvImportResponse importResponse = new FlightCsvImportResponse();
        importResponse.setTotalRowCount(1);
        importResponse.setSuccessfulRowCount(1);

        when(flightCsvImportService.importFlights(any(), eq(100L), any())).thenReturn(importResponse);

        mockMvc.perform(multipart("/api/flights/csv/upload")
                        .file(file)
                        .with(jwt().jwt(j -> j.subject("100")).authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRowCount").value(1));
    }

    // ==================== PATCH /api/flights/{id}/status Tests ====================

    @Test
    @DisplayName("PATCH /api/flights/{id}/status - OPERATIONS veya ADMIN yetkisi ve geçerli durum ile 200 OK dönmelidir")
    void updateFlightStatus_shouldReturn200_whenUserIsAuthorizedAndPayloadIsValid() throws Exception {
        when(flightService.updateFlightStatus(eq(1L), any(FlightStatusUpdateRequest.class), eq(100L), any()))
                .thenReturn(sampleResponse);

        mockMvc.perform(patch("/api/flights/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"flightStatus\": \"DELAYED\"}")
                        .with(jwt().jwt(j -> j.subject("100")).authorities(new SimpleGrantedAuthority("ROLE_OPERATIONS"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flightId").value(1L))
                .andExpect(jsonPath("$.flightNumber").value("TK1234"));

        verify(flightService).updateFlightStatus(eq(1L), any(FlightStatusUpdateRequest.class), eq(100L), any());
    }

    @Test
    @DisplayName("PATCH /api/flights/{id}/status - Yetkisiz rol (örn. BI_ANALYST) 403 Forbidden almalıdır")
    void updateFlightStatus_shouldReturn403_whenUserIsUnauthorized() throws Exception {
        mockMvc.perform(patch("/api/flights/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"flightStatus\": \"DELAYED\"}")
                        .with(jwt().jwt(j -> j.subject("100")).authorities(new SimpleGrantedAuthority("ROLE_BI_ANALYST"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PATCH /api/flights/{id}/status - Boş veya geçersiz JSON gövdesinde 400 Bad Request dönmelidir")
    void updateFlightStatus_shouldReturn400_whenPayloadIsInvalid() throws Exception {
        mockMvc.perform(patch("/api/flights/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .with(jwt().jwt(j -> j.subject("100")).authorities(new SimpleGrantedAuthority("ROLE_OPERATIONS"))))
                .andExpect(status().isBadRequest());
    }
}
