package com.jobportal.job_portal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.jobportal.job_portal")
public class JobPortalApplication {
	public static void main(String[] args) {
		SpringApplication.run(JobPortalApplication.class, args);
	}
}