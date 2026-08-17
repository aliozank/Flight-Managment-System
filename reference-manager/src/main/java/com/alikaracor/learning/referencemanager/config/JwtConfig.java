package com.alikaracor.learning.referencemanager.config;

import com.alikaracor.learning.referencemanager.security.RevokedTokenValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.security.interfaces.RSAPublicKey;

@Configuration
public class JwtConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.public-key-location}")
    private RSAPublicKey publicKey;

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
