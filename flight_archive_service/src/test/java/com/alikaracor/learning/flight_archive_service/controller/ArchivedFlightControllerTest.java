package com.alikaracor.learning.flight_archive_service.controller;

import com.alikaracor.learning.flight_archive_service.config.SecurityConfig;
import com.alikaracor.learning.flight_archive_service.dto.ArchivedFlightResponse;
import com.alikaracor.learning.flight_archive_service.model.FlightStatus;
import com.alikaracor.learning.flight_archive_service.service.FlightArchiveService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ArchivedFlightController.class)
@Import(SecurityConfig.class)
class ArchivedFlightControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private FlightArchiveService flightArchiveService;

    private ArchivedFlightResponse sampleResponse;

    @BeforeEach
    void setUp() {
        sampleResponse = new ArchivedFlightResponse();
        sampleResponse.setArchiveId(1L);
        sampleResponse.setFlightId(100L);
        sampleResponse.setFlightNumber("TK1234");
        sampleResponse.setFlightStatus(FlightStatus.ARRIVED);
    }

    @Test
    @DisplayName("GET /api/archived-flights - Yetkili kullanıcı ile 200 OK ve liste dönmelidir")
    void getArchivedFlights_shouldReturn200_whenUserIsAuthorized() throws Exception {
        when(flightArchiveService.getAllArchivedFlights()).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/archived-flights")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].archiveId").value(1L))
                .andExpect(jsonPath("$[0].flightNumber").value("TK1234"));
    }

    @Test
    @DisplayName("GET /api/archived-flights/{archiveId} - Başarılı sorgulamada 200 OK dönmelidir")
    void getArchivedFlightByArchiveId_shouldReturn200_whenFound() throws Exception {
        when(flightArchiveService.getArchivedFlightByArchiveId(1L)).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/archived-flights/1")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_OPERATIONS"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archiveId").value(1L));
    }

    @Test
    @DisplayName("GET /api/archived-flight/{archiveId} - Bulunamayan kayıt için 404 NOT_FOUND dönmelidir")
    void getArchivedFlightByArchiveId_shouldReturn404_whenNotFound() throws Exception {
        when(flightArchiveService.getArchivedFlightByArchiveId(99L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Archived flight not found"));

        mockMvc.perform(get("/api/archived-flight/99")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/archived-flights/by-flight/{flightId} - FlightId ile sorgulamada 200 OK dönmelidir")
    void getArchivedFlightByFlightId_shouldReturn200_whenFound() throws Exception {
        when(flightArchiveService.getArchivedFlightByFlightId(100L)).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/archived-flights/by-flight/100")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_BI_ANALYST"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flightId").value(100L));
    }

    @Test
    @DisplayName("GET /api/archived-flights/by-flight/{flightId} - Bulunamadığında 404 NOT_FOUND dönmelidir")
    void getArchivedFlightByFlightId_shouldReturn404_whenNotFound() throws Exception {
        when(flightArchiveService.getArchivedFlightByFlightId(999L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Bu id ile bir kayıt bulunamadı"));

        mockMvc.perform(get("/api/archived-flights/by-flight/999")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isNotFound());
    }
}
