package com.insightwrite.util;

import com.insightwrite.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

@Component
public class JwtUtil {

    private static final String ISSUER = "insightwrite";
    private static final String AUDIENCE = "insightwrite-api";
    private static final int MIN_SECRET_BYTES = 32;
    private static final long EXPIRATION_MS = 7 * 24 * 60 * 60 * 1000L;

    private final SecretKey key;

    public JwtUtil(@Value("${insightwrite.jwt.secret}") String secret) {
        if (secret == null || secret.isBlank()
                || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("JWT secret must be configured and at least 32 bytes");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Integer userId, String email) {
        return generateToken(userId, email, 0);
    }

    public String generateToken(User user) {
        return generateToken(user.getId(), user.getEmail(), tokenVersion(user.getTokenVersion()));
    }

    public String generateToken(Integer userId, String email, Integer tokenVersion) {
        Date now = new Date();
        return Jwts.builder()
                .issuer(ISSUER)
                .subject(String.valueOf(userId))
                .audience().add(AUDIENCE).and()
                .claim("email", email)
                .claim("tv", tokenVersion(tokenVersion))
                .issuedAt(now)
                .id(UUID.randomUUID().toString())
                .expiration(new Date(now.getTime() + EXPIRATION_MS))
                .signWith(key)
                .compact();
    }

    public Integer getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return Integer.valueOf(claims.getSubject());
    }

    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public boolean validateToken(String token, User currentUser) {
        if (currentUser == null) {
            return false;
        }
        try {
            Claims claims = parseToken(token);
            Integer subjectUserId = Integer.valueOf(claims.getSubject());
            Integer tokenVersion = claims.get("tv", Integer.class);
            return Objects.equals(subjectUserId, currentUser.getId())
                    && Objects.equals(claims.get("email", String.class), currentUser.getEmail())
                    && Objects.equals(tokenVersion(tokenVersion), tokenVersion(currentUser.getTokenVersion()))
                    && claims.getIssuedAt() != null
                    && claims.getId() != null
                    && !claims.getId().isBlank();
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(ISSUER)
                .requireAudience(AUDIENCE)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Integer tokenVersion(Integer tokenVersion) {
        return tokenVersion != null ? tokenVersion : 0;
    }
}
