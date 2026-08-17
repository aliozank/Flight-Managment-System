package com.alikaracor.learning.flight_archive_service.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RevokedTokenValidatorTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @InjectMocks
    private RevokedTokenValidator revokedTokenValidator;

    private Jwt sampleJwt;

    @BeforeEach
    void setUp() {
        sampleJwt = Jwt.withTokenValue("sample-jwt-token")
                .header("alg", "RS256")
                .jti("archive-jti-uuid-456")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }

    @Test
    @DisplayName("validate - jti eksik/null olduğunda failure dönmelidir")
    void validate_shouldReturnFailure_whenJtiIsNull() {
        Jwt jwtWithoutJti = Jwt.withTokenValue("no-jti-token")
                .header("alg", "RS256")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        OAuth2TokenValidatorResult result = revokedTokenValidator.validate(jwtWithoutJti);

        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors()).extracting("description").contains("The token is invalid or revoked.");
        verifyNoInteractions(redisTemplate);
    }

    @Test
    @DisplayName("validate - jti boş (blank) olduğunda failure dönmelidir")
    void validate_shouldReturnFailure_whenJtiIsBlank() {
        Jwt jwtWithBlankJti = Jwt.withTokenValue("blank-jti-token")
                .header("alg", "RS256")
                .jti("   ")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        OAuth2TokenValidatorResult result = revokedTokenValidator.validate(jwtWithBlankJti);

        assertThat(result.hasErrors()).isTrue();
        verifyNoInteractions(redisTemplate);
    }

    @Test
    @DisplayName("validate - Token Redis'te revoked-token key'ine sahipse failure dönmelidir")
    void validate_shouldReturnFailure_whenTokenIsRevokedInRedis() {
        when(redisTemplate.hasKey("revoked-token:archive-jti-uuid-456")).thenReturn(true);

        OAuth2TokenValidatorResult result = revokedTokenValidator.validate(sampleJwt);

        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors()).extracting("description").contains("The token is invalid or revoked.");
        verify(redisTemplate).hasKey("revoked-token:archive-jti-uuid-456");
    }

    @Test
    @DisplayName("validate - Token Redis'te bulunmuyorsa success dönmelidir")
    void validate_shouldReturnSuccess_whenTokenIsNotRevokedInRedis() {
        when(redisTemplate.hasKey("revoked-token:archive-jti-uuid-456")).thenReturn(false);

        OAuth2TokenValidatorResult result = revokedTokenValidator.validate(sampleJwt);

        assertThat(result.hasErrors()).isFalse();
        verify(redisTemplate).hasKey("revoked-token:archive-jti-uuid-456");
    }
}
