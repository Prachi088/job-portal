package com.jobportal.job_portal.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "applications")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private Long jobId;

    private String status; // APPLIED, ACCEPTED, REJECTED
}