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

    @PutMapping("/read/{senderId}/{receiverId}")
    public ResponseEntity<?> markAsRead(
            @PathVariable Long senderId, @PathVariable Long receiverId) {
        List<Message> unread = messageRepo.findByReceiverIdAndIsReadFalse(receiverId);
        unread.stream()
            .filter(m -> m.getSenderId().equals(senderId))
            .forEach(m -> m.setIsRead(true));
        messageRepo.saveAll(unread);
        return ResponseEntity.ok("Marked as read");
    }

    @GetMapping("/unread/{userId}")
    public ResponseEntity<?> getUnreadCount(@PathVariable Long userId) {
        List<Message> unread = messageRepo.findByReceiverIdAndIsReadFalse(userId);
        return ResponseEntity.ok(Map.of("count", unread.size()));
    }
}