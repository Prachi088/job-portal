package com.jobportal.job_portal.controller;

import com.jobportal.job_portal.entity.Message;
import com.jobportal.job_portal.repository.ConnectionRepository;
import com.jobportal.job_portal.repository.MessageRepository;
import com.jobportal.job_portal.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/messages")
@CrossOrigin(origins = "*")
public class MessageController {

    @Autowired
    MessageRepository messageRepo;

    @Autowired
    ConnectionRepository connectionRepo;

    @Autowired
    JwtUtil jwtUtil;

    private Long getAuthenticatedUserId(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null;
        String token = authHeader.substring(7);
        if (!jwtUtil.isTokenValid(token)) return null;
        return jwtUtil.extractUserId(token);
    }

    @PostMapping
    public ResponseEntity<?> sendMessage(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Message message) {

        Long authUserId = getAuthenticatedUserId(authHeader);
        if (authUserId == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authorization header required");
        if (message.getSenderId() == null || message.getReceiverId() == null)
            return ResponseEntity.badRequest().body("senderId and receiverId are required");
        if (!authUserId.equals(message.getSenderId()))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Sender must match the authenticated user");
        if (message.getContent() == null || message.getContent().isBlank())
            return ResponseEntity.badRequest().body("Message content cannot be empty");
        if (!connectionRepo.existsBetweenUsers(message.getSenderId(), message.getReceiverId()))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Messages are allowed only between connected users");

        message.setIsRead(false);
        message.setCreatedAt(LocalDateTime.now());
        return ResponseEntity.ok(messageRepo.save(message));
    }

    @GetMapping("/conversation/{user1}/{user2}")
    public ResponseEntity<?> getConversation(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long user1, @PathVariable Long user2) {

        Long authUserId = getAuthenticatedUserId(authHeader);
        if (authUserId == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authorization header required");
        if (!authUserId.equals(user1) && !authUserId.equals(user2))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Unauthorized access to this conversation");
        if (!connectionRepo.existsBetweenUsers(user1, user2))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Users are not connected");

        List<Message> messages = messageRepo
                .findBySenderIdAndReceiverIdOrSenderIdAndReceiverIdOrderByCreatedAtAsc(
                        user1, user2, user2, user1);
        return ResponseEntity.ok(messages);
    }

    @PutMapping("/read/{senderId}/{receiverId}")
    public ResponseEntity<?> markAsRead(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long senderId, @PathVariable Long receiverId) {

        Long authUserId = getAuthenticatedUserId(authHeader);
        if (authUserId == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authorization header required");
        if (!authUserId.equals(receiverId))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only the receiver may mark messages as read");
        if (!connectionRepo.existsBetweenUsers(senderId, receiverId))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Users are not connected");

        List<Message> unread = messageRepo.findBySenderIdAndReceiverIdAndIsReadFalse(senderId, receiverId);
        unread.forEach(m -> m.setIsRead(true));
        messageRepo.saveAll(unread);
        return ResponseEntity.ok("Marked as read");
    }

    @GetMapping("/unread/{userId}")
    public ResponseEntity<?> getUnreadCount(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long userId) {

        Long authUserId = getAuthenticatedUserId(authHeader);
        if (authUserId == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authorization header required");
        if (!authUserId.equals(userId))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Unauthorized user");

        List<Message> unread = messageRepo.findByReceiverIdAndIsReadFalse(userId);
        return ResponseEntity.ok(Map.of("count", unread.size()));
    }

    // ── Delete conversation ───────────────────────────────────────────────────
    // Deletes all messages in both directions between two users.
    // Only a participant of the conversation may call this.
    @DeleteMapping("/conversation")
    public ResponseEntity<?> deleteConversation(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, Long> body) {

        Long authUserId = getAuthenticatedUserId(authHeader);
        if (authUserId == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authorization header required");

        Long userId  = body.get("userId");
        Long otherId = body.get("otherId");

        if (userId == null || otherId == null)
            return ResponseEntity.badRequest().body("userId and otherId are required");
        if (!authUserId.equals(userId))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Unauthorized");

        // Fetch all messages between the two users and delete them
        List<Message> messages = messageRepo
                .findBySenderIdAndReceiverIdOrSenderIdAndReceiverIdOrderByCreatedAtAsc(
                        userId, otherId, otherId, userId);
        messageRepo.deleteAll(messages);
        return ResponseEntity.ok(Map.of("deleted", messages.size()));
    }
}