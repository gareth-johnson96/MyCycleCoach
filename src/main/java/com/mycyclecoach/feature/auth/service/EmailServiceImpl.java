package com.mycyclecoach.feature.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${mycyclecoach.email.from:noreply@mycyclecoach.com}")
    private String fromEmail;

    @Value("${mycyclecoach.email.verification-url:http://localhost:8080/api/v1/auth/verify}")
    private String verificationBaseUrl;

    @Override
    public void sendVerificationEmail(String toEmail, String verificationToken) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Verify Your MyCycleCoach Email");
            message.setText(buildVerificationEmailBody(verificationToken));

            mailSender.send(message);
            log.info("Verification email sent to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send verification email to: {}", toEmail, e);
            // Don't throw exception - allow registration to continue
            // User can request resend if needed
        }
    }

    private String buildVerificationEmailBody(String token) {
        return String.format(
                """
                Welcome to MyCycleCoach!

                Please verify your email address by clicking the link below:

                %s?token=%s

                This link will expire in 24 hours.

                If you didn't create this account, please ignore this email.

                Best regards,
                The MyCycleCoach Team
                """,
                verificationBaseUrl, token);
    }
}
