package com.jobportal.job_portal.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "jobs")
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(length = 1000)
    private String description;

    private String location;

    private String company;

    private String salary;

    private String type;

    // FIX: added recruiterId so we know which recruiter posted the job.
    // Without this field, there was no way to filter jobs by recruiter,
    // and the /jobs/recruiter/{id} endpoint could not exist.
    private Long recruiterId;

    public Job() {}

    public Job(Long id, String title, String description, String location,
               String company, String salary, String type, Long recruiterId) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.location = location;
        this.company = company;
        this.salary = salary;
        this.type = type;
        this.recruiterId = recruiterId;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }

    public String getSalary() { return salary; }
    public void setSalary(String salary) { this.salary = salary; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Long getRecruiterId() { return recruiterId; }
    public void setRecruiterId(Long recruiterId) { this.recruiterId = recruiterId; }
}