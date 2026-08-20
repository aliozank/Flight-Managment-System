package com.alikaracor.learning.flightservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {

        return new OpenAPI()
                .info(new Info()
                        .title("Flight Service API")
                        .description("Uçuş, kullanıcı, kimlik doğrulama ve operasyon yönetimi API'si.")
                        .version("1.0.0"))
                .addSecurityItem(
                        new SecurityRequirement().addList(SECURITY_SCHEME_NAME)
                )
                .components(
                        new Components().addSecuritySchemes(
                                SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                        )
                );
    }


    @Bean
    public OpenApiCustomizer publicEndpointCustomizer() {
        return openApi -> {
            if (openApi.getPaths() == null) {
                return;
            }

            var loginPath = openApi.getPaths().get("/api/auth/login");

            if (loginPath != null && loginPath.getPost() != null) {
                loginPath.getPost().setSecurity(Collections.emptyList());
            }
        };
    }



}
