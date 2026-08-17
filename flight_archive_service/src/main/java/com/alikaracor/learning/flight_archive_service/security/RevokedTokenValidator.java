package com.alikaracor.learning.flight_archive_service.security;

import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;


import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;

@Component
public class RevokedTokenValidator implements OAuth2TokenValidator<Jwt> {

    private static final String KEY_PREFIX = "revoked-token:";

    private final StringRedisTemplate redisTemplate;

    public RevokedTokenValidator(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {

        String tokenId = jwt.getId();

        OAuth2Error error = new OAuth2Error(
                OAuth2ErrorCodes.INVALID_TOKEN,
                "The token is invalid or revoked.",
                null
        );

        if (tokenId == null || tokenId.isBlank()) {
            return OAuth2TokenValidatorResult.failure(error);
        }

        boolean revoked = Boolean.TRUE.equals(
                redisTemplate.hasKey(KEY_PREFIX + tokenId)
        );

        if (revoked) {
            return OAuth2TokenValidatorResult.failure(error);
        }

        return OAuth2TokenValidatorResult.success();
    }
}
