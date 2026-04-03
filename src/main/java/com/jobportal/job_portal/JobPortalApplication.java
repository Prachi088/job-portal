package com.jobportal.job_portal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(
		scanBasePackages = "com.jobportal.job_portal",
		exclude = {UserDetailsServiceAutoConfiguration.class}
)
public class JobPortalApplication {
	public static void main(String[] args) {
		SpringApplication.run(JobPortalApplication.class, args);
	}
}