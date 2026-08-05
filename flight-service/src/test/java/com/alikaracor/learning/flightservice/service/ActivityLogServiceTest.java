package com.alikaracor.learning.flightservice.service;

import com.alikaracor.learning.flightservice.model.ActivityAction;
import com.alikaracor.learning.flightservice.model.ActivityLog;
import com.alikaracor.learning.flightservice.model.ActivityResourceType;
import com.alikaracor.learning.flightservice.repository.ActivityLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ActivityLogServiceTest {

    @Mock
    private ActivityLogRepository activityLogRepository;

    @InjectMocks
    private ActivityLogService activityLogService;

    private String ipAddress;

    @BeforeEach
    void setUp() {
        ipAddress = "192.168.1.100";
    }

    @Test
    @DisplayName("logLoginSuccess - Başarılı giriş logunu doğru alanlarla kaydetmelidir")
    void logLoginSuccess_shouldSaveActivityLogWithCorrectFields() {
        activityLogService.logLoginSuccess(1L, ipAddress);

        ArgumentCaptor<ActivityLog> captor = ArgumentCaptor.forClass(ActivityLog.class);
        verify(activityLogRepository).save(captor.capture());

        ActivityLog log = captor.getValue();
        assertThat(log.getActorUserId()).isEqualTo(1L);
        assertThat(log.getActivityAction()).isEqualTo(ActivityAction.LOGIN_SUCCESS);
        assertThat(log.getActivityResourceType()).isEqualTo(ActivityResourceType.AUTH);
        assertThat(log.getResourceId()).isEqualTo(1L);
        assertThat(log.isSuccess()).isTrue();
        assertThat(log.getFailureReason()).isNull();
        assertThat(log.getIpAddress()).isEqualTo(ipAddress);
    }

    @Test
    @DisplayName("logLoginFailure - Başarısız giriş logunu doğru hata gerekçesiyle kaydetmelidir")
    void logLoginFailure_shouldSaveActivityLogWithFailureReason() {
        activityLogService.logLoginFailure(2L, "Invalid password", ipAddress);

        ArgumentCaptor<ActivityLog> captor = ArgumentCaptor.forClass(ActivityLog.class);
        verify(activityLogRepository).save(captor.capture());

        ActivityLog log = captor.getValue();
        assertThat(log.getActorUserId()).isNull();
        assertThat(log.getActivityAction()).isEqualTo(ActivityAction.LOGIN_FAILED);
        assertThat(log.getActivityResourceType()).isEqualTo(ActivityResourceType.AUTH);
        assertThat(log.getResourceId()).isEqualTo(2L);
        assertThat(log.isSuccess()).isFalse();
        assertThat(log.getFailureReason()).isEqualTo("Invalid password");
        assertThat(log.getIpAddress()).isEqualTo(ipAddress);
    }

    @Test
    @DisplayName("logUserCreated - Kullanıcı oluşturma logunu kaydetmelidir")
    void logUserCreated_shouldSaveActivityLog() {
        activityLogService.logUserCreated(10L, 20L, ipAddress);

        ArgumentCaptor<ActivityLog> captor = ArgumentCaptor.forClass(ActivityLog.class);
        verify(activityLogRepository).save(captor.capture());

        ActivityLog log = captor.getValue();
        assertThat(log.getActorUserId()).isEqualTo(10L);
        assertThat(log.getActivityAction()).isEqualTo(ActivityAction.USER_CREATED);
        assertThat(log.getActivityResourceType()).isEqualTo(ActivityResourceType.USER);
        assertThat(log.getResourceId()).isEqualTo(20L);
        assertThat(log.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("logUserCreateFailure - Kullanıcı oluşturma hatasını kaydetmelidir")
    void logUserCreateFailure_shouldSaveActivityLog() {
        activityLogService.logUserCreateFailure(10L, "Username duplicate", ipAddress);

        ArgumentCaptor<ActivityLog> captor = ArgumentCaptor.forClass(ActivityLog.class);
        verify(activityLogRepository).save(captor.capture());

        ActivityLog log = captor.getValue();
        assertThat(log.getActorUserId()).isEqualTo(10L);
        assertThat(log.getActivityAction()).isEqualTo(ActivityAction.USER_CREATE_FAILED);
        assertThat(log.getActivityResourceType()).isEqualTo(ActivityResourceType.USER);
        assertThat(log.getResourceId()).isNull();
        assertThat(log.isSuccess()).isFalse();
        assertThat(log.getFailureReason()).isEqualTo("Username duplicate");
    }

    @Test
    @DisplayName("logFlightCreated - Uçuş oluşturma logunu kaydetmelidir")
    void logFlightCreated_shouldSaveActivityLog() {
        activityLogService.logFlightCreated(10L, 50L, ipAddress);

        ArgumentCaptor<ActivityLog> captor = ArgumentCaptor.forClass(ActivityLog.class);
        verify(activityLogRepository).save(captor.capture());

        ActivityLog log = captor.getValue();
        assertThat(log.getActorUserId()).isEqualTo(10L);
        assertThat(log.getActivityAction()).isEqualTo(ActivityAction.FLIGHT_CREATED);
        assertThat(log.getActivityResourceType()).isEqualTo(ActivityResourceType.FLIGHT);
        assertThat(log.getResourceId()).isEqualTo(50L);
        assertThat(log.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("logFlightCancel - Uçuş iptal logunu kaydetmelidir")
    void logFlightCancel_shouldSaveActivityLog() {
        activityLogService.logFlightCancel(10L, 50L, ipAddress);

        ArgumentCaptor<ActivityLog> captor = ArgumentCaptor.forClass(ActivityLog.class);
        verify(activityLogRepository).save(captor.capture());

        ActivityLog log = captor.getValue();
        assertThat(log.getActorUserId()).isEqualTo(10L);
        assertThat(log.getActivityAction()).isEqualTo(ActivityAction.FLIGHT_CANCELED);
        assertThat(log.getActivityResourceType()).isEqualTo(ActivityResourceType.FLIGHT);
        assertThat(log.getResourceId()).isEqualTo(50L);
        assertThat(log.isSuccess()).isTrue();
    }
}
