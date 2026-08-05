package com.alikaracor.learning.flightservice.service;

import com.alikaracor.learning.flightservice.model.Role;
import com.alikaracor.learning.flightservice.model.RoleName;
import com.alikaracor.learning.flightservice.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    @Mock
    private JwtEncoder jwtEncoder;

    private JwtService jwtService;
    private User sampleUser;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(jwtEncoder, 3600L);

        Role adminRole = new Role();
        adminRole.setRoleId(1L);
        adminRole.setRoleName(RoleName.ADMIN);

        sampleUser = new User();
        sampleUser.setUserId(42L);
        sampleUser.setUserName("john_admin");
        sampleUser.setUserRoles(Set.of(adminRole));
    }

    @Test
    @DisplayName("generateToken - userId (sub), username ve roles claim'lerini doğru şekilde eklemeli ve JWT üretmelidir")
    void generateToken_shouldContainUserIdUsernameAndRoles() {
        Jwt mockJwt = Jwt.withTokenValue("jwt-token-xyz")
                .header("alg", "RS256")
                .claim("sub", "42")
                .claim("username", "john_admin")
                .claim("roles", List.of("ADMIN"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(mockJwt);

        String token = jwtService.generateToken(sampleUser);

        assertThat(token).isEqualTo("jwt-token-xyz");

        ArgumentCaptor<JwtEncoderParameters> captor = ArgumentCaptor.forClass(JwtEncoderParameters.class);
        verify(jwtEncoder).encode(captor.capture());

        JwtClaimsSet claims = captor.getValue().getClaims();
        assertThat(claims.getSubject()).isEqualTo("42");
        assertThat(claims.getClaimAsString("username")).isEqualTo("john_admin");
        assertThat(claims.getClaimAsStringList("roles")).containsExactly("ADMIN");
        assertThat(claims.getClaimAsString("iss")).isEqualTo("flight-service");
    }

    @Test
    @DisplayName("getExpirationSeconds - Konfigüre edilen süreyi dönmelidir")
    void getExpirationSeconds_shouldReturnConfiguredValue() {
        assertThat(jwtService.getExpirationSeconds()).isEqualTo(3600L);
    }
}
