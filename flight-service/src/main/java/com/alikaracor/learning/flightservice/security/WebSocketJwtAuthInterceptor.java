package com.alikaracor.learning.flightservice.security;

import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class WebSocketJwtAuthInterceptor implements ChannelInterceptor {

    private final JwtDecoder jwtDecoder;
    private final JwtAuthenticationConverter jwtAuthenticationConverter;


    public WebSocketJwtAuthInterceptor(JwtDecoder jwtDecoder, JwtAuthenticationConverter jwtAuthenticationConverter) {
        this.jwtDecoder = jwtDecoder;
        this.jwtAuthenticationConverter = jwtAuthenticationConverter;
    }


    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {

        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(
                        message,
                        StompHeaderAccessor.class
                );

        if (accessor != null
                && StompCommand.CONNECT.equals(accessor.getCommand())) {

            String authorizationHeader =
                    accessor.getFirstNativeHeader(
                            HttpHeaders.AUTHORIZATION              //authorization başlığını ver demek
                    );

            if (authorizationHeader == null
                    || !authorizationHeader.startsWith("Bearer ")) {

                throw new BadCredentialsException(
                        "WebSocket token bulunamadı"
                );
            }

            String token = authorizationHeader.substring(7);

            Jwt jwt = jwtDecoder.decode(token);

            var authentication =                                    //Java sağ tarafa bakıp 'authentication' değişkeninin tipini otomatik çözer
                    jwtAuthenticationConverter.convert(jwt);

            if (authentication == null) {
                throw new BadCredentialsException(
                        "WebSocket token doğrulanamadı"
                );
            }

            Set<String> allowedRoles = Set.of(
                    "ROLE_ADMIN",
                    "ROLE_OPERATIONS",
                    "ROLE_BI_ANALYST"
            );

            boolean authorized = authentication.getAuthorities()
                    .stream()
                    .anyMatch(authority ->
                            allowedRoles.contains(
                                    authority.getAuthority()
                            )
                    );

            if (!authorized) {
                throw new AccessDeniedException(
                        "WebSocket bağlantısı için yetkiniz yok"
                );
            }

            accessor.setUser(authentication);
        }

        return message;
    }

}
