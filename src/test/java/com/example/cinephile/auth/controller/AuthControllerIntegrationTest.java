package com.example.cinephile.auth.controller;

import com.example.cinephile.auth.dto.*;
import com.example.cinephile.auth.entity.AuthUser;
import com.example.cinephile.auth.entity.RefreshToken;
import com.example.cinephile.auth.util.JwtUtil;
import com.example.cinephile.user.entity.Role;
import com.example.cinephile.user.entity.User;
import com.example.cinephile.user.repository.UserRepository;
import com.example.cinephile.auth.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api/auth";

        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void register_WithValidRequest_ShouldReturnCreated() {
        RegisterRequest request = new RegisterRequest(
                "test@example.com",
                "Test User",
                "Password123",
                "USER"
        );

        ResponseEntity<Void> response = restTemplate.postForEntity(
                baseUrl + "/register",
                request,
                Void.class
        );
        System.out.println(response);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(userRepository.findByEmail("test@example.com")).isPresent();
    }

    @Test
    void register_WithInvalidEmail_ShouldReturnBadRequest() {
        RegisterRequest request = new RegisterRequest(
                "invalid-email",
                "Test User",
                "Password123!",
                "USER"
        );

        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/register",
                request,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void register_WithDuplicateEmail_ShouldReturnConflict() {
        createTestUser("test@example.com", "Test User", Role.USER);
        RegisterRequest request = new RegisterRequest(
                "test@example.com",
                "Another User",
                "Password123",
                "USER"
        );

        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/register",
                request,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void login_WithValidCredentials_ShouldReturnTokens() {
        createTestUser("test@example.com", "Test User", Role.USER);
        LoginRequest request = new LoginRequest("test@example.com", "password123");

        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                baseUrl + "/login",
                request,
                AuthResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().accessToken()).isNotBlank();
        assertThat(response.getBody().refreshToken()).isNotBlank();
        assertThat(response.getBody().email()).isEqualTo("test@example.com");
        assertThat(response.getBody().role()).isEqualTo(Role.USER);
    }

    @Test
    void login_WithInvalidCredentials_ShouldReturnUnauthorized() {
        createTestUser("test@example.com", "Test User", Role.USER);
        LoginRequest request = new LoginRequest("test@example.com", "wrongpassword");

        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/login",
                request,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void login_WithNonExistentUser_ShouldReturnUnauthorized() {
        LoginRequest request = new LoginRequest("nonexistent@example.com", "password123");

        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/login",
                request,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void logout_WithValidRefreshToken_ShouldReturnOk() {
        User user = createTestUser("test@example.com", "Test User", Role.USER);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Refresh-Token", jwtUtil.generateRefreshToken(new AuthUser(user)));
        headers.setBearerAuth(jwtUtil.generateAccessToken(new AuthUser(user)));
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/logout",
                HttpMethod.DELETE,
                request,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("Logged out successfully");
    }

    @Test
    void logout_WithInvalidRefreshToken_ShouldReturnUnauthorized() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Refresh-Token", "invalid-refresh-token");
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/logout",
                HttpMethod.DELETE,
                request,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void verifyAccount_WithValidToken_ShouldReturnOk() {
        User user = createUnverifiedUser("test@example.com", "Test User", Role.USER);
        String verificationToken = createVerificationToken(user);
        user.setVerificationToken(verificationToken);
        user.setVerificationTokenExpiry(java.time.LocalDateTime.now().plusHours(1));
        userRepository.save(user);

        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/verify?token=" + verificationToken,
                null,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        User verifiedUser = userRepository.findByEmail("test@example.com").orElseThrow();
        assertThat(verifiedUser.getEnabled()).isTrue();
    }

    @Test
    void verifyAccount_WithInvalidToken_ShouldReturnBadRequest() {
        String invalidToken = "invalid-verification-token";

        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/verify?token=" + invalidToken,
                null,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void forgotPassword_WithValidEmail_ShouldReturnOk() {
        createTestUser("test@example.com", "Test User", Role.USER);
        ForgotPasswordRequest request = new ForgotPasswordRequest("test@example.com");

        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/forgot-password",
                request,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void forgotPassword_WithNonExistentEmail_ShouldReturnNotFound() {
        ForgotPasswordRequest request = new ForgotPasswordRequest("nonexistent@example.com");

        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/forgot-password",
                request,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void resetPassword_WithValidToken_ShouldReturnOk() {
        User user = createTestUser("test@example.com", "Test User", Role.USER);
        String resetToken = createPasswordResetToken(user);
        user.setResetPasswordToken(resetToken);
        user.setResetPasswordTokenExpiry(java.time.LocalDateTime.now().plusHours(1));
        userRepository.save(user);
        ResetPasswordRequest request = new ResetPasswordRequest("NewPassword123", resetToken);

        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/reset-password",
                request,
                String.class
        );
        System.out.println(response);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void resetPassword_WithInvalidToken_ShouldReturnBadRequest() {
        ResetPasswordRequest request = new ResetPasswordRequest("NewPassword123!", "invalid-token");

        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/reset-password",
                request,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void refreshToken_WithValidRefreshToken_ShouldReturnNewTokens() {
        User user = createTestUser("test@example.com", "Test User", Role.USER);
        String refreshToken = jwtUtil.generateRefreshToken(new AuthUser(user));
        RefreshToken refreshTokenEntity = new RefreshToken();
        refreshTokenEntity.setUserEmail(user.getEmail());
        refreshTokenEntity.setToken(refreshToken);
        refreshTokenRepository.save(refreshTokenEntity);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Refresh-Token", refreshToken);
        headers.setBearerAuth(jwtUtil.generateAccessToken(new AuthUser(user)));
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<AuthResponse> response = restTemplate.exchange(
                baseUrl + "/refresh",
                HttpMethod.POST,
                request,
                AuthResponse.class
        );
        System.out.println(response);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().accessToken()).isNotBlank();
        assertThat(response.getBody().email()).isEqualTo("test@example.com");
    }

    @Test
    void refreshToken_WithInvalidRefreshToken_ShouldReturnUnauthorized() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Refresh-Token", "invalid-refresh-token");
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/refresh",
                HttpMethod.POST,
                request,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // helper methods
    private User createTestUser(String email, String name, Role role) {
        User user = new User();
        user.setEmail(email);
        user.setName(name);
        user.setPassword(passwordEncoder.encode("password123"));
        user.setRole(role);
        user.setEnabled(true);
        return userRepository.save(user);
    }

    private User createUnverifiedUser(String email, String name, Role role) {
        User user = new User();
        user.setEmail(email);
        user.setName(name);
        user.setPassword(passwordEncoder.encode("password123"));
        user.setRole(role);
        user.setEnabled(false);
        return userRepository.save(user);
    }

    private String createVerificationToken(User user) {
        return UUID.randomUUID().toString();
    }

    private String createPasswordResetToken(User user) {
        return UUID.randomUUID().toString();
    }
}
