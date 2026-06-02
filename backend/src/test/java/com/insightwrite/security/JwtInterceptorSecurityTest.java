package com.insightwrite.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtInterceptorSecurityTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    @Test
    void rejectsTokenWhenCurrentUserTokenVersionChanged() throws Exception {
        Object jwtUtil = jwtUtilClass().getConstructor(String.class).newInstance(SECRET);
        String token = (String) jwtUtilClass()
                .getMethod("generateToken", userClass())
                .invoke(jwtUtil, user(12, "scoped@example.com", 4));
        Class<?> userRepoClass = Class.forName("com.insightwrite.repository.UserRepository");
        Object userRepository = mock(userRepoClass);
        when(userRepoClass.getMethod("findById", Object.class).invoke(userRepository, 12))
                .thenReturn(Optional.of(user(12, "scoped@example.com", 5)));
        Object interceptor = interceptorClass()
                .getConstructor(jwtUtilClass(), userRepoClass)
                .newInstance(jwtUtil, userRepository);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("Authorization", "Bearer " + token);

        boolean allowed = (boolean) interceptorClass()
                .getMethod("preHandle", jakarta.servlet.http.HttpServletRequest.class,
                        jakarta.servlet.http.HttpServletResponse.class, Object.class)
                .invoke(interceptor, request, response, new Object());

        assertThat(allowed).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
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

    private Class<?> interceptorClass() throws ClassNotFoundException {
        return Class.forName("com.insightwrite.config.JwtInterceptor");
    }
}
