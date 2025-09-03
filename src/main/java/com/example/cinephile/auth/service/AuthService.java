package com.example.cinephile.auth.service;

import com.example.cinephile.auth.dto.*;
import com.example.cinephile.auth.entity.AuthUser;
import com.example.cinephile.auth.entity.RefreshToken;
import com.example.cinephile.auth.repository.RefreshTokenRepository;
import com.example.cinephile.auth.util.JwtUtil;
import com.example.cinephile.common.exception.CinephileException;
import com.example.cinephile.user.entity.Role;
import com.example.cinephile.user.entity.User;
import com.example.cinephile.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final JwtUtil jwtUtil;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthenticationManager authenticationManager;

    public void register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new CinephileException("Email already in use", HttpStatus.CONFLICT);
        }

        User user = new User();
        user.setEmail(request.email());
        user.setName(request.name());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(Role.valueOf(request.role()));
        user.setEnabled(false);

        String verificationToken = UUID.randomUUID().toString();
        user.setVerificationToken(verificationToken);
        user.setVerificationTokenExpiry(LocalDateTime.now().plusHours(24));

        userRepository.save(user);

        emailService.sendVerificationEmail(user.getEmail(), verificationToken);
        log.info("User registered: {}", user.getEmail());
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        AuthUser principal = (AuthUser) authentication.getPrincipal();
        if (!principal.isEnabled()) {
            throw new CinephileException("Account not verified", HttpStatus.FORBIDDEN);
        }

        String accessToken = jwtUtil.generateAccessToken(principal);
        String refreshToken = jwtUtil.generateRefreshToken(principal);

        refreshTokenRepository.deleteByUserEmail(principal.getUsername()); // to enforce single session
        RefreshToken refreshTokenEntity = new RefreshToken();
        refreshTokenEntity.setUserEmail(principal.getUsername());
        refreshTokenEntity.setToken(refreshToken);
        refreshTokenRepository.save(refreshTokenEntity);

        return new AuthResponse(accessToken,
                refreshToken,
                principal.getUsername(),
                principal.getUser().getRole(),
                "Login successful");
    }

    public void logout(String refreshToken) {
        refreshTokenRepository.deleteByToken(refreshToken);
        log.info("User logged out");
    }

    public String verifyAccount(String verificationToken) {
        User user = userRepository.findByVerificationToken(verificationToken)
                .orElseThrow(() -> new CinephileException("Invalid verification token", HttpStatus.BAD_REQUEST));

        if (user.getVerificationTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new CinephileException("Verification token expired", HttpStatus.BAD_REQUEST);
        }

        user.setEnabled(true);
        user.setVerificationToken(null);
        user.setVerificationTokenExpiry(null);
        userRepository.save(user);
        log.info("User verified: {}", user.getEmail());

        return "Account verified successfully. You can now login.";
    }

    public String forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new CinephileException("Email not found", HttpStatus.NOT_FOUND));

        String resetToken = UUID.randomUUID().toString();
        user.setResetPasswordToken(resetToken);
        user.setResetPasswordTokenExpiry(LocalDateTime.now().plusHours(1));
        userRepository.save(user);

        emailService.sendPasswordResetEmail(user.getEmail(), resetToken);

        return "Password reset email sent. Please check your inbox.";
    }

    public String resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByResetPasswordToken(request.token())
                .orElseThrow(() -> new CinephileException("Invalid reset token", HttpStatus.BAD_REQUEST));

        if (user.getResetPasswordTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new CinephileException("Reset token expired", HttpStatus.BAD_REQUEST);
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setResetPasswordToken(null);
        user.setResetPasswordTokenExpiry(null);
        userRepository.save(user);
        log.info("User password reset: {}", user.getEmail());

        return "Password reset successful. You can now login with your new password.";
    }

    public AuthResponse refreshToken(String refreshToken) {
        RefreshToken storedToken = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new CinephileException("Refresh token not found", HttpStatus.UNAUTHORIZED));

        if (jwtUtil.isTokenExpired(refreshToken) || !jwtUtil.isTokenType(refreshToken, "refresh")) {
            throw new CinephileException("Invalid or expired refresh token", HttpStatus.UNAUTHORIZED);
        }

        String email = jwtUtil.extractUsername(refreshToken);
        if (!storedToken.getUserEmail().equals(email)) {
            throw new CinephileException("Refresh token does not match user", HttpStatus.UNAUTHORIZED);
        }

        AuthUser principal = new AuthUser(userRepository.findByEmail(email).get());
        String newAccessToken = jwtUtil.generateAccessToken(principal);

        return new AuthResponse(newAccessToken,
                null,
                email,
                principal.getUser().getRole(),
                "Token refreshed");
    }
}
