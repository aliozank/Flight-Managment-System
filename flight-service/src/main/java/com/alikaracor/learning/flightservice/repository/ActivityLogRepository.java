package com.alikaracor.learning.flightservice.repository;

import com.alikaracor.learning.flightservice.model.ActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

}
