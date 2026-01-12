package com.civicledger.security;

import com.civicledger.entity.Role;
import com.civicledger.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for JWT token generation and validation.
 * Used for mock authentication in dev/demo environments.
 */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    @Value("${civicledger.jwt.secret:}")
    private String jwtSecret;

    @Value("${civicledger.jwt.expiration-minutes:60}")
    private int expirationMinutes;

    @Value("${civicledger.jwt.issuer:civicledger}")
    private String issuer;

    private SecretKey signingKey;

    @PostConstruct
    public void init() {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            // Generate a random key for dev - logs warning
            log.warn("No JWT secret configured - generating random key. This should only happen in development.");
            byte[] keyBytes = new byte[64];
            new java.security.SecureRandom().nextBytes(keyBytes);
            this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        } else {
            // Use configured secret (must be at least 512 bits for HS512)
            byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
            if (keyBytes.length < 64) {
                throw new IllegalStateException("JWT secret must be at least 64 bytes (512 bits) for HS512");
            }
            this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        }
    }

    /**
     * Generate a JWT token for the given user.
     */
    public String generateToken(User user) {
        Instant now = Instant.now();
        Instant expiration = now.plusSeconds(expirationMinutes * 60L);

        Map<String, Object> claims = new HashMap<>();
        claims.put("email", user.getEmail());
        claims.put("name", user.getFullName());
        claims.put("roles", user.getRoles().stream()
                .map(Role::name)
                .collect(Collectors.toList()));

        // Include realm_access for compatibility with Keycloak JWT structure
        Map<String, Object> realmAccess = new HashMap<>();
        realmAccess.put("roles", user.getRoles().stream()
                .map(Role::name)
                .collect(Collectors.toList()));
        claims.put("realm_access", realmAccess);

        return Jwts.builder()
                .issuer(issuer)
                .subject(user.getId().toString())
                .claims(claims)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(signingKey, Jwts.SIG.HS512)
                .compact();
    }

    /**
     * Validate a JWT token and extract claims.
     */
    public Optional<Claims> validateToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(claims);
        } catch (JwtException e) {
            log.debug("Invalid JWT token: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Extract user ID from token.
     */
    public Optional<UUID> extractUserId(String token) {
        return validateToken(token)
                .map(claims -> UUID.fromString(claims.getSubject()));
    }

    /**
     * Extract roles from token.
     */
    @SuppressWarnings("unchecked")
    public Set<Role> extractRoles(String token) {
        return validateToken(token)
                .map(claims -> {
                    List<String> roles = claims.get("roles", List.class);
                    if (roles == null) {
                        return Collections.<Role>emptySet();
                    }
                    return roles.stream()
                            .map(Role::valueOf)
                            .collect(Collectors.toSet());
                })
                .orElse(Collections.emptySet());
    }

    /**
     * Check if token is expired.
     */
    public boolean isTokenExpired(String token) {
        return validateToken(token)
                .map(claims -> claims.getExpiration().before(new Date()))
                .orElse(true);
    }

    /**
     * Get expiration time in minutes.
     */
    public int getExpirationMinutes() {
        return expirationMinutes;
    }
}
