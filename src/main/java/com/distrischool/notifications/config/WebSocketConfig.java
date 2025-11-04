package com.distrischool.notifications.config;

import com.distrischool.notifications.websocket.NotificationWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket configuration for real-time notifications.
 * Uses plain WebSocket (not STOMP) for simple client connections.
 * Protected by JWT authentication via WebSocketAuthInterceptor.
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final NotificationWebSocketHandler notificationWebSocketHandler;
    private final WebSocketAuthInterceptor webSocketAuthInterceptor;

    @Value("${websocket.allowed-origins:*}")
    private String allowedOrigins;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Register the WebSocket handler at /ws/notifications
        // Authentication is handled by WebSocketAuthInterceptor during handshake
        registry.addHandler(notificationWebSocketHandler, "/ws/notifications")
                .addInterceptors(webSocketAuthInterceptor)
                .setAllowedOriginPatterns(allowedOrigins.split(","));
    }
}

