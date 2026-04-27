package com.jobportal.job_portal.controller;

import com.jobportal.job_portal.service.GroqService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/chat")
@CrossOrigin(origins = "*")
public class ChatController {

    private final GroqService groqService;

    public ChatController(GroqService groqService) {
        this.groqService = groqService;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> chat(@RequestBody Map<String, String> request) {
        try {
            String message = request.get("message");

            if (message == null || message.isBlank()) {
                return ResponseEntity
                        .badRequest()
                        .body(Map.of("error", "Message cannot be empty"));
            }

            String reply = groqService.getResponse(message);

            return ResponseEntity.ok(Map.of("reply", reply));
        } catch (Exception e) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of("error", "Chat failed: " + e.getMessage()));
        }
    }
}