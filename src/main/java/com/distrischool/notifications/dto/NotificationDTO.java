package com.distrischool.notifications.dto;

import com.distrischool.notifications.entity.Notification;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.format.DateTimeFormatter;

/**
 * DTO for Notification that matches frontend Notification interface.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDTO {

    private String id;
    private String type;
    private String title;
    private String message;
    private String timestamp;
    private Boolean read;
    private Object data; // Can be parsed JSON object

    /**
     * Convert Notification entity to DTO.
     */
    public static NotificationDTO fromEntity(Notification notification, ObjectMapper objectMapper) {
        NotificationDTO dto = new NotificationDTO();
        
        // Convert Long id to String
        dto.setId(notification.getId().toString());
        
        dto.setType(notification.getEventType());
        dto.setTitle(notification.getTitle());
        dto.setMessage(notification.getMessage());
        dto.setRead(notification.getRead());
        
        // Format timestamp as ISO string
        dto.setTimestamp(notification.getTimestamp().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        // Parse JSON data string back to object
        if (notification.getData() != null && !notification.getData().isEmpty()) {
            try {
                dto.setData(objectMapper.readValue(notification.getData(), Object.class));
            } catch (Exception e) {
                // If parsing fails, set as null or empty object
                dto.setData(null);
            }
        } else {
            dto.setData(null);
        }
        
        return dto;
    }
}


