package com.jobportal.job_portal.controller;

import com.jobportal.job_portal.entity.Connection;
import com.jobportal.job_portal.entity.ConnectionRequest;
import com.jobportal.job_portal.entity.User;
import com.jobportal.job_portal.repository.ConnectionRepository;
import com.jobportal.job_portal.repository.ConnectionRequestRepository;
import com.jobportal.job_portal.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
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

    @Autowired ConnectionRequestRepository requestRepo;
    @Autowired ConnectionRepository        connectionRepo;
    @Autowired UserRepository              userRepo;

    // ── Helper: get the email of the authenticated caller from SecurityContext ─
    // JwtFilter already validated the token and set the principal as the user's
    // email. We look up the User row by email to get their numeric ID.
    // This is more reliable than re-parsing the JWT in the controller because
    // Spring Security has already done the validation work.
    private Long getAuthenticatedUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        String email = (String) auth.getPrincipal();
        return userRepo.findByEmail(email).map(u -> u.getId()).orElse(null);
    }

    // ── Send a connection request ─────────────────────────────────────────────
    @PostMapping("/request")
    public ResponseEntity<?> sendRequest(@RequestBody Map<String, Long> body) {
        Long senderId   = body.get("senderId");
        Long receiverId = body.get("receiverId");

        if (senderId == null || receiverId == null)
            return ResponseEntity.badRequest().body("senderId and receiverId are required");

        if (senderId.equals(receiverId))
            return ResponseEntity.badRequest().body("Cannot send a connection request to yourself");

        // Use bidirectional check to catch connections stored in either column order.
        if (connectionRepo.existsBetweenUsers(senderId, receiverId))
            return ResponseEntity.badRequest().body("Already connected");

        // FIX: Only block on PENDING requests — not on REJECTED or ACCEPTED ones.
        // The old existsBySenderIdAndReceiverId check found any row regardless of
        // status, so a previously REJECTED request permanently prevented the sender
        // from trying again. Now we only block if there is an active PENDING row.
        if (requestRepo.existsBySenderIdAndReceiverIdAndStatus(senderId, receiverId, "PENDING"))
            return ResponseEntity.badRequest().body("Request already sent");

        if (requestRepo.existsBySenderIdAndReceiverIdAndStatus(receiverId, senderId, "PENDING"))
            return ResponseEntity.badRequest().body("A request from this user is already pending");

        ConnectionRequest req = new ConnectionRequest();
        req.setSenderId(senderId);
        req.setReceiverId(receiverId);
        req.setStatus("PENDING");
        req.setCreatedAt(LocalDateTime.now());
        return ResponseEntity.ok(requestRepo.save(req));
    }

    // ── Accept or reject a request ────────────────────────────────────────────
    @PutMapping("/request/{id}")
    public ResponseEntity<?> updateRequest(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        // Spring Security already validated the JWT in JwtFilter before this
        // method is reached. We read the caller's identity from SecurityContext
        // (set by JwtFilter) rather than re-parsing the token, which avoids
        // any clock-skew or re-validation failures.
        Long callerId = getAuthenticatedUserId();
        if (callerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Could not identify the authenticated user");
        }

        String status = body.get("status");
        if (!"ACCEPTED".equals(status) && !"REJECTED".equals(status))
            return ResponseEntity.badRequest().body("Status must be ACCEPTED or REJECTED");

        ConnectionRequest req = requestRepo.findById(id).orElse(null);
        if (req == null) return ResponseEntity.notFound().build();

        // FIX: Ownership guard — only the receiver may accept or reject.
        if (!req.getReceiverId().equals(callerId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Only the receiver of this request may update it");
        }

        req.setStatus(status);
        requestRepo.save(req);

        if ("ACCEPTED".equals(status)) {
            // Bidirectional guard prevents a duplicate Connection row if the
            // endpoint is hit twice (e.g. double-click or retry).
            if (!connectionRepo.existsBetweenUsers(req.getSenderId(), req.getReceiverId())) {
                Connection conn = new Connection();
                conn.setUser1Id(req.getSenderId());
                conn.setUser2Id(req.getReceiverId());
                conn.setConnectedAt(LocalDateTime.now());
                connectionRepo.save(conn);
            }
        }
        return ResponseEntity.ok(req);
    }

    // ── Get pending OUTGOING requests sent BY a user ──────────────────────────
    //
    // IMPORTANT: this mapping (/requests/sent/{userId}) MUST be declared before
    // /requests/{userId} in the source file. Although Spring MVC prioritises
    // literal path segments over variables when dispatching, having the more
    // specific route first avoids any ambiguity in older Spring Boot versions
    // and makes the intent explicit to future readers.
    @GetMapping("/requests/sent/{userId}")
    public ResponseEntity<?> getSentRequests(@PathVariable Long userId) {
        List<ConnectionRequest> requests =
                requestRepo.findBySenderIdAndStatus(userId, "PENDING");

        List<Map<String, Object>> result = new ArrayList<>();
        for (ConnectionRequest req : requests) {
            Map<String, Object> map = new HashMap<>();
            map.put("id",         req.getId());
            map.put("receiverId", req.getReceiverId());
            map.put("status",     req.getStatus());
            map.put("createdAt",  req.getCreatedAt());
            result.add(map);
        }
        return ResponseEntity.ok(result);
    }

    // ── Get pending INCOMING requests for a user (Notifications page) ─────────
    @GetMapping("/requests/{userId}")
    public ResponseEntity<?> getRequests(@PathVariable Long userId) {
        List<ConnectionRequest> requests =
                requestRepo.findByReceiverIdAndStatus(userId, "PENDING");

        List<Map<String, Object>> result = new ArrayList<>();
        for (ConnectionRequest req : requests) {
            User sender = userRepo.findById(req.getSenderId()).orElse(null);
            Map<String, Object> map = new HashMap<>();
            map.put("id",          req.getId());
            map.put("senderId",    req.getSenderId());
            map.put("senderName",  sender != null ? sender.getName()   : "Unknown");
            map.put("senderRole",  sender != null ? sender.getRole()   : "");
            map.put("senderSkills",sender != null ? sender.getSkills() : "");
            map.put("createdAt",   req.getCreatedAt());
            result.add(map);
        }
        return ResponseEntity.ok(result);
    }

    // ── Get all connections for a user (Connections page + chat) ─────────────
    @GetMapping("/{userId}")
    public ResponseEntity<?> getConnections(@PathVariable Long userId) {
        List<Connection> connections =
                connectionRepo.findByUser1IdOrUser2Id(userId, userId);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Connection conn : connections) {
            Long otherId = conn.getUser1Id().equals(userId)
                    ? conn.getUser2Id() : conn.getUser1Id();
            User other = userRepo.findById(otherId).orElse(null);
            if (other == null) continue;

            Map<String, Object> map = new HashMap<>();
            map.put("connectionId", conn.getId());
            map.put("userId",       other.getId());   // used by frontend for /chat/:userId
            map.put("name",         other.getName());
            map.put("role",         other.getRole());
            map.put("skills",       other.getSkills());
            map.put("company",      other.getCompany());
            map.put("connectedAt",  conn.getConnectedAt());
            result.add(map);
        }
        return ResponseEntity.ok(result);
    }

    // ── List all users for the Connect/Discover page ──────────────────────────
    @GetMapping("/users/all")
    public ResponseEntity<?> getAllUsers() {
        List<User> users = userRepo.findAll();
        List<Map<String, Object>> result = new ArrayList<>();
        for (User u : users) {
            Map<String, Object> map = new HashMap<>();
            map.put("id",          u.getId());
            map.put("name",        u.getName());
            map.put("role",        u.getRole());
            map.put("skills",      u.getSkills());
            map.put("company",     u.getCompany());
            map.put("education",   u.getEducation());
            map.put("currentRole", u.getCurrentRole());
            result.add(map);
        }
        return ResponseEntity.ok(result);
    }
}