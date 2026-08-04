package com.alikaracor.learning.flightservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.client.RestClient;

@Configuration
public class ReferenceManagerClientConfig {

    @Bean
    public RestClient referenceManagerRestClient(
            @Value("${app.reference-manager.base-url}")
            String referenceManagerBaseUrl
    ) {

        return RestClient.builder()
                .baseUrl(referenceManagerBaseUrl)
                .requestInterceptor((request, body, execution) -> {

                    Authentication authentication =
                            SecurityContextHolder
                                    .getContext()
                                    .getAuthentication();

                    if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {

                        String token = jwtAuthentication
                                .getToken()
                                .getTokenValue();

                        request.getHeaders().setBearerAuth(token);
                    }

                    return execution.execute(request, body);
                })
                .build();
    }
}