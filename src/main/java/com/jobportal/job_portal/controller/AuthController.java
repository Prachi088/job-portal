package com.jobportal.job_portal.controller;

import com.jobportal.job_portal.entity.User;
import com.jobportal.job_portal.repository.UserRepository;
import com.jobportal.job_portal.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        try {
            String email    = body.get("email");
            String password = body.get("password");
            String name     = body.get("name");
            String role     = body.get("role");

            if (email == null || email.isBlank())
                return ResponseEntity.badRequest().body("Email is required");
            if (password == null || password.isBlank())
                return ResponseEntity.badRequest().body("Password is required");
            if (name == null || name.isBlank())
                return ResponseEntity.badRequest().body("Name is required");
            if (userRepository.findByEmail(email).isPresent())
                return ResponseEntity.badRequest().body("Email already exists!");

            User user = new User();
            user.setEmail(email);
            user.setName(name);
            user.setRole(role != null ? role : "STUDENT");
            user.setPassword(passwordEncoder.encode(password));

            User savedUser = userRepository.save(user);
            return ResponseEntity.ok(savedUser);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Registration failed: " + e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        try {
            String email    = body.get("email");
            String password = body.get("password");

            if (email == null || email.isBlank())
                return ResponseEntity.badRequest().body("Email is required");
            if (password == null || password.isBlank())
                return ResponseEntity.badRequest().body("Password is required");

            Optional<User> found = userRepository.findByEmail(email);
            if (found.isEmpty())
                return ResponseEntity.badRequest().body("User not found!");
            if (!passwordEncoder.matches(password, found.get().getPassword()))
                return ResponseEntity.badRequest().body("Wrong password!");

            String token = jwtUtil.generateToken(
                    found.get().getEmail(),
                    found.get().getRole(),
                    found.get().getId()
            );
            Map<String, String> response = new HashMap<>();
            response.put("token", token);
            response.put("role",  found.get().getRole());
            response.put("name",  found.get().getName());
            response.put("id",    String.valueOf(found.get().getId()));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Login failed: " + e.getMessage());
        }
    }
}