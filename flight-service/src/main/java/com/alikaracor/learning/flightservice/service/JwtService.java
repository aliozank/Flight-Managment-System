package com.alikaracor.learning.flightservice.service;

import com.alikaracor.learning.flightservice.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class JwtService {


    private final JwtEncoder jwtEncoder;
    private final long expirationSeconds;

    public JwtService(JwtEncoder jwtEncoder, @Value("${app.jwt.expiration-seconds}") long expirationSeconds)
    {
        this.jwtEncoder = jwtEncoder;
        this.expirationSeconds = expirationSeconds;
    }

    public String generateToken(User user) {

        Instant now = Instant.now();
        String tokenId = UUID.randomUUID().toString();

        List<String> roleNames = user.getUserRoles()
                .stream()
                .map(role -> role.getRoleName().name())
                .toList();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("flight-service")
                .subject(user.getUserId().toString())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(expirationSeconds))
                .claim("username", user.getUserName())
                .claim("roles", roleNames)
                .id(tokenId)
                .build();

        return jwtEncoder
                .encode(JwtEncoderParameters.from(claims))
                .getTokenValue();
    }


    //authservice için hammalık cezası
    public long getExpirationSeconds() {
        return expirationSeconds;
    }



}
