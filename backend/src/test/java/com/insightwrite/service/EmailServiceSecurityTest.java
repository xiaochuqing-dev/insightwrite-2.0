package com.insightwrite.service;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailServiceSecurityTest {

    @Test
    void sendVerificationCodeStoresOnlyHashAndInvalidatesPriorCodesForPurpose() throws Exception {
        FakeCodeRepository repository = new FakeCodeRepository(Optional.empty());
        Object service = emailService(repository);

        invoke(service, "sendVerificationCode", "user@example.com", "register");

        Object saved = repository.saved;
        assertThat(saved).isNotNull();
        assertThat((String) saved.getClass().getMethod("getCodeHash").invoke(saved))
                .isNotBlank()
                .doesNotMatch("\\d{6}");
        assertThat((String) saved.getClass().getMethod("getCode").invoke(saved))
                .isEqualTo("HASHED");
        assertThat(repository.invalidatedEmail).isEqualTo("user@example.com");
        assertThat(repository.invalidatedPurpose).isEqualTo("register");
    }

    @Test
    void validateCodeLocksRecordAfterRepeatedWrongAttempts() throws Exception {
        Object record = verificationRecord("user@example.com", "register", bcryptHash("111111"));
        FakeCodeRepository repository = new FakeCodeRepository(Optional.of(record));
        Object service = emailService(repository);

        for (int i = 0; i < 5; i += 1) {
            assertThatThrownBy(() -> invoke(service, "validateCode", "user@example.com", "000000", "register"))
                    .isInstanceOf(RuntimeException.class);
        }

        assertThat((Integer) record.getClass().getMethod("getFailedAttempts").invoke(record)).isEqualTo(5);
        Object lockedUntil = record.getClass().getMethod("getLockedUntil").invoke(record);
        assertThat(lockedUntil).isInstanceOf(LocalDateTime.class);
        assertThat((LocalDateTime) lockedUntil).isAfter(LocalDateTime.now());
    }

    private Object emailService(FakeCodeRepository repository) throws Exception {
        Class<?> mailSenderClass = Class.forName("org.springframework.mail.javamail.JavaMailSender");
        Object mailSender = Proxy.newProxyInstance(
                mailSenderClass.getClassLoader(),
                new Class<?>[]{mailSenderClass},
                new MailSenderHandler());
        Class<?> repoClass = Class.forName("com.insightwrite.repository.EmailVerificationCodeRepository");
        Class<?> passwordEncoderClass = Class.forName("org.springframework.security.crypto.password.PasswordEncoder");
        Object passwordEncoder = Class.forName("org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder")
                .getConstructor()
                .newInstance();
        return Class.forName("com.insightwrite.service.EmailService")
                .getConstructor(mailSenderClass, repoClass, passwordEncoderClass, String.class)
                .newInstance(mailSender, repository.proxy(), passwordEncoder, "noreply@example.com");
    }

    private Object verificationRecord(String email, String purpose, String codeHash) throws Exception {
        Object record = Class.forName("com.insightwrite.entity.EmailVerificationCode")
                .getConstructor()
                .newInstance();
        record.getClass().getMethod("setEmail", String.class).invoke(record, email);
        record.getClass().getMethod("setPurpose", String.class).invoke(record, purpose);
        record.getClass().getMethod("setCode", String.class).invoke(record, "HASHED");
        record.getClass().getMethod("setCodeHash", String.class).invoke(record, codeHash);
        record.getClass().getMethod("setFailedAttempts", Integer.class).invoke(record, 0);
        record.getClass().getMethod("setUsed", Boolean.class).invoke(record, false);
        record.getClass().getMethod("setExpiresAt", LocalDateTime.class).invoke(record, LocalDateTime.now().plusMinutes(5));
        record.getClass().getMethod("setCreatedAt", LocalDateTime.class).invoke(record, LocalDateTime.now());
        return record;
    }

    private String bcryptHash(String value) throws Exception {
        Object passwordEncoder = Class.forName("org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder")
                .getConstructor()
                .newInstance();
        return (String) passwordEncoder.getClass()
                .getMethod("encode", CharSequence.class)
                .invoke(passwordEncoder, value);
    }

    private void invoke(Object target, String method, String email, String codeOrPurpose, String purpose) throws Exception {
        try {
            target.getClass().getMethod(method, String.class, String.class, String.class)
                    .invoke(target, email, codeOrPurpose, purpose);
        } catch (java.lang.reflect.InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw e;
        }
    }

    private void invoke(Object target, String method, String email, String purpose) throws Exception {
        try {
            target.getClass().getMethod(method, String.class, String.class)
                    .invoke(target, email, purpose);
        } catch (java.lang.reflect.InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw e;
        }
    }

    private static final class FakeCodeRepository implements InvocationHandler {
        private final Optional<Object> latest;
        private Object saved;
        private String invalidatedEmail;
        private String invalidatedPurpose;

        private FakeCodeRepository(Optional<Object> latest) {
            this.latest = latest;
        }

        private Object proxy() throws Exception {
            Class<?> repoClass = Class.forName("com.insightwrite.repository.EmailVerificationCodeRepository");
            return Proxy.newProxyInstance(repoClass.getClassLoader(), new Class<?>[]{repoClass}, this);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            return switch (method.getName()) {
                case "findTopByEmailAndPurposeAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc" -> latest;
                case "invalidateUnusedForEmailAndPurpose" -> {
                    invalidatedEmail = (String) args[0];
                    invalidatedPurpose = (String) args[1];
                    yield null;
                }
                case "save" -> {
                    saved = args[0];
                    yield args[0];
                }
                default -> null;
            };
        }
    }

    private static final class MailSenderHandler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            if ("createMimeMessage".equals(method.getName())) {
                return new MimeMessage(Session.getInstance(new Properties()));
            }
            return null;
        }
    }
}
