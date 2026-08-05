package com.alikaracor.learning.flightservice.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebSocketJwtAuthInterceptorTest {

    @Mock
    private JwtDecoder jwtDecoder;

    @Mock
    private JwtAuthenticationConverter jwtAuthenticationConverter;

    @Mock
    private MessageChannel messageChannel;

    @InjectMocks
    private WebSocketJwtAuthInterceptor interceptor;

    private Message<?> createStompConnectMessage(String authHeader) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        if (authHeader != null) {
            accessor.setNativeHeader(HttpHeaders.AUTHORIZATION, authHeader);
        }
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Message<?> createStompSubscribeMessage() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Jwt createMockJwt() {
        return Jwt.withTokenValue("valid-token")
                .header("alg", "none")
                .claim("sub", "100")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }

    @Test
    @DisplayName("preSend - Geçerli ADMIN tokenı ile bağlantı kabul edilmeli ve Authentication atanmalıdır")
    void preSend_shouldAcceptConnectionAndSetUser_whenTokenIsValidWithAdminRole() {
        Message<?> connectMessage = createStompConnectMessage("Bearer valid-admin-token");
        Jwt jwt = createMockJwt();
        Authentication auth = new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        when(jwtDecoder.decode("valid-admin-token")).thenReturn(jwt);
        when(jwtAuthenticationConverter.convert(jwt)).thenReturn((JwtAuthenticationToken) auth);

        Message<?> result = interceptor.preSend(connectMessage, messageChannel);

        assertThat(result).isNotNull();
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(result, StompHeaderAccessor.class);
        assertThat(accessor).isNotNull();
        assertThat(accessor.getUser()).isEqualTo(auth);
    }

    @Test
    @DisplayName("preSend - Geçerli OPERATIONS tokenı ile bağlantı kabul edilmelidir")
    void preSend_shouldAcceptConnectionAndSetUser_whenTokenIsValidWithOperationsRole() {
        Message<?> connectMessage = createStompConnectMessage("Bearer valid-ops-token");
        Jwt jwt = createMockJwt();
        Authentication auth = new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_OPERATIONS")));

        when(jwtDecoder.decode("valid-ops-token")).thenReturn(jwt);
        when(jwtAuthenticationConverter.convert(jwt)).thenReturn((JwtAuthenticationToken) auth);

        Message<?> result = interceptor.preSend(connectMessage, messageChannel);

        assertThat(result).isNotNull();
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(result, StompHeaderAccessor.class);
        assertThat(accessor).isNotNull();
        assertThat(accessor.getUser()).isEqualTo(auth);
    }

    @Test
    @DisplayName("preSend - Geçerli BI_ANALYST tokenı ile bağlantı kabul edilmelidir")
    void preSend_shouldAcceptConnectionAndSetUser_whenTokenIsValidWithBiAnalystRole() {
        Message<?> connectMessage = createStompConnectMessage("Bearer valid-bi-token");
        Jwt jwt = createMockJwt();
        Authentication auth = new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_BI_ANALYST")));

        when(jwtDecoder.decode("valid-bi-token")).thenReturn(jwt);
        when(jwtAuthenticationConverter.convert(jwt)).thenReturn((JwtAuthenticationToken) auth);

        Message<?> result = interceptor.preSend(connectMessage, messageChannel);

        assertThat(result).isNotNull();
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(result, StompHeaderAccessor.class);
        assertThat(accessor).isNotNull();
        assertThat(accessor.getUser()).isEqualTo(auth);
    }

    @Test
    @DisplayName("preSend - Authorization header yoksa BadCredentialsException fırlatılmalıdır")
    void preSend_shouldThrowBadCredentialsException_whenAuthorizationHeaderIsMissing() {
        Message<?> connectMessage = createStompConnectMessage(null);

        assertThatThrownBy(() -> interceptor.preSend(connectMessage, messageChannel))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("WebSocket token bulunamadı");

        verifyNoInteractions(jwtDecoder, jwtAuthenticationConverter);
    }

    @Test
    @DisplayName("preSend - Header Bearer ile başlamıyorsa BadCredentialsException fırlatılmalıdır")
    void preSend_shouldThrowBadCredentialsException_whenAuthorizationHeaderDoesNotStartWithBearer() {
        Message<?> connectMessage = createStompConnectMessage("Basic dXNlcjpwYXNz");

        assertThatThrownBy(() -> interceptor.preSend(connectMessage, messageChannel))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("WebSocket token bulunamadı");

        verifyNoInteractions(jwtDecoder, jwtAuthenticationConverter);
    }

    @Test
    @DisplayName("preSend - JwtAuthenticationConverter null döndürürse BadCredentialsException fırlatılmalıdır")
    void preSend_shouldThrowBadCredentialsException_whenConverterReturnsNull() {
        Message<?> connectMessage = createStompConnectMessage("Bearer invalid-auth-token");
        Jwt jwt = createMockJwt();

        when(jwtDecoder.decode("invalid-auth-token")).thenReturn(jwt);
        when(jwtAuthenticationConverter.convert(jwt)).thenReturn(null);

        assertThatThrownBy(() -> interceptor.preSend(connectMessage, messageChannel))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("WebSocket token doğrulanamadı");
    }

    @Test
    @DisplayName("preSend - DEVOPS veya izin verilmeyen bir rol AccessDeniedException almalıdır")
    void preSend_shouldThrowAccessDeniedException_whenUserHasDisallowedRole() {
        Message<?> connectMessage = createStompConnectMessage("Bearer devops-token");
        Jwt jwt = createMockJwt();
        Authentication auth = new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_DEVOPS")));

        when(jwtDecoder.decode("devops-token")).thenReturn(jwt);
        when(jwtAuthenticationConverter.convert(jwt)).thenReturn((JwtAuthenticationToken) auth);

        assertThatThrownBy(() -> interceptor.preSend(connectMessage, messageChannel))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("WebSocket bağlantısı için yetkiniz yok");
    }

    @Test
    @DisplayName("preSend - CONNECT dışındaki STOMP mesajları değiştirilmeden geçmeli ve JwtDecoder çağrılmamalıdır")
    void preSend_shouldPassNonConnectMessages_withoutCallingJwtDecoder() {
        Message<?> subscribeMessage = createStompSubscribeMessage();

        Message<?> result = interceptor.preSend(subscribeMessage, messageChannel);

        assertThat(result).isEqualTo(subscribeMessage);
        verifyNoInteractions(jwtDecoder, jwtAuthenticationConverter);
    }
}
