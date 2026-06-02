package com.insightwrite.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthServiceSecurityTest {

    @Test
    void sendCodeDoesNotRevealRegisteredEmailForRegisterPurpose() throws Exception {
        Class<?> userRepoClass = Class.forName("com.insightwrite.repository.UserRepository");
        Object userRepository = mock(userRepoClass);
        when(userRepoClass.getMethod("existsByEmail", String.class).invoke(userRepository, "used@example.com"))
                .thenReturn(true);
        Object service = buildAuthService(userRepository, null);

        assertThat(invokeBoolean(service, "shouldSendVerificationCode", "used@example.com", "register", null))
                .isFalse();
    }

    @Test
    void sendCodeAllowsRegisteredEmailForPasswordResetPurpose() throws Exception {
        Class<?> userRepoClass = Class.forName("com.insightwrite.repository.UserRepository");
        Object userRepository = mock(userRepoClass);
        when(userRepoClass.getMethod("existsByEmail", String.class).invoke(userRepository, "used@example.com"))
                .thenReturn(true);
        Object service = buildAuthService(userRepository, null);

        assertThat(invokeBoolean(service, "shouldSendVerificationCode", "used@example.com", "forgot_password", null))
                .isTrue();
    }

    @Test
    void sendCodeDoesNotRevealUnknownEmailForPasswordResetPurpose() throws Exception {
        Class<?> userRepoClass = Class.forName("com.insightwrite.repository.UserRepository");
        Object userRepository = mock(userRepoClass);
        when(userRepoClass.getMethod("existsByEmail", String.class).invoke(userRepository, "missing@example.com"))
                .thenReturn(false);
        Object service = buildAuthService(userRepository, null);

        assertThat(invokeBoolean(service, "shouldSendVerificationCode", "missing@example.com", "forgot_password", null))
                .isFalse();
    }

    @Test
    void loginUsesSameMessageForMissingEmailAndBadPassword() throws Exception {
        Class<?> userRepoClass = Class.forName("com.insightwrite.repository.UserRepository");
        Object userRepository = mock(userRepoClass);
        Object existingUser = Class.forName("com.insightwrite.entity.User").getConstructor().newInstance();
        existingUser.getClass().getMethod("setPasswordHash", String.class).invoke(existingUser, "hash");
        when(userRepoClass.getMethod("findByEmail", String.class).invoke(userRepository, "missing@example.com"))
                .thenReturn(java.util.Optional.empty());
        when(userRepoClass.getMethod("findByEmail", String.class).invoke(userRepository, "used@example.com"))
                .thenReturn(java.util.Optional.of(existingUser));
        Class<?> passwordEncoderClass = Class.forName("org.springframework.security.crypto.password.PasswordEncoder");
        Object passwordEncoder = mock(passwordEncoderClass);
        when(passwordEncoderClass.getMethod("matches", CharSequence.class, String.class)
                .invoke(passwordEncoder, "bad-password", "hash"))
                .thenReturn(false);
        Object service = buildAuthService(userRepository, passwordEncoder, null);

        Throwable missingEmail = thrownByLogin(service, loginRequest("missing@example.com", "bad-password"));
        Throwable badPassword = thrownByLogin(service, loginRequest("used@example.com", "bad-password"));

        assertThat(missingEmail).isInstanceOf(RuntimeException.class).hasMessage("邮箱或密码错误");
        assertThat(badPassword).isInstanceOf(RuntimeException.class).hasMessage("邮箱或密码错误");
    }

    @Test
    void registerUsesGenericMessageForDuplicateEmail() throws Exception {
        Class<?> userRepoClass = Class.forName("com.insightwrite.repository.UserRepository");
        Object userRepository = mock(userRepoClass);
        when(userRepoClass.getMethod("existsByEmail", String.class).invoke(userRepository, "used@example.com"))
                .thenReturn(true);
        Object service = buildAuthService(userRepository, null);
        Object request = Class.forName("com.insightwrite.dto.RegisterRequest")
                .getConstructor()
                .newInstance();
        request.getClass().getMethod("setEmail", String.class).invoke(request, "used@example.com");

        assertThatThrownBy(() -> service.getClass().getMethod("register", request.getClass()).invoke(service, request))
                .hasRootCauseInstanceOf(RuntimeException.class)
                .hasRootCauseMessage("注册信息无效或验证码错误");
    }

    @Test
    void resetPasswordIncrementsTokenVersionToRevokeExistingTokens() throws Exception {
        Class<?> userRepoClass = Class.forName("com.insightwrite.repository.UserRepository");
        Object userRepository = mock(userRepoClass);
        Object user = Class.forName("com.insightwrite.entity.User").getConstructor().newInstance();
        user.getClass().getMethod("setEmail", String.class).invoke(user, "used@example.com");
        user.getClass().getMethod("setPasswordHash", String.class).invoke(user, "old-hash");
        user.getClass().getMethod("setTokenVersion", Integer.class).invoke(user, 2);
        when(userRepoClass.getMethod("findByEmail", String.class).invoke(userRepository, "used@example.com"))
                .thenReturn(java.util.Optional.of(user));
        Class<?> passwordEncoderClass = Class.forName("org.springframework.security.crypto.password.PasswordEncoder");
        Object passwordEncoder = mock(passwordEncoderClass);
        when(passwordEncoderClass.getMethod("encode", CharSequence.class)
                .invoke(passwordEncoder, "new-password"))
                .thenReturn("new-hash");
        Object emailService = mock(Class.forName("com.insightwrite.service.EmailService"));
        Object service = buildAuthService(userRepository, passwordEncoder, emailService, null);
        Object request = Class.forName("com.insightwrite.dto.ForgotPasswordRequest")
                .getConstructor()
                .newInstance();
        request.getClass().getMethod("setEmail", String.class).invoke(request, "used@example.com");
        request.getClass().getMethod("setCode", String.class).invoke(request, "123456");
        request.getClass().getMethod("setNewPassword", String.class).invoke(request, "new-password");

        service.getClass().getMethod("resetPassword", request.getClass()).invoke(service, request);

        assertThat((Integer) user.getClass().getMethod("getTokenVersion").invoke(user)).isEqualTo(3);
        assertThat((String) user.getClass().getMethod("getPasswordHash").invoke(user)).isEqualTo("new-hash");
    }

    @Test
    void loginBlocksAfterRepeatedFailedAttemptsForSameEmailAndClient() throws Exception {
        Object limiter = Class.forName("com.insightwrite.service.AuthAttemptLimiter")
                .getConstructor()
                .newInstance();
        Object request = Class.forName("com.insightwrite.dto.LoginRequest")
                .getConstructor()
                .newInstance();
        request.getClass().getMethod("setEmail", String.class).invoke(request, "locked@example.com");
        request.getClass().getMethod("setPassword", String.class).invoke(request, "wrong-password");
        Object service = buildAuthService(mock(Class.forName("com.insightwrite.repository.UserRepository")), limiter);

        for (int i = 0; i < 5; i += 1) {
            assertThatThrownBy(() -> invokeLogin(service, request, "203.0.113.7"))
                    .isInstanceOf(RuntimeException.class);
        }

        assertThatThrownBy(() -> invokeLogin(service, request, "203.0.113.7"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("尝试次数过多");
    }

    @Test
    void sendCodeBlocksAfterRepeatedRequestsFromSameClient() throws Exception {
        Object limiter = Class.forName("com.insightwrite.service.AuthAttemptLimiter")
                .getConstructor()
                .newInstance();

        for (int i = 0; i < 10; i += 1) {
            assertThatCode(() -> invokeCheckSendCode(limiter, "198.51.100.23"))
                    .doesNotThrowAnyException();
        }

        assertThatThrownBy(() -> invokeCheckSendCode(limiter, "198.51.100.23"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("验证码发送过于频繁");
    }

    private Object buildAuthService(Object userRepository, Object limiter) throws Exception {
        return buildAuthService(userRepository, null, limiter);
    }

    private Object buildAuthService(Object userRepository, Object passwordEncoder, Object limiter) throws Exception {
        return buildAuthService(userRepository, passwordEncoder, null, limiter);
    }

    private Object buildAuthService(Object userRepository, Object passwordEncoder, Object emailService, Object limiter) throws Exception {
        Class<?> userRepoClass = Class.forName("com.insightwrite.repository.UserRepository");
        Class<?> passwordEncoderClass = Class.forName("org.springframework.security.crypto.password.PasswordEncoder");
        Class<?> jwtUtilClass = Class.forName("com.insightwrite.util.JwtUtil");
        Class<?> emailServiceClass = Class.forName("com.insightwrite.service.EmailService");
        Class<?> limiterClass = Class.forName("com.insightwrite.service.AuthAttemptLimiter");
        return Class.forName("com.insightwrite.service.AuthService")
                .getConstructor(userRepoClass, passwordEncoderClass, jwtUtilClass, emailServiceClass, limiterClass)
                .newInstance(userRepository, passwordEncoder, null, emailService, limiter);
    }

    private void invoke(Object target, String method, String email, String purpose) throws Exception {
        try {
            target.getClass().getMethod(method, String.class, String.class).invoke(target, email, purpose);
        } catch (java.lang.reflect.InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw e;
        }
    }

    private Object invokeLogin(Object service, Object request, String clientKey) throws Exception {
        try {
            return service.getClass()
                    .getMethod("login", request.getClass(), String.class)
                    .invoke(service, request, clientKey);
        } catch (java.lang.reflect.InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw e;
        }
    }

    private boolean invokeBoolean(Object target, String method, String email, String purpose, String clientKey) throws Exception {
        try {
            return (boolean) target.getClass()
                    .getMethod(method, String.class, String.class, String.class)
                    .invoke(target, email, purpose, clientKey);
        } catch (java.lang.reflect.InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw e;
        }
    }

    private Object loginRequest(String email, String password) throws Exception {
        Object request = Class.forName("com.insightwrite.dto.LoginRequest")
                .getConstructor()
                .newInstance();
        request.getClass().getMethod("setEmail", String.class).invoke(request, email);
        request.getClass().getMethod("setPassword", String.class).invoke(request, password);
        return request;
    }

    private Throwable thrownByLogin(Object service, Object request) {
        try {
            invokeLogin(service, request, "203.0.113.9");
            throw new AssertionError("Expected login to fail");
        } catch (Throwable throwable) {
            return throwable;
        }
    }

    private void invokeCheckSendCode(Object limiter, String clientKey) throws Exception {
        try {
            limiter.getClass().getMethod("checkSendCodeAllowed", String.class).invoke(limiter, clientKey);
        } catch (java.lang.reflect.InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw e;
        }
    }
}
