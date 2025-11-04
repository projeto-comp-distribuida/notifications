package com.distrischool.notifications.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket handshake interceptor that validates JWT tokens from Auth0.
 * The token should be passed as a query parameter: ws://host/ws/notifications?token=JWT_TOKEN
 */
@Slf4j
@Component
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    private final JwtDecoder jwtDecoder;
    private final String audience;

    public WebSocketAuthInterceptor(JwtDecoder jwtDecoder, @Value("${auth0.audience}") String audience) {
        this.jwtDecoder = jwtDecoder;
        this.audience = audience;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        
        // Extract token from query parameters
        String token = extractTokenFromRequest(request);
        
        if (token == null || token.isEmpty()) {
            log.warn("WebSocket handshake rejected: No token provided");
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        try {
            // Decode and validate JWT token
            Jwt jwt = jwtDecoder.decode(token);
            
            // Verify audience
            if (jwt.getAudience() != null && !jwt.getAudience().isEmpty() && !jwt.getAudience().contains(audience)) {
                log.warn("WebSocket handshake rejected: Invalid audience in token");
                response.setStatusCode(HttpStatus.FORBIDDEN);
                return false;
            }

            // Store user information in session attributes for later use
            attributes.put("userId", jwt.getSubject());
            attributes.put("email", jwt.getClaimAsString("email"));
            attributes.put("name", jwt.getClaimAsString("name"));
            attributes.put("jwt", jwt);
            
            log.info("WebSocket handshake authenticated for user: {} ({})", 
                    jwt.getClaimAsString("email"), jwt.getSubject());
            
            return true;
            
        } catch (JwtException e) {
            log.warn("WebSocket handshake rejected: Invalid or expired token - {}", e.getMessage());
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        } catch (Exception e) {
            log.error("WebSocket handshake error: {}", e.getMessage(), e);
            response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // Nothing to do after handshake
    }

    /**
     * Extracts JWT token from request query parameters or Authorization header.
     * Supports both:
     * - Query parameter: ?token=JWT_TOKEN
     * - Authorization header: Authorization: Bearer JWT_TOKEN
     */
    private String extractTokenFromRequest(ServerHttpRequest request) {
        // Try query parameter first (most common for WebSocket)
        if (request instanceof ServletServerHttpRequest servletRequest) {
            String token = servletRequest.getServletRequest().getParameter("token");
            if (token != null && !token.isEmpty()) {
                return token;
            }
        }

        // Try Authorization header as fallback
        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        // Try Sec-WebSocket-Protocol header (some clients use this)
        String protocol = request.getHeaders().getFirst("Sec-WebSocket-Protocol");
        if (protocol != null && protocol.startsWith("Bearer ")) {
            return protocol.substring(7);
        }

        return null;
    }
}

