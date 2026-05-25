package com.jobportal.job_portal.controller;

import com.jobportal.job_portal.entity.Application;
import com.jobportal.job_portal.entity.User;
import com.jobportal.job_portal.repository.ApplicationRepository;
import com.jobportal.job_portal.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/applications")
@CrossOrigin(origins = "*")
public class ApplicationController {

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private UserRepository userRepository;

    @PostMapping
    public ResponseEntity<?> applyForJob(@RequestBody Application application) {
        try {
            application.setStatus("APPLIED");
            Application saved = applicationRepository.save(application);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to apply for job: " + e.getMessage());
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getApplicationsByUser(@PathVariable Long userId) {
        try {
            List<Application> applications = applicationRepository.findByUserId(userId);
            return ResponseEntity.ok(applications);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to fetch applications: " + e.getMessage());
        }
    }

    @GetMapping("/job/{jobId}")
    public ResponseEntity<?> getApplicationsByJob(@PathVariable Long jobId) {
        try {
            List<Application> applications = applicationRepository.findByJobId(jobId);

            // FIX: Map.of() throws NullPointerException if ANY value is null.
            // Switched to HashMap which allows null values, so optional user
            // profile fields (phone, skills, etc.) don't crash the endpoint.
            List<Map<String, Object>> applicationsWithUsers = applications.stream().map(app -> {
                User user = userRepository.findById(app.getUserId()).orElse(null);

                Map<String, Object> userMap = new HashMap<>();
                if (user != null) {
                    userMap.put("id",             user.getId());
                    userMap.put("name",           user.getName());
                    userMap.put("email",          user.getEmail());
                    userMap.put("phone",          user.getPhone());
                    userMap.put("skills",         user.getSkills());
                    userMap.put("experience",     user.getExperience());
                    userMap.put("education",      user.getEducation());
                    userMap.put("resumeFileName", user.getResumeFileName());
                }

                Map<String, Object> result = new HashMap<>();
                result.put("id",     app.getId());
                result.put("jobId",  app.getJobId());
                result.put("userId", app.getUserId());
                result.put("status", app.getStatus());
                result.put("user",   user != null ? userMap : null);
                return result;

            }).collect(Collectors.toList());

            return ResponseEntity.ok(applicationsWithUsers);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to fetch applications: " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllApplications() {
        try {
            List<Application> applications = applicationRepository.findAll();
            return ResponseEntity.ok(applications);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to fetch applications: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateApplicationStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        try {
            String status = request.get("status");
            Application application = applicationRepository.findById(id).orElse(null);
            if (application == null) {
                return ResponseEntity.notFound().build();
            }
            application.setStatus(status);
            Application updated = applicationRepository.save(application);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to update application status: " + e.getMessage());
        }
    }
}