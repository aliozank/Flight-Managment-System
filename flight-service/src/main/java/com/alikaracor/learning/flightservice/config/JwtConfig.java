package com.alikaracor.learning.flightservice.config;

import com.alikaracor.learning.flightservice.security.RevokedTokenValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

@Configuration
public class JwtConfig {

    @Value("${app.jwt.public-key}")
    private RSAPublicKey publicKey;

    @Value("${app.jwt.private-key}")
    private RSAPrivateKey privateKey;

    @Bean
    public JwtEncoder jwtEncoder() {
        return NimbusJwtEncoder
                .withKeyPair(publicKey, privateKey)
                .build();
    }


    @Bean
    public JwtDecoder jwtDecoder(RevokedTokenValidator revokedTokenValidator) {

        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder
                .withPublicKey(publicKey)
                .build();

        OAuth2TokenValidator<Jwt> defaultValidator = JwtValidators.createDefaultWithIssuer("flight-service");

        OAuth2TokenValidator<Jwt> combinedValidator = new DelegatingOAuth2TokenValidator<>(defaultValidator, revokedTokenValidator);

        jwtDecoder.setJwtValidator(combinedValidator);

        return jwtDecoder;
    }

}
