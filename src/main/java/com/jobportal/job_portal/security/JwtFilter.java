package com.jobportal.job_portal.security;

import com.jobportal.job_portal.entity.User;
import com.jobportal.job_portal.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
public class JwtFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (jwtUtil.isTokenValid(token)) {
                String email = jwtUtil.extractEmail(token);
                String role  = jwtUtil.extractRole(token);
                Long userId  = jwtUtil.extractUserId(token);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                email, null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + role))
                        );
                SecurityContextHolder.getContext().setAuthentication(authentication);

                // ── Update lastSeenAt on every authenticated request ──────────
                // Throttle to once per minute to avoid a DB write on every poll.
                if (userId != null) {
                    Optional<User> userOpt = userRepository.findById(userId);
                    userOpt.ifPresent(user -> {
                        LocalDateTime now  = LocalDateTime.now();
                        LocalDateTime last = user.getLastSeenAt();
                        // Only write if never set, or more than 60 seconds ago
                        if (last == null || last.isBefore(now.minusSeconds(60))) {
                            user.setLastSeenAt(now);
                            userRepository.save(user);
                        }
                    });
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}