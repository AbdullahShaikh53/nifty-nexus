package com.niftynexus.auth;

import com.niftynexus.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
public class JwtService {
    private final SecretKey key;
    private final int expiryHours;

    public JwtService(AppProperties props) {
        String secret = props.getJwtSecret();
        if (secret == null || secret.length() < 32) throw new IllegalStateException("JWT_SECRET must be at least 32 characters");
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiryHours = props.getJwtExpiryHours();
    }

    public String issue(Long userId, String email, String role) {
        Instant now = Instant.now();
        return Jwts.builder().subject(String.valueOf(userId)).claim("email", email).claim("role", role)
                .issuedAt(Date.from(now)).expiration(Date.from(now.plus(expiryHours, ChronoUnit.HOURS)))
                .signWith(key).compact();
    }

    /** @return claims, or null when the token is invalid/expired. */
    public Claims parse(String token) {
        try { return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload(); }
        catch (Exception e) { return null; }
    }
}
