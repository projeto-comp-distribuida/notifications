package com.distrischool.template.kafka;

import com.distrischool.notifications.service.NotificationService;
import com.distrischool.template.kafka.DistriSchoolEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.Map;

/**
 * Kafka Consumer for receiving events from other DistriSchool microservices.
 * Processes events and saves them as notifications.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        log.info("EventConsumer initialized. Listening to topics: distrischool.auth.user.created, teacher-events, distrischool.events");
    }

    /**
     * Consumer for DistriSchool events.
     * Listens to multiple topics: user events, teacher events, and generic events.
     * Accepts events as Map to handle different event types from different services.
     * Note: groupId is set in the ConsumerFactory, so we don't need to specify it here.
     */
    @KafkaListener(
            topics = {
                "distrischool.auth.user.created",
                "teacher-events",
                "distrischool.events"
            },
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeEvent(
            @Payload Map<String, Object> eventMap,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {
        
        log.info("Event received from topic '{}' [partition: {}, offset: {}]", topic, partition, offset);
        log.debug("Event details: {}", eventMap);
        
        try {
            // Convert Map to DistriSchoolEvent
            DistriSchoolEvent event = convertMapToEvent(eventMap);
            
            // Process events that should become notifications
            String eventType = event.getEventType();
            if (shouldCreateNotification(eventType)) {
                notificationService.saveNotification(event);
                log.info("Notification created successfully for event: {}", event.getEventId());
            } else {
                log.debug("Event type {} does not require a notification, skipping", eventType);
            }
        } catch (Exception e) {
            log.error("Error processing event: {}", e.getMessage(), e);
            // TODO: Implement retry logic or DLQ (Dead Letter Queue) if needed
        }
    }
    
    /**
     * Converts a Map event to DistriSchoolEvent.
     * Handles different event formats from different services.
     */
    private DistriSchoolEvent convertMapToEvent(Map<String, Object> eventMap) {
        try {
            // Convert Map to JSON string and then to DistriSchoolEvent
            String json = objectMapper.writeValueAsString(eventMap);
            return objectMapper.readValue(json, DistriSchoolEvent.class);
        } catch (Exception e) {
            log.warn("Failed to convert event map to DistriSchoolEvent, creating from map directly: {}", e.getMessage());
            // Fallback: create DistriSchoolEvent from map fields directly
            DistriSchoolEvent event = new DistriSchoolEvent();
            event.setEventId((String) eventMap.getOrDefault("eventId", java.util.UUID.randomUUID().toString()));
            event.setEventType((String) eventMap.getOrDefault("eventType", "unknown"));
            event.setSource((String) eventMap.getOrDefault("source", "unknown"));
            event.setVersion((String) eventMap.getOrDefault("version", "1.0"));
            event.setTimestamp(java.time.LocalDateTime.now());
            
            // Extract data - handle both nested data object and flat structure
            Object dataObj = eventMap.get("data");
            if (dataObj instanceof Map) {
                event.setData((Map<String, Object>) dataObj);
            } else {
                // If data is flat in the root, extract relevant fields
                Map<String, Object> data = new java.util.HashMap<>();
                if (eventMap.containsKey("userEmail")) data.put("userEmail", eventMap.get("userEmail"));
                if (eventMap.containsKey("email")) data.put("email", eventMap.get("email"));
                if (eventMap.containsKey("firstName")) data.put("firstName", eventMap.get("firstName"));
                if (eventMap.containsKey("lastName")) data.put("lastName", eventMap.get("lastName"));
                if (eventMap.containsKey("userName")) data.put("userName", eventMap.get("userName"));
                event.setData(data);
            }
            
            return event;
        }
    }

    /**
     * Determines if an event type should create a notification.
     * Processes: user.created, USER_CREATED, user.disabled, USER_DISABLED, teacher.created
     */
    private boolean shouldCreateNotification(String eventType) {
        if (eventType == null) {
            return false;
        }
        // Normalize to lowercase for comparison
        String normalized = eventType.toLowerCase();
        return "user.created".equals(normalized) ||
               "user_created".equals(normalized) ||
               "user.disabled".equals(normalized) ||
               "user_disabled".equals(normalized) ||
               "teacher.created".equals(normalized) ||
               "teacher_created".equals(normalized);
    }
}