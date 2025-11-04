package com.distrischool.notifications.controller;

import com.distrischool.notifications.dto.NotificationDTO;
import com.distrischool.notifications.service.NotificationService;
import com.distrischool.template.dto.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST Controller for notifications.
 * Provides endpoints matching frontend expectations.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    /**
     * GET /api/v1/notifications
     * Returns all notifications in the format expected by the frontend.
     * Response format: { success: boolean, data: Notification[], message?: string }
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationDTO>>> getAllNotifications() {
        log.info("GET /api/v1/notifications - Fetching all notifications");

        try {
            List<NotificationDTO> notifications = notificationService.getAllNotifications()
                    .stream()
                    .map(n -> NotificationDTO.fromEntity(n, objectMapper))
                    .collect(Collectors.toList());

            ApiResponse<List<NotificationDTO>> response = new ApiResponse<>(
                    true,
                    String.format("Found %d notifications", notifications.size()),
                    notifications
            );

            log.info("Returning {} notifications", notifications.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching notifications", e);
            ApiResponse<List<NotificationDTO>> errorResponse = ApiResponse.error(
                    "Failed to fetch notifications: " + e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * PUT /api/v1/notifications/{id}/read
     * Marks a notification as read.
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable Long id) {
        log.info("PUT /api/v1/notifications/{}/read - Marking notification as read", id);

        try {
            var notification = notificationService.markAsRead(id);
            if (notification.isPresent()) {
                ApiResponse<Void> response = ApiResponse.success("Notification marked as read");
                return ResponseEntity.ok(response);
            } else {
                ApiResponse<Void> response = ApiResponse.error("Notification not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        } catch (Exception e) {
            log.error("Error marking notification {} as read", id, e);
            ApiResponse<Void> errorResponse = ApiResponse.error(
                    "Failed to mark notification as read: " + e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}

