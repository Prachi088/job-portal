package com.jobportal.job_portal.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    // FIX: inconsistent indentation of @Value annotations cleaned up.
    // Both fields are now consistently indented at the class level.
    @Value("${jwt.secret}")
    private String SECRET;

    @Value("${jwt.expiration}")
    private long expiration;

    private Key getSigningKey() {
        // FIX: HMAC-SHA256 requires a key of at least 256 bits (32 bytes).
        // If jwt.secret is too short the library throws a WeakKeyException at
        // startup. The fix is enforced in configuration (application.properties),
        // but we add a runtime guard here as a safety net.
        if (SECRET == null || SECRET.length() < 32) {
            throw new IllegalStateException(
                    "jwt.secret must be at least 32 characters long for HS256");
        }
        return Keys.hmacShaKeyFor(SECRET.getBytes());
    }

    public String generateToken(String email, String role, Long userId) {
        return Jwts.builder()
                .setSubject(email)
                .claim("role", role)
                .claim("id", userId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractEmail(String token) {
        return getClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return (String) getClaims(token).get("role");
    }

    public Long extractUserId(String token) {
        // FIX: extractUserId was never exposed despite the claim being stored.
        // Added so controllers can retrieve the authenticated user's ID from
        // the token via JwtUtil instead of parsing it manually elsewhere.
        Object id = getClaims(token).get("id");
        if (id instanceof Integer) return ((Integer) id).longValue();
        if (id instanceof Long) return (Long) id;
        return null;
    }

    public boolean isTokenValid(String token) {
        try {
            getClaims(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }

    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}