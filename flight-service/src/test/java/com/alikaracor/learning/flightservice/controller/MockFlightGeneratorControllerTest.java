package com.alikaracor.learning.flightservice.controller;

import com.alikaracor.learning.flightservice.config.SecurityConfig;
import com.alikaracor.learning.flightservice.dto.FlightResponse;
import com.alikaracor.learning.flightservice.dto.MockFlightGenerationRequest;
import com.alikaracor.learning.flightservice.service.MockFlightGeneratorService;
import org.junit.jupiter.api.DisplayName;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MockFlightGeneratorController.class)
@Import(SecurityConfig.class)
class MockFlightGeneratorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private MockFlightGeneratorService mockFlightGeneratorService;

    @Test
    @DisplayName("POST /api/flights/mock - ADMIN yetkisi ile 201 Created dönmeli ve kullanıcı ID ile IP aktarılmalıdır")
    void generateFlights_shouldReturn201_whenUserIsAdminAndRequestIsValid() throws Exception {
        FlightResponse response = new FlightResponse();
        response.setFlightId(1L);
        response.setFlightNumber("TK1001");

        when(mockFlightGeneratorService.generateFlights(any(MockFlightGenerationRequest.class), eq(100L), any()))
                .thenReturn(List.of(response));

        String validJson = """
                {
                  "flightCount": 5,
                  "maximumFutureDays": 30
                }
                """;

        mockMvc.perform(post("/api/flights/mock")
                        .with(jwt().jwt(j -> j.subject("100")).authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].flightId").value(1L))
                .andExpect(jsonPath("$[0].flightNumber").value("TK1001"));
    }

    @Test
    @DisplayName("POST /api/flights/mock - Validasyon hatasında 400 Bad Request dönmelidir")
    void generateFlights_shouldReturn400_whenRequestIsInvalid() throws Exception {
        String invalidJson = """
                {
                  "flightCount": 0,
                  "maximumFutureDays": -1
                }
                """;

        mockMvc.perform(post("/api/flights/mock")
                        .with(jwt().jwt(j -> j.subject("100")).authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/flights/mock - Yetkisiz istek 401 Unauthorized almalıdır")
    void generateFlights_shouldReturn401_whenUnauthenticated() throws Exception {
        String validJson = """
                {
                  "flightCount": 5,
                  "maximumFutureDays": 30
                }
                """;

        mockMvc.perform(post("/api/flights/mock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson))
                .andExpect(status().isUnauthorized());
    }
}
