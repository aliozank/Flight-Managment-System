package com.alikaracor.learning.referencemanager.repository;

import com.alikaracor.learning.referencemanager.model.FlightType;
import com.alikaracor.learning.referencemanager.model.FlightTypeStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FlightTypeRepository extends JpaRepository<FlightType, Long> {

    boolean existsByFlightTypeNameIgnoreCase(String flightTypeName);

    boolean existsByFlightTypeCodeIgnoreCase(String flightTypeCode);

    boolean existsByFlightTypeNameIgnoreCaseAndFlightTypeIdNot(String flightTypeName, Long flightTypeId);

    boolean existsByFlightTypeCodeIgnoreCaseAndFlightTypeIdNot(String flightTypeCode, Long flightTypeId);

    List<FlightType> findAllByFlightTypeStatusOrderByFlightTypeNameAsc(FlightTypeStatus flightTypeStatus);
}
