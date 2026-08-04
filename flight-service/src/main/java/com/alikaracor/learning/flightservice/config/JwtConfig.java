package com.alikaracor.learning.flightservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

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
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder
                .withPublicKey(publicKey)
                .build();
    }

}
