package com.distrischool.notifications.repository;

import com.distrischool.notifications.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Notification entity.
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * Find all unread notifications ordered by timestamp descending.
     */
    List<Notification> findByReadFalseOrderByTimestampDesc();

    /**
     * Find all notifications ordered by timestamp descending.
     */
    List<Notification> findAllByOrderByTimestampDesc();

    /**
     * Find notification by ID if it's unread.
     */
    Optional<Notification> findByIdAndReadFalse(Long id);

    /**
     * Find notification by event ID (to avoid duplicates).
     */
    Optional<Notification> findByEventId(String eventId);
}





