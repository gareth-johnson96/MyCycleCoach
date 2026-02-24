package com.mycyclecoach.feature.auth.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doThrow;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailServiceImpl emailService;

    @Test
    void shouldSendVerificationEmailSuccessfully() {
        // given
        String toEmail = "test@example.com";
        String verificationToken = "test-token-123";
        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@mycyclecoach.com");
        ReflectionTestUtils.setField(emailService, "verificationBaseUrl", "http://localhost:8080/api/v1/auth/verify");

        // when
        emailService.sendVerificationEmail(toEmail, verificationToken);

        // then
        then(mailSender).should().send(any(SimpleMailMessage.class));
    }

    @Test
    void shouldHandleMailExceptionGracefully() {
        // given
        String toEmail = "test@example.com";
        String verificationToken = "test-token-123";
        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@mycyclecoach.com");
        ReflectionTestUtils.setField(emailService, "verificationBaseUrl", "http://localhost:8080/api/v1/auth/verify");
        doThrow(new MailException("SMTP server unavailable") {})
                .when(mailSender)
                .send(any(SimpleMailMessage.class));

        // when - should not throw exception
        emailService.sendVerificationEmail(toEmail, verificationToken);

        // then - verify send was attempted
        then(mailSender).should().send(any(SimpleMailMessage.class));
    }
}
