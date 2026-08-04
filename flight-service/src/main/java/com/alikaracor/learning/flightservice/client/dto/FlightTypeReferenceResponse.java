package com.alikaracor.learning.flightservice.client.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
public class FlightTypeReferenceResponse implements Serializable {

   private Long flightTypeId;
   private String flightTypeStatus;

}
