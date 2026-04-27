package com.jobportal.job_portal.controller;

import com.jobportal.job_portal.entity.Event;
import com.jobportal.job_portal.entity.EventApplication;
import com.jobportal.job_portal.repository.EventRepository;
import com.jobportal.job_portal.repository.EventApplicationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jobportal.job_portal.entity.User;
import com.jobportal.job_portal.repository.UserRepository;

@RestController
@RequestMapping("/api/events")
@CrossOrigin(origins = "*")
public class EventController {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
private EventApplicationRepository eventApplicationRepository;

@Autowired
private UserRepository userRepository;

    @PostMapping
    public ResponseEntity<?> createEvent(@RequestBody Event event) {
        try {
           if (event.getEventDate() == null) {
                 event.setEventDate(LocalDateTime.now());
}            Event savedEvent = eventRepository.save(event);
            return ResponseEntity.ok(savedEvent);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error creating event: " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<Event>> getAllEvents() {
        try {
            List<Event> events = eventRepository.findAll();
            return ResponseEntity.ok(events);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/recruiter/{recruiterId}")
    public ResponseEntity<List<Event>> getEventsByRecruiter(@PathVariable Long recruiterId) {
        try {
            List<Event> events = eventRepository.findByRecruiterId(recruiterId);
            return ResponseEntity.ok(events);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getEventById(@PathVariable Long id) {
        try {
            Event event = eventRepository.findById(id).orElse(null);
            if (event == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(event);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error fetching event: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteEvent(@PathVariable Long id) {
        try {
            if (!eventRepository.existsById(id)) {
                return ResponseEntity.notFound().build();
            }
            eventRepository.deleteById(id);
            return ResponseEntity.ok("Event deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error deleting event: " + e.getMessage());
        }
    }

    @PostMapping("/{eventId}/register")
    public ResponseEntity<?> registerForEvent(@PathVariable Long eventId, @RequestBody Map<String, Long> request) {
        try {
            Long userId = request.get("userId");
            if (userId == null) {
                return ResponseEntity.badRequest().body("User ID is required");
            }

            // Check if already registered
            if (eventApplicationRepository.existsByEventIdAndUserId(eventId, userId)) {
                return ResponseEntity.badRequest().body("Already registered for this event");
            }

            EventApplication application = new EventApplication();
            application.setEventId(eventId);
            application.setUserId(userId);
            application.setAppliedAt(LocalDateTime.now());
            application.setStatus("REGISTERED");

            EventApplication savedApplication = eventApplicationRepository.save(application);
            return ResponseEntity.ok(savedApplication);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error registering for event: " + e.getMessage());
        }
    }

   @GetMapping("/{eventId}/applications")
public ResponseEntity<?> getEventApplications(@PathVariable Long eventId) {
    try {
        List<EventApplication> applications = eventApplicationRepository.findByEventId(eventId);
        List<Map<String, Object>> result = new ArrayList<>();

        for (EventApplication app : applications) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("id", app.getId());
            entry.put("eventId", app.getEventId());
            entry.put("userId", app.getUserId());
            entry.put("appliedAt", app.getAppliedAt());
            entry.put("status", app.getStatus());

            // Attach user details
            User user = userRepository.findById(app.getUserId()).orElse(null);
            if (user != null) {
                Map<String, Object> userMap = new HashMap<>();
                userMap.put("id", user.getId());
                userMap.put("name", user.getName());
                userMap.put("email", user.getEmail());
                userMap.put("phone", user.getPhone());
                userMap.put("skills", user.getSkills());
                entry.put("user", userMap);
            }

            result.add(entry);
        }

        return ResponseEntity.ok(result);
    } catch (Exception e) {
        return ResponseEntity.badRequest().body("Error fetching applications: " + e.getMessage());
    }
}

    @GetMapping("/user/{userId}/applications")
    public ResponseEntity<List<EventApplication>> getUserEventApplications(@PathVariable Long userId) {
        try {
            List<EventApplication> applications = eventApplicationRepository.findByUserId(userId);
            return ResponseEntity.ok(applications);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PutMapping("/applications/{applicationId}/status")
    public ResponseEntity<?> updateApplicationStatus(@PathVariable Long applicationId, @RequestBody Map<String, String> request) {
        try {
            String status = request.get("status");
            if (status == null || (!status.equals("REGISTERED") && !status.equals("ATTENDED") && !status.equals("CANCELLED"))) {
                return ResponseEntity.badRequest().body("Invalid status. Must be REGISTERED, ATTENDED, or CANCELLED");
            }

            EventApplication application = eventApplicationRepository.findById(applicationId).orElse(null);
            if (application == null) {
                return ResponseEntity.notFound().build();
            }

            application.setStatus(status);
            EventApplication updatedApplication = eventApplicationRepository.save(application);
            return ResponseEntity.ok(updatedApplication);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error updating application status: " + e.getMessage());
        }
    }
}