package com.example.cinephile.auth.service;

import com.example.cinephile.booking.dto.BookingConfirmResponse;
import com.example.cinephile.booking.service.QrCodeService;
import jakarta.activation.DataSource;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.util.ByteArrayDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;
    private final QrCodeService qrCodeService;

    @Value("${spring.mail.username}")
    private String from;

    private String appUrl = "http://localhost:8080/api/auth";

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' hh:mm a");

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

    public void sendBookingConfirmationEmail(String to, BookingConfirmResponse bookingDetails) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject("Cinephile Booking Confirmation");

            String content = bookingEmailContent(bookingDetails);
            helper.setText(content, false);

            byte[] qrCode = qrCodeService.generateQrCode(bookingDetails, 250, 250);
            DataSource qrCodeDataSource = new ByteArrayDataSource(qrCode, "image/png");
            helper.addAttachment(
                    "booking-qr-%s.png".formatted(bookingDetails.bookingId()),
                    qrCodeDataSource);

            mailSender.send(message);
            log.info("Sent booking confirmation email to {} for booking {}", to, bookingDetails.bookingId());
        } catch (Exception e) {
            log.error("Failed to send booking confirmation email to {}", to, e);
        }
    }

    private String bookingEmailContent(BookingConfirmResponse booking) {
        StringBuilder content = new StringBuilder();
        content.append("BOOKING CONFIRMATION\n");
        content.append("========================\n\n");
        content.append("Your booking is confirmed! Here are your booking details:\n\n");
        content.append("BOOKING DETAILS:\n");
        content.append("----------------\n");

        content.append("Booking ID: ").append(booking.bookingId()).append("\n");
        content.append("Showtime ID: ").append(booking.showtimeId()).append("\n");
        content.append("Movie: ").append(booking.movieTitle()).append("\n");
        content.append("Cinema: ").append(booking.cinemaName()).append("\n");
        content.append("Screen: ").append(booking.screenName()).append("\n");
        content.append("Number of Seats: ").append(booking.numberOfSeats()).append("\n");
        content.append("Total Price: $").append(booking.totalPrice()).append("\n");
        content.append("Start Time: ").append(booking.startTime().format(DATE_TIME_FORMATTER)).append("\n");
        content.append("Confirmed At: ").append(booking.confirmedAt().format(DATE_TIME_FORMATTER)).append("\n\n");

        if (booking.bookedSeatDTOs() != null && !booking.bookedSeatDTOs().isEmpty()) {
            content.append("SEAT DETAILS:\n");
            content.append("-------------\n");
            booking.bookedSeatDTOs().forEach(seat -> {
                content.append("Seat: ").append(seat.seatNumber())
                       .append(" - Type: ").append(seat.type())
                       .append(" - Price: $").append(seat.price()).append("\n");
            });
            content.append("\n");
        }

        content.append("Please present the attached QR code at the cinema entrance for verification.\n\n");
        content.append("Thank you for choosing Cinephile! Enjoy your movie!\n");
        content.append("Cinephile Team");
        return content.toString();
    }
}
