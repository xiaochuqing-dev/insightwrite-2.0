package com.insightwrite.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilSecurityTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    @Test
    void constructorRequiresExternalStrongSecretWithoutFallback() throws Exception {
        Constructor<?> constructor = jwtUtilClass().getConstructor(String.class);
        Value annotation = constructor.getParameters()[0].getAnnotation(Value.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).isEqualTo("${insightwrite.jwt.secret}");
        assertThatThrownBy(() -> newJwtUtil("short-secret"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT secret");
    }

    @Test
    void generatedTokensContainIssuerAudienceJtiAndTokenVersion() throws Exception {
        Object jwtUtil = newJwtUtil(SECRET);
        Object user = user(12, "scoped@example.com", 4);

        String token = (String) jwtUtilClass()
                .getMethod("generateToken", userClass())
                .invoke(jwtUtil, user);
        Claims claims = parseRawClaims(token);

        assertThat(claims.getIssuer()).isEqualTo("insightwrite");
        assertThat(claims.getAudience()).containsExactly("insightwrite-api");
        assertThat(claims.getId()).isNotBlank();
        assertThat(claims.getIssuedAt()).isNotNull();
        assertThat(claims.getSubject()).isEqualTo("12");
        assertThat(claims.get("email", String.class)).isEqualTo("scoped@example.com");
        assertThat(claims.get("tv", Integer.class)).isEqualTo(4);
    }

    @Test
    void validateTokenRejectsMismatchedCurrentUserVersion() throws Exception {
        Object jwtUtil = newJwtUtil(SECRET);
        String token = (String) jwtUtilClass()
                .getMethod("generateToken", userClass())
                .invoke(jwtUtil, user(12, "scoped@example.com", 4));

        boolean valid = (boolean) jwtUtilClass()
                .getMethod("validateToken", String.class, userClass())
                .invoke(jwtUtil, token, user(12, "scoped@example.com", 5));

        assertThat(valid).isFalse();
    }

    @Test
    void validateTokenRejectsLegacyTokensWithoutRequiredScopeClaims() throws Exception {
        Object jwtUtil = newJwtUtil(SECRET);
        Date now = new Date();
        String legacyToken = Jwts.builder()
                .subject("12")
                .claim("email", "scoped@example.com")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + 60_000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();

        boolean valid = (boolean) jwtUtilClass()
                .getMethod("validateToken", String.class, userClass())
                .invoke(jwtUtil, legacyToken, user(12, "scoped@example.com", 4));

        assertThat(valid).isFalse();
    }

    private Claims parseRawClaims(String token) {
        return Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Object newJwtUtil(String secret) throws Exception {
        try {
            return jwtUtilClass().getConstructor(String.class).newInstance(secret);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw e;
        }
    }

    private Object user(Integer id, String email, Integer tokenVersion) throws Exception {
        Object user = userClass().getConstructor().newInstance();
        userClass().getMethod("setId", Integer.class).invoke(user, id);
        userClass().getMethod("setEmail", String.class).invoke(user, email);
        userClass().getMethod("setTokenVersion", Integer.class).invoke(user, tokenVersion);
        return user;
    }

    private Class<?> jwtUtilClass() throws ClassNotFoundException {
        return Class.forName("com.insightwrite.util.JwtUtil");
    }

    private Class<?> userClass() throws ClassNotFoundException {
        return Class.forName("com.insightwrite.entity.User");
    }
}
