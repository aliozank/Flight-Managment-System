package com.alikaracor.learning.flightservice.controller;

import com.alikaracor.learning.flightservice.config.SecurityConfig;
import com.alikaracor.learning.flightservice.dto.RegisterRequest;
import com.alikaracor.learning.flightservice.dto.UserResponse;
import com.alikaracor.learning.flightservice.dto.UserUpdateRequest;
import com.alikaracor.learning.flightservice.service.UserService;
import org.junit.jupiter.api.BeforeEach;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private UserService userService;

    private UserResponse sampleUserResponse;

    @BeforeEach
    void setUp() {
        sampleUserResponse = new UserResponse();
        sampleUserResponse.setUserId(2L);
        sampleUserResponse.setUserName("johndoe");
        sampleUserResponse.setUserEmail("john@example.com");
    }

    @Test
    @DisplayName("POST /api/users - ADMIN yetkisi ile 201 Created ve UserResponse dönmelidir")
    void createUser_shouldReturn201_whenUserIsAdmin() throws Exception {
        when(userService.registerUser(any(RegisterRequest.class), eq(100L), any()))
                .thenReturn(sampleUserResponse);

        String registerJson = """
                {
                  "userName": "johndoe",
                  "userEmail": "john@example.com",
                  "userPassword": "Password123!",
                  "userRoleNames": ["ADMIN"]
                }
                """;

        mockMvc.perform(post("/api/users")
                        .with(jwt().jwt(j -> j.subject("100")).authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(2L))
                .andExpect(jsonPath("$.userName").value("johndoe"));
    }

    @Test
    @DisplayName("POST /api/users - OPERATIONS yetkisi ile (Sadece ADMIN izinli) 403 Forbidden dönmelidir")
    void createUser_shouldReturn403_whenUserIsOperations() throws Exception {
        String registerJson = """
                {
                  "userName": "johndoe",
                  "userEmail": "john@example.com",
                  "userPassword": "Password123!",
                  "userRoleNames": ["ADMIN"]
                }
                """;

        mockMvc.perform(post("/api/users")
                        .with(jwt().jwt(j -> j.subject("100")).authorities(new SimpleGrantedAuthority("ROLE_OPERATIONS")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/users/{userId} - ADMIN yetkisi ile 200 OK dönmelidir")
    void getUserById_shouldReturn200_whenUserIsAdmin() throws Exception {
        when(userService.getUserById(2L)).thenReturn(sampleUserResponse);

        mockMvc.perform(get("/api/users/2")
                        .with(jwt().jwt(j -> j.subject("100")).authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(2L));
    }

    @Test
    @DisplayName("GET /api/users - ADMIN yetkisi ile 200 OK ve kullanıcı listesi dönmelidir")
    void getAllUsers_shouldReturn200_whenUserIsAdmin() throws Exception {
        when(userService.getAllUsers()).thenReturn(List.of(sampleUserResponse));

        mockMvc.perform(get("/api/users")
                        .with(jwt().jwt(j -> j.subject("100")).authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(2L));
    }

    @Test
    @DisplayName("PUT /api/users/{userId} - ADMIN yetkisi ile 200 OK ve güncellenmiş kullanıcı dönmelidir")
    void updateUser_shouldReturn200_whenUserIsAdmin() throws Exception {
        when(userService.updateUserById(eq(2L), any(UserUpdateRequest.class), eq(100L), any()))
                .thenReturn(sampleUserResponse);

        String updateJson = """
                {
                  "userRoleNames": ["ADMIN"],
                  "userStatus": "ACTIVE"
                }
                """;

        mockMvc.perform(put("/api/users/2")
                        .with(jwt().jwt(j -> j.subject("100")).authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(2L));
    }
}
