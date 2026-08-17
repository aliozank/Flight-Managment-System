package com.alikaracor.learning.flightservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenRevocationServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private TokenRevocationService tokenRevocationService;

    @BeforeEach
    void setUp() {
        tokenRevocationService = new TokenRevocationService(redisTemplate);
    }

    @Test
    @DisplayName("revokeToken - Geçerli tokenId ve gelecek bir expiresAt ile Redis'e doğru key ve TTL ile kaydetmelidir")
    void revokeToken_shouldSetRedisKeyWithProperTtl_whenTokenIdAndExpiresAtAreValid() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        String tokenId = "test-jti-12345";
        Instant expiresAt = Instant.now().plusSeconds(1800);

        tokenRevocationService.revokeToken(tokenId, expiresAt);

        ArgumentCaptor<Duration> durationCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(valueOperations).set(eq("revoked-token:test-jti-12345"), eq("true"), durationCaptor.capture());

        Duration capturedDuration = durationCaptor.getValue();
        assertThat(capturedDuration.getSeconds()).isBetween(1790L, 1800L);
    }

    @Test
    @DisplayName("revokeToken - Null veya blank tokenId verildiğinde IllegalArgumentException fırlatmalıdır")
    void revokeToken_shouldThrowException_whenTokenIdIsNullAddressOrBlank() {
        Instant expiresAt = Instant.now().plusSeconds(1800);

        assertThatThrownBy(() -> tokenRevocationService.revokeToken(null, expiresAt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("tokenId and expiresAt must not be null");

        assertThatThrownBy(() -> tokenRevocationService.revokeToken("   ", expiresAt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("tokenId and expiresAt must not be null");

        verifyNoInteractions(redisTemplate);
    }

    @Test
    @DisplayName("revokeToken - Null expiresAt verildiğinde IllegalArgumentException fırlatmalıdır")
    void revokeToken_shouldThrowException_whenExpiresAtIsNull() {
        assertThatThrownBy(() -> tokenRevocationService.revokeToken("test-jti-123", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("tokenId and expiresAt must not be null");

        verifyNoInteractions(redisTemplate);
    }

    @Test
    @DisplayName("revokeToken - Geçmiş tarihli (süresi dolmuş) expiresAt verildiğinde Redis'e kaydetmeden sonlanmalıdır")
    void revokeToken_shouldReturnEarly_whenExpiresAtIsAlreadyExpired() {
        Instant expiredAt = Instant.now().minusSeconds(10);

        tokenRevocationService.revokeToken("expired-jti-999", expiredAt);

        verifyNoInteractions(redisTemplate);
    }

    @Test
    @DisplayName("isTokenRevoked - Redis'te key bulunursa true dönmelidir")
    void isTokenRevoked_shouldReturnTrue_whenKeyExistsInRedis() {
        when(redisTemplate.hasKey("revoked-token:revoked-jti-1")).thenReturn(true);

        boolean revoked = tokenRevocationService.isTokenRevoked("revoked-jti-1");

        assertThat(revoked).isTrue();
        verify(redisTemplate).hasKey("revoked-token:revoked-jti-1");
    }

    @Test
    @DisplayName("isTokenRevoked - Redis'te key bulunmazsa false dönmelidir")
    void isTokenRevoked_shouldReturnFalse_whenKeyDoesNotExistInRedis() {
        when(redisTemplate.hasKey("revoked-token:valid-jti-1")).thenReturn(false);

        boolean revoked = tokenRevocationService.isTokenRevoked("valid-jti-1");

        assertThat(revoked).isFalse();
        verify(redisTemplate).hasKey("revoked-token:valid-jti-1");
    }
}
