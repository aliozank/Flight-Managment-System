package com.alikaracor.learning.flightservice.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {

        http.csrf(csrf -> csrf.disable())

                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .authorizeHttpRequests(authorize -> authorize

                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/logout").authenticated()

                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/actuator/prometheus").permitAll()

                        .requestMatchers("/actuator/**")
                        .hasAnyRole("ADMIN", "DEVOPS")

                        .requestMatchers("/api/users")
                        .hasRole("ADMIN")

                        .requestMatchers("/api/users/**")
                        .hasRole("ADMIN")

                        .requestMatchers(HttpMethod.GET, "/api/flights/**")
                        .hasAnyRole("ADMIN", "OPERATIONS")

                        .requestMatchers(HttpMethod.POST, "/api/flights/**")
                        .hasAnyRole("ADMIN", "OPERATIONS")

                        .requestMatchers(HttpMethod.PUT, "/api/flights/**")
                        .hasAnyRole("ADMIN", "OPERATIONS")

                        .requestMatchers(HttpMethod.PATCH, "/api/flights/**")
                        .hasAnyRole("ADMIN", "OPERATIONS")

                        .requestMatchers(HttpMethod.DELETE, "/api/flights/**")
                        .hasRole("ADMIN")

                        .requestMatchers(HttpMethod.GET, "/api/reports/**")
                        .hasAnyRole("ADMIN", "BI_ANALYST")

                        .requestMatchers("/api/**").denyAll()

                        .requestMatchers("/ws" ,"/ws/**").permitAll()

                        .requestMatchers(
                                "/v3/api-docs",
                                "/v3/api-docs/**",
                                "/swagger-ui.html",
                                "/swagger-ui/**"
                        ).permitAll()


                        .anyRequest().denyAll()
                )

                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));

        return http.build();

    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {

        JwtGrantedAuthoritiesConverter rolesConverter = new JwtGrantedAuthoritiesConverter();

        rolesConverter.setAuthorityPrefix("ROLE_");
        rolesConverter.setAuthoritiesClaimName("roles");

        JwtAuthenticationConverter authenticationConverter = new JwtAuthenticationConverter();

        authenticationConverter.setJwtGrantedAuthoritiesConverter(rolesConverter);

        return authenticationConverter;


    }


}
