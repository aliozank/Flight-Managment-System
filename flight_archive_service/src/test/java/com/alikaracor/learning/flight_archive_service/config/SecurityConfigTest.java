package com.alikaracor.learning.flight_archive_service.config;

import com.alikaracor.learning.flight_archive_service.controller.ArchivedFlightController;
import com.alikaracor.learning.flight_archive_service.service.FlightArchiveService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ArchivedFlightController.class)
@Import(SecurityConfig.class)
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private FlightArchiveService flightArchiveService;

    @Test
    @DisplayName("GET /actuator/health - Tokensız erişilebilir olmalıdır (Security filtresi 401/403 fırlatmamalıdır)")
    void healthEndpoint_shouldBePublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isNotFound()); // Security permitAll geçişi onaylar (MockMvc'de actuator controller yüklü değil)
    }

    @Test
    @DisplayName("GET /api/archived-flights - Tokensız istek 401 Unauthorized almalıdır")
    void archiveEndpoint_shouldReturn401_whenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/archived-flights"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/archived-flights - ADMIN rolü erişebilmelidir")
    void archiveEndpoint_shouldAllowAdminRole() throws Exception {
        when(flightArchiveService.getAllArchivedFlights()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/archived-flights")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/archived-flights - OPERATIONS rolü erişebilmelidir")
    void archiveEndpoint_shouldAllowOperationsRole() throws Exception {
        when(flightArchiveService.getAllArchivedFlights()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/archived-flights")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_OPERATIONS"))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/archived-flights - BI_ANALYST rolü erişebilmelidir")
    void archiveEndpoint_shouldAllowBiAnalystRole() throws Exception {
        when(flightArchiveService.getAllArchivedFlights()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/archived-flights")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_BI_ANALYST"))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/archived-flights - DEVOPS veya izin verilmeyen rol 403 Forbidden almalıdır")
    void archiveEndpoint_shouldDenyUnauthorizedRole() throws Exception {
        mockMvc.perform(get("/api/archived-flights")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_DEVOPS"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /actuator/prometheus - Tokensız istek 401 Unauthorized almalıdır")
    void prometheusEndpoint_shouldReturn401_whenUnauthenticated() throws Exception {
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /actuator/prometheus - ADMIN veya DEVOPS rolü erişebilmelidir")
    void prometheusEndpoint_shouldAllowAdminOrDevopsRole() throws Exception {
        mockMvc.perform(get("/actuator/prometheus")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_DEVOPS"))))
                .andExpect(status().isNotFound()); // Actuator prometheus controller MockMvc'de yüklü değil, 401/403 olmadığından yetki geçişi doğrulanır
    }
}
