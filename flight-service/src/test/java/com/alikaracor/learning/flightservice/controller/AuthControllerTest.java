package com.alikaracor.learning.flightservice.controller;

import com.alikaracor.learning.flightservice.config.SecurityConfig;
import com.alikaracor.learning.flightservice.dto.AuthResponse;
import com.alikaracor.learning.flightservice.dto.LoginRequest;
import com.alikaracor.learning.flightservice.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private AuthService authService;

    @Test
    @DisplayName("POST /api/auth/login - Endpoint public olmalı, geçerli istek ile 200 OK ve AuthResponse dönmelidir")
    void login_shouldReturn200_whenRequestIsValidAndPublic() throws Exception {
        AuthResponse authResponse = new AuthResponse();
        authResponse.setAccessToken("sample-access-token");
        authResponse.setExpiresIn(3600L);

        when(authService.authenticate(any(LoginRequest.class), any())).thenReturn(authResponse);

        String loginJson = """
                {
                  "userName": "admin",
                  "userPassword": "Password123!"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("sample-access-token"))
                .andExpect(jsonPath("$.expiresIn").value(3600L));
    }

    @Test
    @DisplayName("POST /api/auth/login - Eksik veya geçersiz alanlar için 400 Bad Request dönmelidir")
    void login_shouldReturn400_whenRequestIsInvalid() throws Exception {
        String invalidJson = """
                {
                  "userName": "",
                  "userPassword": ""
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/auth/logout - Geçerli token ile 204 No Content dönmeli ve authService.logout çağrılmalıdır")
    void logout_shouldReturn204_andInvokeAuthService() throws Exception {
        java.time.Instant now = java.time.Instant.now();
        java.time.Instant expiresAt = now.plusSeconds(3600);

        mockMvc.perform(post("/api/auth/logout")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(builder -> builder.jti("test-jti-uuid").expiresAt(expiresAt))))
                .andExpect(status().isNoContent());

        org.mockito.Mockito.verify(authService).logout("test-jti-uuid", expiresAt);
    }
}
