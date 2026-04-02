package com.jobportal.job_portal.controller;

import com.jobportal.job_portal.entity.Application;
import com.jobportal.job_portal.repository.ApplicationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/applications")
@CrossOrigin(origins = "*")
public class ApplicationController {

    @Autowired
    private ApplicationRepository applicationRepository;

    @PostMapping
    public Application applyForJob(@RequestBody Application application) {
        application.setStatus("APPLIED");
        return applicationRepository.save(application);
    }

    @GetMapping("/user/{userId}")
    public List<Application> getApplicationsByUser(@PathVariable Long userId) {
        return applicationRepository.findByUserId(userId);
    }

    @GetMapping("/job/{jobId}")
    public List<Application> getApplicationsByJob(@PathVariable Long jobId) {
        return applicationRepository.findByJobId(jobId);
    }
}