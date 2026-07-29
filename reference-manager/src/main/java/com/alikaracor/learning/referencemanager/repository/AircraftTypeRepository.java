package com.alikaracor.learning.referencemanager.repository;

import com.alikaracor.learning.referencemanager.model.AircraftType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AircraftTypeRepository extends JpaRepository<AircraftType, Long> {

    boolean existsByAircraftTypeIcaoCodeIgnoreCase(String icaoCode);

    boolean existsByAircraftTypeIcaoCodeIgnoreCaseAndAircraftTypeIdNot(String aircraftTypeIcaoCode, Long aircraftTypeId);

}
