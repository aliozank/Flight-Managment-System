package com.alikaracor.learning.referencemanager.controller;

import com.alikaracor.learning.referencemanager.config.SecurityConfig;
import com.alikaracor.learning.referencemanager.dto.AirlineResponse;
import com.alikaracor.learning.referencemanager.service.AirlineService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AirlineController.class)
@Import(SecurityConfig.class)
class AirlineControllerSecurityTest {

    private static final String VALID_AIRLINE_JSON = """
            {
              "airlineName": "Test Airlines",
              "airlineIcaoCode": "TST",
              "airlineIataCode": "T1",
              "airlineCountry": "Türkiye",
              "airlineStatus": "ACTIVE"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AirlineService airlineService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void shouldReturnUnauthorizedWithoutToken() throws Exception {
        mockMvc.perform(get("/api/airlines"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAllowOperationsRoleToRead() throws Exception {
        when(airlineService.getAllAirlines()).thenReturn(List.of());

        mockMvc.perform(
                        get("/api/airlines")
                                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_OPERATIONS")))
                )
                .andExpect(status().isOk());
    }

    @Test
    void shouldAllowBiAnalystRoleToRead() throws Exception {
        when(airlineService.getAllAirlines()).thenReturn(List.of());

        mockMvc.perform(
                        get("/api/airlines")
                                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_BI_ANALYST")))
                )
                .andExpect(status().isOk());
    }

    @Test
    void shouldForbidOperationsRoleFromCreatingAirline() throws Exception {
        mockMvc.perform(
                        post("/api/airlines")
                                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_OPERATIONS")))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(VALID_AIRLINE_JSON)
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowAdminToCreateAirline() throws Exception {
        when(airlineService.addAirline(any())).thenReturn(new AirlineResponse());

        mockMvc.perform(
                        post("/api/airlines")
                                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(VALID_AIRLINE_JSON)
                )
                .andExpect(status().isCreated());
    }

    @Test
    void shouldRejectInvalidRequestBodyForAdmin() throws Exception {
        String invalidJson = """
                {
                  "airlineName": "",
                  "airlineIcaoCode": "th",
                  "airlineIataCode": "TOO_LONG",
                  "airlineCountry": "",
                  "airlineStatus": null
                }
                """;

        mockMvc.perform(
                        post("/api/airlines")
                                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalidJson)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldAllowAdminToDeactivateAirline() throws Exception {
        mockMvc.perform(
                        delete("/api/airlines/1")
                                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                )
                .andExpect(status().isNoContent());
    }
}
