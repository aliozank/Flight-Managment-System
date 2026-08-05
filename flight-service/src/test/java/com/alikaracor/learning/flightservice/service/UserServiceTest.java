package com.alikaracor.learning.flightservice.service;

import com.alikaracor.learning.flightservice.dto.RegisterRequest;
import com.alikaracor.learning.flightservice.dto.UserResponse;
import com.alikaracor.learning.flightservice.dto.UserUpdateRequest;
import com.alikaracor.learning.flightservice.mapper.UserMapper;
import com.alikaracor.learning.flightservice.model.Role;
import com.alikaracor.learning.flightservice.model.RoleName;
import com.alikaracor.learning.flightservice.model.User;
import com.alikaracor.learning.flightservice.model.UserStatus;
import com.alikaracor.learning.flightservice.repository.RoleRepository;
import com.alikaracor.learning.flightservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserMapper userMapper;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private ActivityLogService activityLogService;

    @InjectMocks
    private UserService userService;

    private RegisterRequest registerRequest;
    private UserUpdateRequest updateRequest;
    private User sampleUser;
    private Role adminRole;
    private UserResponse sampleUserResponse;
    private Long performedByUserId;
    private String clientIpAddress;

    @BeforeEach
    void setUp() {
        performedByUserId = 100L;
        clientIpAddress = "127.0.0.1";

        adminRole = new Role();
        adminRole.setRoleId(1L);
        adminRole.setRoleName(RoleName.ADMIN);

        registerRequest = new RegisterRequest();
        registerRequest.setUserName("johndoe");
        registerRequest.setUserEmail("john@example.com");
        registerRequest.setUserPassword("pass1234");
        registerRequest.setUserRoleNames(Set.of(RoleName.ADMIN));

        updateRequest = new UserUpdateRequest();
        updateRequest.setUserRoleNames(Set.of(RoleName.ADMIN));
        updateRequest.setUserStatus(UserStatus.ACTIVE);

        sampleUser = new User();
        sampleUser.setUserId(2L);
        sampleUser.setUserName("johndoe");
        sampleUser.setUserEmail("john@example.com");
        sampleUser.setUserStatus(UserStatus.ACTIVE);

        sampleUserResponse = new UserResponse();
        sampleUserResponse.setUserId(2L);
        sampleUserResponse.setUserName("johndoe");
    }

    // ==================== registerUser Tests ====================

    @Test
    @DisplayName("registerUser - İstek geçerli olduğunda kullanıcı kaydedilmeli ve log tutulmalıdır")
    void registerUser_shouldRegisterUser_whenRequestIsValid() {
        when(userRepository.existsByUserNameIgnoreCase("johndoe")).thenReturn(false);
        when(userRepository.existsByUserEmailIgnoreCase("john@example.com")).thenReturn(false);
        when(userMapper.toUser(registerRequest)).thenReturn(sampleUser);
        when(passwordEncoder.encode("pass1234")).thenReturn("encoded-pass");
        when(roleRepository.findByRoleName(RoleName.ADMIN)).thenReturn(Optional.of(adminRole));
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(userMapper.toUserResponse(sampleUser)).thenReturn(sampleUserResponse);

        UserResponse response = userService.registerUser(registerRequest, performedByUserId, clientIpAddress);

        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo(2L);

        verify(activityLogService).logUserCreated(performedByUserId, 2L, clientIpAddress);
    }

    @Test
    @DisplayName("registerUser - Kullanıcı adı zaten varsa 409 CONFLICT fırlatılmalı ve loglanmalıdır")
    void registerUser_shouldThrowConflict_whenUsernameAlreadyExists() {
        when(userRepository.existsByUserNameIgnoreCase("johndoe")).thenReturn(true);

        assertThatThrownBy(() -> userService.registerUser(registerRequest, performedByUserId, clientIpAddress))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(activityLogService).logUserCreateFailure(performedByUserId, "Username is already in use", clientIpAddress);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("registerUser - E-posta zaten varsa 409 CONFLICT fırlatılmalı ve loglanmalıdır")
    void registerUser_shouldThrowConflict_whenEmailAlreadyExists() {
        when(userRepository.existsByUserNameIgnoreCase("johndoe")).thenReturn(false);
        when(userRepository.existsByUserEmailIgnoreCase("john@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.registerUser(registerRequest, performedByUserId, clientIpAddress))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(activityLogService).logUserCreateFailure(performedByUserId, "Email is already in use", clientIpAddress);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("registerUser - Rol veritabanında yoksa 404 NOT_FOUND fırlatılmalı ve loglanmalıdır")
    void registerUser_shouldThrowNotFound_whenRoleDoesNotExist() {
        when(userRepository.existsByUserNameIgnoreCase("johndoe")).thenReturn(false);
        when(userRepository.existsByUserEmailIgnoreCase("john@example.com")).thenReturn(false);
        when(userMapper.toUser(registerRequest)).thenReturn(sampleUser);
        when(passwordEncoder.encode("pass1234")).thenReturn("encoded-pass");
        when(roleRepository.findByRoleName(RoleName.ADMIN)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.registerUser(registerRequest, performedByUserId, clientIpAddress))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);

        verify(activityLogService).logUserCreateFailure(performedByUserId, "Rol bulunamadı: ADMIN", clientIpAddress);
    }

    // ==================== getUserById Tests ====================

    @Test
    @DisplayName("getUserById - Kullanıcı bulunduğunda UserResponse dönmelidir")
    void getUserById_shouldReturnUserResponse_whenUserExists() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(sampleUser));
        when(userMapper.toUserResponse(sampleUser)).thenReturn(sampleUserResponse);

        UserResponse response = userService.getUserById(2L);

        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("getUserById - Kullanıcı bulunamadığında 404 NOT_FOUND fırlatmalıdır")
    void getUserById_shouldThrowNotFound_whenUserDoesNotExist() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(99L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ==================== getAllUsers Tests ====================

    @Test
    @DisplayName("getAllUsers - Tüm kullanıcı listesini dönmelidir")
    void getAllUsers_shouldReturnUserResponseList() {
        when(userRepository.findAll()).thenReturn(List.of(sampleUser));
        when(userMapper.toUserResponse(sampleUser)).thenReturn(sampleUserResponse);

        List<UserResponse> list = userService.getAllUsers();

        assertThat(list).hasSize(1);
    }

    // ==================== updateUserById Tests ====================

    @Test
    @DisplayName("updateUserById - Geçerli güncelleme isteğinde kullanıcı güncellenmeli ve loglanmalıdır")
    void updateUserById_shouldUpdateUser_whenRequestIsValid() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(sampleUser));
        when(roleRepository.findByRoleName(RoleName.ADMIN)).thenReturn(Optional.of(adminRole));
        when(userRepository.save(sampleUser)).thenReturn(sampleUser);
        when(userMapper.toUserResponse(sampleUser)).thenReturn(sampleUserResponse);

        UserResponse response = userService.updateUserById(2L, updateRequest, performedByUserId, clientIpAddress);

        assertThat(response).isNotNull();
        verify(activityLogService).logUserUpdated(performedByUserId, 2L, clientIpAddress);
    }

    @Test
    @DisplayName("updateUserById - Var olmayan kullanıcı için 404 NOT_FOUND fırlatılmalı ve loglanmalıdır")
    void updateUserById_shouldThrowNotFound_whenUserDoesNotExist() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUserById(99L, updateRequest, performedByUserId, clientIpAddress))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);

        verify(activityLogService).logUserUpdateFailure(performedByUserId, 99L, "User not found", clientIpAddress);
    }

    @Test
    @DisplayName("updateUserById - Rol veritabanında yoksa 404 NOT_FOUND fırlatılmalı ve loglanmalıdır")
    void updateUserById_shouldThrowNotFound_whenRoleDoesNotExist() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(sampleUser));
        when(roleRepository.findByRoleName(RoleName.ADMIN)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUserById(2L, updateRequest, performedByUserId, clientIpAddress))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);

        verify(activityLogService).logUserUpdateFailure(performedByUserId, 2L, "Rol bulunamadı: ADMIN", clientIpAddress);
    }
}
