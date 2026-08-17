package com.alikaracor.learning.flightservice.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;


import java.time.Duration;
import java.time.Instant;

@Service
public class TokenRevocationService {

    private final StringRedisTemplate redisTemplate;
    private static final String KEY_PREFIX = "revoked-token:";

    public TokenRevocationService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }


    public void revokeToken(String tokenId, Instant expiresAt) {

        if (tokenId == null || expiresAt == null || tokenId.isBlank()) {

            throw new IllegalArgumentException("tokenId and expiresAt must not be null");

        }

        Duration ttl = Duration.between(Instant.now(), expiresAt);

        if (ttl.isNegative() || ttl.isZero()) {

            return;

        }

        redisTemplate.opsForValue().set(KEY_PREFIX + tokenId, "true", ttl);

    }


    public boolean isTokenRevoked(String tokenId) {

        return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + tokenId));

    }

}
