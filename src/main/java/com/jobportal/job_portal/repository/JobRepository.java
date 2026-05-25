package com.jobportal.job_portal.repository;

import com.jobportal.job_portal.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface JobRepository extends JpaRepository<Job, Long> {
    List<Job> findByTitleContainingIgnoreCase(String title);

    // FIX: added so the recruiter dashboard can fetch only its own jobs.
    // Spring Data JPA auto-implements this from the method name.
    List<Job> findByRecruiterId(Long recruiterId);
}