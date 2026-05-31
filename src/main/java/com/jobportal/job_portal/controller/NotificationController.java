package com.jobportal.job_portal.controller;

import com.jobportal.job_portal.entity.Notification;
import com.jobportal.job_portal.repository.NotificationRepository;
import com.jobportal.job_portal.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*")
public class NotificationController {

    @Autowired
    NotificationRepository notificationRepo;

    @Autowired
    JwtUtil jwtUtil;

    private Long getAuthenticatedUserId(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null;
        String token = authHeader.substring(7);
        if (!jwtUtil.isTokenValid(token)) return null;
        return jwtUtil.extractUserId(token);
    }

    // ── Get all notifications for a user ─────────────────────────────────────
    @GetMapping("/{userId}")
    public ResponseEntity<?> getNotifications(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long userId) {

        Long authUserId = getAuthenticatedUserId(authHeader);
        if (authUserId == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        if (!authUserId.equals(userId))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Forbidden");

        List<Notification> notifications = notificationRepo.findByUserIdOrderByCreatedAtDesc(userId);
        return ResponseEntity.ok(notifications);
    }

    // ── Get unread count for navbar badge ────────────────────────────────────
    @GetMapping("/{userId}/unread-count")
    public ResponseEntity<?> getUnreadCount(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long userId) {

        Long authUserId = getAuthenticatedUserId(authHeader);
        if (authUserId == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        if (!authUserId.equals(userId))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Forbidden");

        long count = notificationRepo.countByUserIdAndIsReadFalse(userId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    // ── Mark all notifications as read ────────────────────────────────────────
    @PutMapping("/{userId}/mark-read")
    public ResponseEntity<?> markAllRead(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long userId) {

        Long authUserId = getAuthenticatedUserId(authHeader);
        if (authUserId == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        if (!authUserId.equals(userId))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Forbidden");

        notificationRepo.markAllReadForUser(userId);
        return ResponseEntity.ok(Map.of("message", "Marked as read"));
    }

    // ── Mark single notification as read ──────────────────────────────────────
    @PutMapping("/read/{notificationId}")
    public ResponseEntity<?> markOneRead(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long notificationId) {

        Long authUserId = getAuthenticatedUserId(authHeader);
        if (authUserId == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");

        Notification n = notificationRepo.findById(notificationId).orElse(null);
        if (n == null) return ResponseEntity.notFound().build();
        if (!n.getUserId().equals(authUserId))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Forbidden");

        n.setIsRead(true);
        notificationRepo.save(n);
        return ResponseEntity.ok(Map.of("message", "Marked as read"));
    }
}