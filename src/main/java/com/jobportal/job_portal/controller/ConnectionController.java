package com.jobportal.job_portal.controller;

import com.jobportal.job_portal.entity.Connection;
import com.jobportal.job_portal.entity.ConnectionRequest;
import com.jobportal.job_portal.entity.User;
import com.jobportal.job_portal.repository.ConnectionRepository;
import com.jobportal.job_portal.repository.ConnectionRequestRepository;
import com.jobportal.job_portal.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/connections")
@CrossOrigin(origins = "*")
public class ConnectionController {

    @Autowired
    ConnectionRequestRepository requestRepo;
    @Autowired
    ConnectionRepository connectionRepo;
    @Autowired
    UserRepository userRepo;

    // Send connection request
    @PostMapping("/request")
    public ResponseEntity<?> sendRequest(@RequestBody Map<String, Long> body) {
        Long senderId = body.get("senderId");
        Long receiverId = body.get("receiverId");
        if (requestRepo.existsBySenderIdAndReceiverId(senderId, receiverId))
            return ResponseEntity.badRequest().body("Request already sent");
        ConnectionRequest req = new ConnectionRequest();
        req.setSenderId(senderId);
        req.setReceiverId(receiverId);
        req.setStatus("PENDING");
        req.setCreatedAt(LocalDateTime.now());
        return ResponseEntity.ok(requestRepo.save(req));
    }

    // Get incoming requests
    @GetMapping("/requests/{userId}")
    public ResponseEntity<?> getRequests(@PathVariable Long userId) {
        List<ConnectionRequest> requests = requestRepo
            .findByReceiverIdAndStatus(userId, "PENDING");
        List<Map<String, Object>> result = new ArrayList<>();
        for (ConnectionRequest req : requests) {
            User sender = userRepo.findById(req.getSenderId()).orElse(null);
            Map<String, Object> map = new HashMap<>();
            map.put("id", req.getId());
            map.put("senderId", req.getSenderId());
            map.put("senderName", sender != null ? sender.getName() : "Unknown");
            map.put("senderRole", sender != null ? sender.getRole() : "");
            map.put("senderSkills", sender != null ? sender.getSkills() : "");
            map.put("createdAt", req.getCreatedAt());
            result.add(map);
        }
        return ResponseEntity.ok(result);
    }

    // Accept or reject request
    @PutMapping("/request/{id}")
    public ResponseEntity<?> updateRequest(@PathVariable Long id,
                                           @RequestBody Map<String, String> body) {
        String status = body.get("status"); // ACCEPTED or REJECTED
        ConnectionRequest req = requestRepo.findById(id).orElse(null);
        if (req == null) return ResponseEntity.notFound().build();
        req.setStatus(status);
        requestRepo.save(req);
        if ("ACCEPTED".equals(status)) {
            Connection conn = new Connection();
            conn.setUser1Id(req.getSenderId());
            conn.setUser2Id(req.getReceiverId());
            conn.setConnectedAt(LocalDateTime.now());
            connectionRepo.save(conn);
        }
        return ResponseEntity.ok(req);
    }

    // Get all connections for a user
    @GetMapping("/{userId}")
    public ResponseEntity<?> getConnections(@PathVariable Long userId) {
        List<Connection> connections = connectionRepo
            .findByUser1IdOrUser2Id(userId, userId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Connection conn : connections) {
            Long otherId = conn.getUser1Id().equals(userId)
                ? conn.getUser2Id() : conn.getUser1Id();
            User other = userRepo.findById(otherId).orElse(null);
            if (other == null) continue;
            Map<String, Object> map = new HashMap<>();
            map.put("connectionId", conn.getId());
            map.put("userId", other.getId());
            map.put("name", other.getName());
            map.put("role", other.getRole());
            map.put("skills", other.getSkills());
            map.put("company", other.getCompany());
            map.put("connectedAt", conn.getConnectedAt());
            result.add(map);
        }
        return ResponseEntity.ok(result);
    }

    // Get all users (for alumni/connect page)
    @GetMapping("/users/all")
    public ResponseEntity<?> getAllUsers() {
        List<User> users = userRepo.findAll();
        List<Map<String, Object>> result = new ArrayList<>();
        for (User u : users) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", u.getId());
            map.put("name", u.getName());
            map.put("role", u.getRole());
            map.put("skills", u.getSkills());
            map.put("company", u.getCompany());
            map.put("education", u.getEducation());
            map.put("currentRole", u.getCurrentRole());
            result.add(map);
        }
        return ResponseEntity.ok(result);
    }
}