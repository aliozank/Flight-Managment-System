package com.alikaracor.learning.referencemanager.controller;

import com.alikaracor.learning.referencemanager.config.SecurityConfig;
import com.alikaracor.learning.referencemanager.dto.AircraftResponse;
import com.alikaracor.learning.referencemanager.service.AircraftService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AircraftController.class)
@Import(SecurityConfig.class)
class AircraftControllerSecurityTest {

    private static final String VALID_AIRCRAFT_JSON = """
            {
              "aircraftRegistrationNumber": "TC-JAA",
              "operatorAirlineId": 1,
              "aircraftTypeId": 1,
              "aircraftCapacity": 180,
              "aircraftManufactureYear": 2020,
              "aircraftStatus": "ACTIVE"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AircraftService aircraftService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void shouldReturnUnauthorizedWithoutToken() throws Exception {
        mockMvc.perform(get("/api/aircrafts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldForbidOperationsRoleFromCreatingAircraft() throws Exception {
        mockMvc.perform(
                        post("/api/aircrafts")
                                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_OPERATIONS")))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(VALID_AIRCRAFT_JSON)
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowAdminToCreateAircraft() throws Exception {
        when(aircraftService.addAircraft(any())).thenReturn(new AircraftResponse());

        mockMvc.perform(
                        post("/api/aircrafts")
                                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(VALID_AIRCRAFT_JSON)
                )
                .andExpect(status().isCreated());
    }

    @Test
    void shouldRejectMissingRequiredField() throws Exception {
        String invalidJson = """
                {
                  "operatorAirlineId": 1,
                  "aircraftTypeId": 1,
                  "aircraftCapacity": 180,
                  "aircraftManufactureYear": 2020,
                  "aircraftStatus": "ACTIVE"
                }
                """; // missing registration number

        mockMvc.perform(
                        post("/api/aircrafts")
                                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalidJson)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectFutureManufactureYear() throws Exception {
        int futureYear = java.time.Year.now().getValue() + 1;
        String invalidJson = """
                {
                  "aircraftRegistrationNumber": "TC-JAA",
                  "operatorAirlineId": 1,
                  "aircraftTypeId": 1,
                  "aircraftCapacity": 180,
                  "aircraftManufactureYear": %d,
                  "aircraftStatus": "ACTIVE"
                }
                """.formatted(futureYear);

        when(aircraftService.addAircraft(any()))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "İleri tarihli üretim uçak yazılamaz"));

        mockMvc.perform(
                        post("/api/aircrafts")
                                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalidJson)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnNotFoundWhenAircraftIdDoesNotExist() throws Exception {
        when(aircraftService.getAircraftById(99L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Aircraft not found"));

        mockMvc.perform(
                        get("/api/aircrafts/99")
                                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_OPERATIONS")))
                )
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldAllowAdminToDeactivateAircraft() throws Exception {
        doNothing().when(aircraftService).deactiveAircraftById(1L);

        mockMvc.perform(
                        delete("/api/aircrafts/1")
                                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                )
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    void shouldAllowAdminToUpdateAircraft() throws Exception {
        when(aircraftService.updateAircraftById(any(), any())).thenReturn(new AircraftResponse());

        mockMvc.perform(
                        put("/api/aircrafts/1")
                                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(VALID_AIRCRAFT_JSON)
                )
                .andExpect(status().isOk());
    }
}
