package com.example.cinephile.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String from;

    private String appUrl = "http://localhost:8080/api/auth";

    public void sendVerificationEmail(String to, String verificationToken) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject("Cinephile Email Verification");
            message.setText(String.format(
                "Welcome to Cinephile! Please verify your account by clicking the link below:\n\n" +
                "%s/verify?token=%s\n\n" +
                "This link will expire in 24 hours.\n\n" +
                "If you didn't create an account, please ignore this email.",
                appUrl, verificationToken
            ));

            mailSender.send(message);
            log.info("Sent verification email to {}", to);
        } catch (MailException e) {
            log.error("Failed to send verification email to {}", to, e);
        }
    }

    public void sendPasswordResetEmail(String to, String resetToken) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject("Cinephile Password Reset");
            message.setText(String.format(
                    "You requested to reset your password. Click the link below to set a new password:\n\n" +
                    "%s/reset-password?token=%s\n\n" +
                    "This link will expire in 1 hour.\n\n" +
                    "If you didn't request a password reset, please ignore this email.",
                    appUrl, resetToken
            ));

            mailSender.send(message);
            log.info("Sent password reset email to {}", to);
        } catch (MailException e) {
            log.error("Failed to send password reset email to {}", to, e);
        }
    }
}
