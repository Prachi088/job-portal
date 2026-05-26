package com.jobportal.job_portal.controller;

import com.jobportal.job_portal.entity.Message;
import com.jobportal.job_portal.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
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

    @PostMapping
    public ResponseEntity<?> sendMessage(@RequestBody Message message) {
        // FIX: validate required fields before saving
        if (message.getSenderId() == null || message.getReceiverId() == null) {
            return ResponseEntity.badRequest().body("senderId and receiverId are required");
        }
        if (message.getContent() == null || message.getContent().isBlank()) {
            return ResponseEntity.badRequest().body("Message content cannot be empty");
        }
        message.setIsRead(false);
        message.setCreatedAt(LocalDateTime.now());
        return ResponseEntity.ok(messageRepo.save(message));
    }

    @GetMapping("/conversation/{user1}/{user2}")
    public ResponseEntity<?> getConversation(
            @PathVariable Long user1, @PathVariable Long user2) {
        List<Message> messages = messageRepo
                .findBySenderIdAndReceiverIdOrSenderIdAndReceiverIdOrderByCreatedAtAsc(
                        user1, user2, user2, user1);
        return ResponseEntity.ok(messages);
    }

    // FIX: the original implementation fetched ALL unread messages for the
    // receiver and then filtered by senderId in Java memory. This is
    // inefficient and breaks at scale. We now use a dedicated repository
    // method that adds the senderId filter at the SQL level.
    @PutMapping("/read/{senderId}/{receiverId}")
    public ResponseEntity<?> markAsRead(
            @PathVariable Long senderId, @PathVariable Long receiverId) {
        List<Message> unread = messageRepo
                .findBySenderIdAndReceiverIdAndIsReadFalse(senderId, receiverId);
        unread.forEach(m -> m.setIsRead(true));
        messageRepo.saveAll(unread);
        return ResponseEntity.ok("Marked as read");
    }

    @GetMapping("/unread/{userId}")
    public ResponseEntity<?> getUnreadCount(@PathVariable Long userId) {
        List<Message> unread = messageRepo.findByReceiverIdAndIsReadFalse(userId);
        return ResponseEntity.ok(Map.of("count", unread.size()));
    }
}