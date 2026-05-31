package com.jobportal.job_portal.repository;

import com.jobportal.job_portal.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // All notifications for a user, newest first
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    // Unread count for badge
    long countByUserIdAndIsReadFalse(Long userId);

    // Mark all unread as read for a user
    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.userId = :userId AND n.isRead = false")
    void markAllReadForUser(@Param("userId") Long userId);
}