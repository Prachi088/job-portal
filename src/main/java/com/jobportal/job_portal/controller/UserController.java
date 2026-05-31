package com.jobportal.job_portal.controller;

import com.jobportal.job_portal.entity.User;
import com.jobportal.job_portal.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody User user) {
        try {
            User saved = userRepository.save(user);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to create user: " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllUsers() {
        try {
            List<User> users = userRepository.findAll();
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to fetch users: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        try {
            User user = userRepository.findById(id).orElse(null);
            if (user == null) return ResponseEntity.notFound().build();
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to fetch user: " + e.getMessage());
        }
    }

    // ── Presence endpoint ─────────────────────────────────────────────────────
    // Returns { isOnline, lastSeenAt } for a given user.
    // isOnline = true when lastSeenAt is within the last 2 minutes.
    // Called by the frontend every 30 seconds to update the chat header status.
    @GetMapping("/{id}/presence")
    public ResponseEntity<?> getUserPresence(@PathVariable Long id) {
        try {
            User user = userRepository.findById(id).orElse(null);
            if (user == null) return ResponseEntity.notFound().build();

            LocalDateTime lastSeen = user.getLastSeenAt();
            boolean isOnline = lastSeen != null &&
                    lastSeen.isAfter(LocalDateTime.now().minusMinutes(2));

            return ResponseEntity.ok(Map.of(
                    "isOnline",    isOnline,
                    "lastSeenAt",  lastSeen != null ? lastSeen.toString() : ""
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to fetch presence: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/profile")
    public ResponseEntity<?> updateProfile(@PathVariable Long id, @RequestBody User profileData) {
        try {
            User user = userRepository.findById(id).orElse(null);
            if (user == null) return ResponseEntity.notFound().build();

            if (profileData.getPhone() != null)       user.setPhone(profileData.getPhone());
            if (profileData.getAddress() != null)     user.setAddress(profileData.getAddress());
            if (profileData.getSkills() != null)      user.setSkills(profileData.getSkills());
            if (profileData.getExperience() != null)  user.setExperience(profileData.getExperience());
            if (profileData.getEducation() != null)   user.setEducation(profileData.getEducation());
            if (profileData.getCompany() != null)     user.setCompany(profileData.getCompany());
            if (profileData.getCurrentRole() != null) user.setCurrentRole(profileData.getCurrentRole());
            if (profileData.getLinkedinUrl() != null) user.setLinkedinUrl(profileData.getLinkedinUrl());
            if (profileData.getWebsite() != null)     user.setWebsite(profileData.getWebsite());
            if (profileData.getBio() != null)         user.setBio(profileData.getBio());
            if (profileData.getProjects() != null)    user.setProjects(profileData.getProjects());
            if (profileData.getBatch() != null)       user.setBatch(profileData.getBatch());

            User updatedUser = userRepository.save(user);
            return ResponseEntity.ok(updatedUser);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to update profile: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/resume")
    public ResponseEntity<?> uploadResume(@PathVariable Long id, @RequestParam("resume") MultipartFile file) {
        try {
            User user = userRepository.findById(id).orElse(null);
            if (user == null) return ResponseEntity.notFound().build();
            if (file.isEmpty()) return ResponseEntity.badRequest().body("Resume file is required");

            String base64Data = Base64.getEncoder().encodeToString(file.getBytes());
            user.setResumeFileName(file.getOriginalFilename());
            user.setResumeFilePath("db");
            user.setResumeData(base64Data);
            userRepository.save(user);
            return ResponseEntity.ok(Map.of(
                    "message",  "Resume uploaded successfully",
                    "fileName", file.getOriginalFilename()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to upload resume: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/resume")
    public ResponseEntity<?> downloadResume(@PathVariable Long id) {
        try {
            User user = userRepository.findById(id).orElse(null);
            if (user == null || user.getResumeData() == null) return ResponseEntity.notFound().build();

            byte[] fileContent = Base64.getDecoder().decode(user.getResumeData());
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"" + user.getResumeFileName() + "\"")
                    .body(fileContent);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to download resume: " + e.getMessage());
        }
    }
}