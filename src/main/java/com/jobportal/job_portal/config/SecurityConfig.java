package com.jobportal.job_portal.config;

import com.jobportal.job_portal.security.JwtFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtFilter jwtFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // allowCredentials must be false when using wildcard origins.
        // Since we use JWT in Authorization header (not cookies), we don't
        // need credentials=true at all.
        config.setAllowCredentials(false);

        // AWS production origins only
        config.setAllowedOrigins(List.of(
                "http://job-portal-frontend-prach.s3-website.ap-south-1.amazonaws.com"
        ));

        config.setAllowedHeaders(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setExposedHeaders(List.of("Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                // Without an AuthenticationEntryPoint, Spring Security returns 403
                // (not 401) for requests where no authentication is set — including
                // expired JWTs, because JwtFilter silently skips them.
                // This makes the frontend's response interceptor correctly distinguish
                // "token expired / not logged in" (401) from "authenticated but forbidden" (403).
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\":\"Unauthorized - token missing or expired\"}");
                        })
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/health").permitAll()
                        .requestMatchers("/chat").permitAll()

                        .requestMatchers(HttpMethod.GET, "/api/users").permitAll()
                        .requestMatchers(HttpMethod.GET, "/jobs").permitAll()
                        .requestMatchers(HttpMethod.GET, "/jobs/search").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/events").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/connections/users/all").permitAll()

                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}