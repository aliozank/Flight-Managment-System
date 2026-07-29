package com.alikaracor.learning.referencemanager.dto;

import com.alikaracor.learning.referencemanager.model.FlightTypeStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class FlightTypeRequest {

    @NotBlank
    @Size(max = 40)
    private String flightTypeName;

    @NotBlank
    @Pattern(
            regexp = "^(?=.{2,40}$)[A-Z]+(?:_[A-Z]+)*$",
            message = "Flight type code büyük harflerden oluşmalı, birden fazla kelime varsa alt çizgi ile ayrılmalıdır"
    )
    private String flightTypeCode;

    @NotNull
    private FlightTypeStatus flightTypeStatus;
}
