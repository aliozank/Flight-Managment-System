package com.alikaracor.learning.flightservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LoginRequest {


    @NotBlank
    @Size(min = 3, max = 50)
    @Pattern(
            regexp = "^[A-Za-z0-9._-]+$",
            message = "Kullanıcı adı yalnızca harf, rakam, nokta, alt çizgi ve tire içerebilir"
    )
    private String userName;

    @NotBlank
    @Size(min = 3, max = 72)
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
            message = "Parola en az bir küçük harf, bir büyük harf ve bir rakam içermelidir"
    )
    private String userPassword;
}
