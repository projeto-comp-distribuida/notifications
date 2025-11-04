package com.distrischool.notifications.service;

import com.distrischool.notifications.entity.Notification;
import com.distrischool.notifications.websocket.NotificationWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for broadcasting notifications via WebSocket to connected clients.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationBroadcastService {

    private final NotificationWebSocketHandler webSocketHandler;

    /**
     * Broadcasts a notification to all connected WebSocket clients.
     * 
     * @param notification The notification to broadcast
     */
    public void broadcastNotification(Notification notification) {
        try {
            webSocketHandler.broadcastNotification(notification);
            log.info("Broadcasted notification {} to WebSocket clients", notification.getId());
        } catch (Exception e) {
            log.error("Error broadcasting notification {}: {}", notification.getId(), e.getMessage(), e);
        }
    }

    /**
     * Broadcasts a notification with a custom message format.
     * 
     * @param notification The notification to broadcast
     */
    public void broadcastNotificationWithWrapper(Notification notification) {
        // Same as broadcastNotification for plain WebSocket
        broadcastNotification(notification);
    }
}

