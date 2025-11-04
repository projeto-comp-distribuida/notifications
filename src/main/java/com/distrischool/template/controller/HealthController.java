package com.distrischool.notifications.controller;

import com.distrischool.template.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Health check controller for notifications service.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getHealth() {
        log.info("GET /api/v1/health - Checking service health");
        
        Map<String, Object> healthInfo = Map.of(
            "status", "UP",
            "timestamp", LocalDateTime.now(),
            "service", "DistriSchool Notifications Service",
            "version", "1.0.0"
        );
        
        return ResponseEntity.ok(ApiResponse.success(healthInfo, "Service is running correctly"));
    }

    @GetMapping("/info")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getInfo() {
        log.info("GET /api/v1/health/info - Getting service information");
        
        Map<String, Object> serviceInfo = Map.of(
            "name", "DistriSchool Notifications Service",
            "description", "Service for consuming Kafka events and providing notifications to the frontend",
            "version", "1.0.0",
            "features", new String[]{
                "Spring Boot 3.2.0",
                "PostgreSQL with Flyway",
                "Apache Kafka for event consumption",
                "REST API for notifications",
                "WebSocket support (via gateway)"
            }
        );
        
        return ResponseEntity.ok(ApiResponse.success(serviceInfo, "Service information"));
    }
}
