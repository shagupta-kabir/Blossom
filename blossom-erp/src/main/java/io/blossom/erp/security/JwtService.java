package io.blossom.erp.security;

import io.blossom.erp.domain.AppUser;
import io.blossom.erp.domain.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private final SecretKey key;
    private final long expirationMinutes;

    public JwtService(@Value("${app.jwt.secret:}") String secret,
                      @Value("${app.jwt.expiration-minutes:480}") long expirationMinutes) {
        String effective = secret;
        if (effective == null || effective.isBlank()) {
            // Not configured: use a random key so the app still starts. Everybody is logged out whenever it restarts.
            byte[] random = new byte[48];
            new SecureRandom().nextBytes(random);
            effective = Base64.getEncoder().encodeToString(random);
            log.warn("JWT_SECRET is not set: using a temporary random secret. Logins will reset on every restart. "
                    + "Set JWT_SECRET (32+ characters) for production.");
        } else if (effective.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET is too short. Use a random string of at least 32 characters.");
        }
        this.key = Keys.hmacShaKeyFor(effective.getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = expirationMinutes;
    }

    public String generate(AppUser user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getId())
                .claim("email", user.getEmail())
                .claim("name", user.getFullName())
                .claim("role", user.getRole().name())
                .claim("eid", user.getEmployeeId())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expirationMinutes * 60)))
                .signWith(key)
                .compact();
    }

    /** Throws {@link io.jsonwebtoken.JwtException} / IllegalArgumentException when the token is invalid or expired. */
    public AuthUser parse(String token) {
        Claims c = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        return new AuthUser(c.getSubject(), c.get("email", String.class), c.get("name", String.class),
                Role.valueOf(c.get("role", String.class)), c.get("eid", String.class));
    }

    public long expiresInSeconds() {
        return expirationMinutes * 60;
    }
}
