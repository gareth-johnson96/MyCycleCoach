package com.mycyclecoach.feature.auth.service;

public interface EmailService {
    void sendVerificationEmail(String toEmail, String verificationToken);
}
