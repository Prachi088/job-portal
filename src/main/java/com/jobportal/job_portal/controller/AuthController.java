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
    public ResponseEntity<?> register(@RequestBody User user) {
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body("Email already exists!");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);
        return ResponseEntity.ok("User registered successfully!");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User user) {
        Optional<User> found = userRepository.findByEmail(user.getEmail());
        if (found.isEmpty()) {
            return ResponseEntity.badRequest().body("User not found!");
        }
        if (!passwordEncoder.matches(user.getPassword(), found.get().getPassword())) {
            return ResponseEntity.badRequest().body("Wrong password!");
        }
        String token = jwtUtil.generateToken(found.get().getEmail(), found.get().getRole());
        Map<String, String> response = new HashMap<>();
        response.put("token", token);
        response.put("role", found.get().getRole());
        response.put("name", found.get().getName());
        return ResponseEntity.ok(response);
    }
}