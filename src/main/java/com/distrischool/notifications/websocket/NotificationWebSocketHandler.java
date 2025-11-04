package com.distrischool.notifications.websocket;

import com.distrischool.notifications.dto.NotificationDTO;
import com.distrischool.notifications.entity.Notification;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket handler for real-time notifications.
 * Handles plain WebSocket connections (not STOMP).
 */
@Slf4j
@Component
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public NotificationWebSocketHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.put(session.getId(), session);
        
        // Get authenticated user info from session attributes (set by WebSocketAuthInterceptor)
        String userId = (String) session.getAttributes().get("userId");
        String email = (String) session.getAttributes().get("email");
        
        log.info("WebSocket connection established: {} for user: {} ({}) (Total connections: {})", 
                session.getId(), email, userId, sessions.size());
        
        // Send welcome message with user info
        sendMessage(session, createWelcomeMessage(userId, email));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session.getId());
        log.info("WebSocket connection closed: {} (Reason: {}, Total connections: {})", 
                session.getId(), status, sessions.size());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        log.debug("Received message from {}: {}", session.getId(), message.getPayload());
        
        // Handle incoming messages (e.g., subscribe/unsubscribe, ping/pong)
        String payload = message.getPayload();
        try {
            var messageObj = objectMapper.readValue(payload, java.util.Map.class);
            String type = (String) messageObj.get("type");
            
            if ("ping".equals(type)) {
                sendMessage(session, createPongMessage());
            } else if ("subscribe".equals(type)) {
                sendMessage(session, createSubscribeAckMessage());
            }
        } catch (Exception e) {
            log.warn("Failed to parse incoming message: {}", e.getMessage());
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket transport error for session {}: {}", session.getId(), exception.getMessage(), exception);
        sessions.remove(session.getId());
    }

    /**
     * Broadcasts a notification to all connected WebSocket clients.
     */
    public void broadcastNotification(Notification notification) {
        NotificationDTO dto = NotificationDTO.fromEntity(notification, objectMapper);
        
        var message = new java.util.HashMap<String, Object>();
        message.put("type", "notification");
        message.put("data", dto);
        
        String jsonMessage;
        try {
            jsonMessage = objectMapper.writeValueAsString(message);
        } catch (Exception e) {
            log.error("Failed to serialize notification: {}", e.getMessage());
            return;
        }
        
        TextMessage textMessage = new TextMessage(jsonMessage);
        sessions.values().forEach(session -> {
            try {
                if (session.isOpen()) {
                    session.sendMessage(textMessage);
                }
            } catch (IOException e) {
                log.warn("Failed to send message to session {}: {}", session.getId(), e.getMessage());
                sessions.remove(session.getId());
            }
        });
        
        log.info("Broadcasted notification {} to {} WebSocket clients", notification.getId(), sessions.size());
    }

    private void sendMessage(WebSocketSession session, java.util.Map<String, Object> message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(json));
        } catch (IOException e) {
            log.error("Failed to send message to session {}: {}", session.getId(), e.getMessage());
        }
    }

    private java.util.Map<String, Object> createWelcomeMessage(String userId, String email) {
        var message = new java.util.HashMap<String, Object>();
        message.put("type", "welcome");
        message.put("message", "Connected to notifications service");
        message.put("authenticated", true);
        if (userId != null) {
            message.put("userId", userId);
        }
        if (email != null) {
            message.put("email", email);
        }
        message.put("timestamp", java.time.LocalDateTime.now().toString());
        return message;
    }

    private java.util.Map<String, Object> createPongMessage() {
        var message = new java.util.HashMap<String, Object>();
        message.put("type", "pong");
        message.put("timestamp", java.time.LocalDateTime.now().toString());
        return message;
    }

    private java.util.Map<String, Object> createSubscribeAckMessage() {
        var message = new java.util.HashMap<String, Object>();
        message.put("type", "subscribed");
        message.put("message", "You are now subscribed to notifications");
        message.put("timestamp", java.time.LocalDateTime.now().toString());
        return message;
    }
}

