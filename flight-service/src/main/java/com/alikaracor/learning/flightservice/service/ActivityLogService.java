package com.alikaracor.learning.flightservice.service;

import com.alikaracor.learning.flightservice.model.ActivityAction;
import com.alikaracor.learning.flightservice.model.ActivityLog;
import com.alikaracor.learning.flightservice.model.ActivityResourceType;
import com.alikaracor.learning.flightservice.repository.ActivityLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;

    public ActivityLogService(ActivityLogRepository activityLogRepository) {
        this.activityLogRepository = activityLogRepository;
    }

    public void logLoginSuccess(Long authenticatedUserId, String clientIpAddress) {

        saveActivityLog(
                authenticatedUserId,
                ActivityAction.LOGIN_SUCCESS,
                ActivityResourceType.AUTH,
                authenticatedUserId,
                true,
                null,
                clientIpAddress
        );
    }
@Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logLoginFailure(Long matchedUserId, String failureReason, String clientIpAddress) {

        saveActivityLog(
                null,
                ActivityAction.LOGIN_FAILED,
                ActivityResourceType.AUTH,
                matchedUserId,
                false,
                failureReason,
                clientIpAddress
        );
    }

    public void logUserCreated(Long performedByUserId, Long createdUserId, String clientIpAddress) {

        saveActivityLog(
                performedByUserId,
                ActivityAction.USER_CREATED,
                ActivityResourceType.USER,
                createdUserId,
                true,
                null,
                clientIpAddress
        );
    }

    public void logUserUpdated(Long performedByUserId, Long updatedUserId, String clientIpAddress) {

        saveActivityLog(
                performedByUserId,
                ActivityAction.USER_UPDATED,
                ActivityResourceType.USER,
                updatedUserId,
                true,
                null,
                clientIpAddress
        );
    }
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logUserCreateFailure(Long performedByUserId, String failureReason, String clientIpAddress) {

        saveActivityLog(
                performedByUserId,
                ActivityAction.USER_CREATE_FAILED,
                ActivityResourceType.USER,
                null,
                false,
                failureReason,
                clientIpAddress
        );
    }
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logUserUpdateFailure(Long performedByUserId, Long requestedUserId, String failureReason, String clientIpAddress) {

        saveActivityLog(
                performedByUserId,
                ActivityAction.USER_UPDATE_FAILED,
                ActivityResourceType.USER,
                requestedUserId,
                false,
                failureReason,
                clientIpAddress
        );
    }

    public void logFlightCreated(Long performedByUserId, Long flightId, String clientIpAddress) {

        saveActivityLog(
                performedByUserId,
                ActivityAction.FLIGHT_CREATED,
                ActivityResourceType.FLIGHT,
                flightId,
                true,
                null,
                clientIpAddress
        );
    }
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logFlightCreateFailure(Long performedByUserId, String failureReason, String clientIpAddress) {

        saveActivityLog(
                performedByUserId,
                ActivityAction.FLIGHT_CREATE_FAILED,
                ActivityResourceType.FLIGHT,
                null,
                false,
                failureReason,
                clientIpAddress
        );
    }


    public void logFlightUpdated(Long performedByUserId, Long updatedFlightId, String clientIpAddress) {

        saveActivityLog(
                performedByUserId,
                ActivityAction.FLIGHT_UPDATED,
                ActivityResourceType.FLIGHT,
                updatedFlightId,
                true,
                null,
                clientIpAddress
        );
    }

    public void logAutomaticFlightStatusUpdated(Long flightId) {

        saveActivityLog(
                null,
                ActivityAction.FLIGHT_STATUS_AUTO_UPDATED,
                ActivityResourceType.FLIGHT,
                flightId,
                true,
                null,
                null
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logFlightUpdateFailure(Long performedByUserId,Long updatedFlightId, String failureReason, String clientIpAddress) {

        saveActivityLog(
                performedByUserId,
                ActivityAction.FLIGHT_UPDATE_FAILED,
                ActivityResourceType.FLIGHT,
                updatedFlightId,
                false,
                failureReason,
                clientIpAddress
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAutomaticFlightStatusUpdateFailure(Long flightId, String failureReason) {

        saveActivityLog(
                null,
                ActivityAction.FLIGHT_STATUS_AUTO_UPDATE_FAILED,
                ActivityResourceType.FLIGHT,
                flightId,
                false,
                failureReason,
                null
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logFlightCancelFailure(Long performedByUserId,Long updatedFlightId, String failureReason, String clientIpAddress) {

        saveActivityLog(
                performedByUserId,
                ActivityAction.FLIGHT_CANCEL_FAILED,
                ActivityResourceType.FLIGHT,
                updatedFlightId,
                false,
                failureReason,
                clientIpAddress
        );
    }


    public void logFlightCancel(Long performedByUserId, Long updatedFlightId, String clientIpAddress) {

        saveActivityLog(
                performedByUserId,
                ActivityAction.FLIGHT_CANCELED,
                ActivityResourceType.FLIGHT,
                updatedFlightId,
                true,
                null,
                clientIpAddress
        );
    }


    private void saveActivityLog(
            Long performedByUserId,
            ActivityAction activityAction,
            ActivityResourceType resourceType,
            Long affectedResourceId,
            boolean successful,
            String failureReason,
            String clientIpAddress
    ) {
        ActivityLog activityLog = new ActivityLog();

        activityLog.setActorUserId(performedByUserId);
        activityLog.setActivityAction(activityAction);
        activityLog.setActivityResourceType(resourceType);
        activityLog.setResourceId(affectedResourceId);
        activityLog.setSuccess(successful);
        activityLog.setFailureReason(failureReason);
        activityLog.setIpAddress(clientIpAddress);

        activityLogRepository.save(activityLog);
    }
}
