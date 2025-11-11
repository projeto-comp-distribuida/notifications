package com.distrischool.notifications.service;

import com.distrischool.notifications.entity.Notification;
import com.distrischool.notifications.repository.NotificationRepository;
import com.distrischool.notifications.kafka.DistriSchoolEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for managing notifications.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final ObjectMapper objectMapper;
    private final NotificationBroadcastService broadcastService;

    /**
     * Save a notification from a Kafka event.
     * Converts the event to a notification entity and persists it.
     */
    @Transactional
    public Notification saveNotification(DistriSchoolEvent event) {
        log.info("Processing event: {} with ID: {}", event.getEventType(), event.getEventId());

        // Check if notification already exists (avoid duplicates)
        Optional<Notification> existing = notificationRepository.findByEventId(event.getEventId());
        if (existing.isPresent()) {
            log.debug("Notification with eventId {} already exists, skipping", event.getEventId());
            return existing.get();
        }

        // Convert event to notification
        Notification notification = convertEventToNotification(event);

        // Save to database
        Notification saved = notificationRepository.save(notification);
        log.info("Saved notification with ID: {} for event: {}", saved.getId(), event.getEventType());

        // Broadcast to WebSocket clients
        try {
            broadcastService.broadcastNotificationWithWrapper(saved);
        } catch (Exception e) {
            log.warn("Failed to broadcast notification {} via WebSocket: {}", saved.getId(), e.getMessage());
            // Don't fail the save operation if broadcast fails
        }

        return saved;
    }

    /**
     * Convert Kafka event to Notification entity.
     * Maps event data to title and message based on event type.
     */
    private Notification convertEventToNotification(DistriSchoolEvent event) {
        Map<String, Object> eventData = event.getData();
        String eventType = event.getEventType();

        // Map event type to title and message (matching frontend expectations)
        String title;
        String message;
        
        // Normalize event type for comparison (handle both USER_CREATED and user.created formats)
        String normalizedEventType = eventType != null ? eventType.toLowerCase().replace("_", ".") : "";

        switch (normalizedEventType) {
            case "user.created":
                title = "Novo Usuário Criado";
                // Try multiple field names that might be in the event data
                String userName = getStringFromData(eventData, "userName") != null ? 
                    getStringFromData(eventData, "userName") : 
                    (getStringFromData(eventData, "firstName") != null && getStringFromData(eventData, "lastName") != null ?
                        getStringFromData(eventData, "firstName") + " " + getStringFromData(eventData, "lastName") :
                        null);
                String userEmail = getStringFromData(eventData, "userEmail") != null ?
                    getStringFromData(eventData, "userEmail") :
                    getStringFromData(eventData, "email");
                message = String.format("Usuário %s criado com sucesso", 
                    userName != null ? userName : (userEmail != null ? userEmail : "novo"));
                break;

            case "user.disabled":
                title = "Usuário Desabilitado";
                String disabledUserName = getStringFromData(eventData, "userName");
                String disabledUserEmail = getStringFromData(eventData, "userEmail");
                message = String.format("Usuário %s foi desabilitado",
                    disabledUserName != null ? disabledUserName : disabledUserEmail);
                break;

            case "teacher.created":
                title = "Novo Professor Criado";
                String teacherName = getStringFromData(eventData, "teacherName");
                String teacherEmail = getStringFromData(eventData, "teacherEmail");
                message = String.format("Professor %s cadastrado",
                    teacherName != null ? teacherName : teacherEmail);
                break;

            default:
                title = "Notificação";
                message = "Nova notificação disponível";
                log.warn("Unknown event type: {}, using default title and message", eventType);
        }

        // Convert event data to JSON string
        String dataJson = null;
        try {
            dataJson = objectMapper.writeValueAsString(eventData);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize event data to JSON: {}", e.getMessage());
        }

        // Create notification entity
        return Notification.builder()
                .eventId(event.getEventId())
                .eventType(eventType)
                .title(title)
                .message(message)
                .data(dataJson)
                .read(false)
                .timestamp(event.getTimestamp() != null ? event.getTimestamp() : java.time.LocalDateTime.now())
                .build();
    }

    /**
     * Get a string value from event data map.
     */
    private String getStringFromData(Map<String, Object> data, String key) {
        if (data == null) {
            return null;
        }
        Object value = data.get(key);
        if (value == null) {
            return null;
        }
        return value.toString();
    }

    /**
     * Get all notifications ordered by timestamp descending.
     */
    public List<Notification> getAllNotifications() {
        return notificationRepository.findAllByOrderByTimestampDesc();
    }

    /**
     * Get all unread notifications ordered by timestamp descending.
     */
    public List<Notification> getUnreadNotifications() {
        return notificationRepository.findByReadFalseOrderByTimestampDesc();
    }

    /**
     * Mark a notification as read.
     */
    @Transactional
    public Optional<Notification> markAsRead(Long id) {
        Optional<Notification> notification = notificationRepository.findById(id);
        if (notification.isPresent()) {
            Notification n = notification.get();
            n.setRead(true);
            Notification saved = notificationRepository.save(n);
            log.info("Marked notification {} as read", id);
            return Optional.of(saved);
        }
        return Optional.empty();
    }

    /**
     * Get notification by ID.
     */
    public Optional<Notification> getNotificationById(Long id) {
        return notificationRepository.findById(id);
    }
}

