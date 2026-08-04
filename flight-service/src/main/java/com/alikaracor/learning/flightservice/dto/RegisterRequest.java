package com.alikaracor.learning.flightservice.dto;

import com.alikaracor.learning.flightservice.model.RoleName;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
public class RegisterRequest {

    @NotBlank
    @Size(min = 3, max = 50)
    @Pattern(
            regexp = "^[A-Za-z0-9._-]+$",
            message = "Kullanıcı adı yalnızca harf, rakam, nokta, alt çizgi ve tire içerebilir"
    )
    private String userName;

    @NotBlank
    @Email
    private String userEmail;

    @NotBlank
    @Size(min = 3, max = 72)
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
            message = "Parola en az bir küçük harf, bir büyük harf ve bir rakam içermelidir"
    )
    private String userPassword;

    @NotEmpty
    private Set<@NotNull RoleName> userRoleNames;

}
