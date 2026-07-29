package com.alikaracor.learning.referencemanager.repository;

import com.alikaracor.learning.referencemanager.model.Aircraft;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AircraftRepository extends JpaRepository<Aircraft, Long> {


    boolean existsByAircraftRegistrationNumberIgnoreCase(String aircraftRegistrationNumber);

    boolean existsByAircraftRegistrationNumberIgnoreCaseAndAircraftIdNot(String registrationNumber, Long id);


}
