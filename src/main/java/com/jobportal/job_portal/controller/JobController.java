package com.jobportal.job_portal.controller;

import com.jobportal.job_portal.entity.Job;
import com.jobportal.job_portal.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/jobs")
@CrossOrigin(origins = "*")
public class JobController {

    @Autowired
    private JobRepository jobRepository;

    @PostMapping
    public ResponseEntity<?> createJob(@RequestBody Job job) {
        try {
            Job saved = jobRepository.save(job);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to create job: " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllJobs() {
        try {
            List<Job> jobs = jobRepository.findAll();
            return ResponseEntity.ok(jobs);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to fetch jobs: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getJobById(@PathVariable Long id) {
        try {
            Job job = jobRepository.findById(id).orElse(null);
            if (job == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(job);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to fetch job: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteJob(@PathVariable Long id) {
        try {
            if (!jobRepository.existsById(id)) {
                return ResponseEntity.notFound().build();
            }
            jobRepository.deleteById(id);
            return ResponseEntity.ok("Job deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to delete job: " + e.getMessage());
        }
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchJobs(@RequestParam String title) {
        try {
            List<Job> jobs = jobRepository.findByTitleContainingIgnoreCase(title);
            return ResponseEntity.ok(jobs);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to search jobs: " + e.getMessage());
        }
    }
}