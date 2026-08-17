package com.alikaracor.learning.flightservice.security;

import com.alikaracor.learning.flightservice.service.TokenRevocationService;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class RevokedTokenValidator implements OAuth2TokenValidator<Jwt> {

    private final TokenRevocationService tokenRevocationService;

    public RevokedTokenValidator(TokenRevocationService tokenRevocationService) {
        this.tokenRevocationService = tokenRevocationService;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {

        String tokenId = jwt.getId();

        OAuth2Error error = new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN, "The token is invalid or revoked", null);

        if (tokenId == null || tokenId.isBlank()) {
            return OAuth2TokenValidatorResult.failure(error);
        }

        if (tokenRevocationService.isTokenRevoked(tokenId)) {
            return OAuth2TokenValidatorResult.failure(error);
        }

        return OAuth2TokenValidatorResult.success();
    }

}
