package com.example.cinephile.user.controller;

import com.example.cinephile.auth.entity.AuthUser;
import com.example.cinephile.auth.util.JwtUtil;
import com.example.cinephile.user.dto.UpdateProfileRequest;
import com.example.cinephile.user.dto.UserProfile;
import com.example.cinephile.user.entity.Role;
import com.example.cinephile.user.entity.User;
import com.example.cinephile.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class UserControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    private String baseUrl;
    private User testUser;
    private User testAdmin;
    private User testManager;
    private String testUserToken;
    private String testAdminToken;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api/users";

        userRepository.deleteAll();

        testUser = createTestUser("user@test.com", "Test User", Role.USER);
        testAdmin = createTestUser("admin@test.com", "Test Admin", Role.ADMIN);
        testManager = createTestUser("manager@test.com", "Test Manager", Role.MANAGER);
        userRepository.saveAll(List.of(testUser, testAdmin, testManager));

        testUserToken = jwtUtil.generateAccessToken(new AuthUser(testUser));
        testAdminToken = jwtUtil.generateAccessToken(new AuthUser(testAdmin));
    }

    @Test
    void getUserProfile_WithValidToken_ShouldReturnUserProfile() {
        HttpHeaders headers = createAuthHeaders(testUserToken);
        ResponseEntity<UserProfile> response = restTemplate.exchange(
                baseUrl + "/me",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                UserProfile.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().email()).isEqualTo("user@test.com");
        assertThat(response.getBody().name()).isEqualTo("Test User");
        assertThat(response.getBody().role()).isEqualTo("USER");
    }

    @Test
    void getUserProfile_WithoutToken_ShouldReturnUnauthorized() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                baseUrl + "/me",
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void updateUserProfile_WithValidData_ShouldUpdateProfile() {
        UpdateProfileRequest request = new UpdateProfileRequest("Updated Name", "password123", "newPassword123");
        HttpHeaders headers = createAuthHeaders(testUserToken);

        ResponseEntity<UserProfile> response = restTemplate.exchange(
                baseUrl + "/me",
                HttpMethod.PUT,
                new HttpEntity<>(request, headers),
                UserProfile.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().name()).isEqualTo("Updated Name");
        assertThat(response.getBody().email()).isEqualTo("user@test.com");

        // verify password was updated in database
        User updatedUser = userRepository.findById(testUser.getId()).orElse(null);
        assertThat(updatedUser).isNotNull();
        assertThat(passwordEncoder.matches("newPassword123", updatedUser.getPassword())).isTrue();
    }

    @Test
    void updateUserProfile_WithWrongOldPassword_ShouldReturnBadRequest() {
        UpdateProfileRequest request = new UpdateProfileRequest("Updated Name", "wrongPassword", "newPassword123");
        HttpHeaders headers = createAuthHeaders(testUserToken);

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/me",
                HttpMethod.PUT,
                new HttpEntity<>(request, headers),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void getAllUsers_AsAdmin_ShouldReturnPagedUsers() {
        HttpHeaders headers = createAuthHeaders(testAdminToken);
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                baseUrl + "?page=0&size=10",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {}
        );

        List<?> content = (List<?>) response.getBody().get("content");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(content).hasSize(3); // testUser, testAdmin, testManager
    }

    @Test
    void getAllUsers_WithFilters_ShouldReturnFilteredUsers() {
        HttpHeaders headers = createAuthHeaders(testAdminToken);
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                baseUrl + "?role=USER&page=0&size=10",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {}
        );

        List<?> content = (List<?>) response.getBody().get("content");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(content).hasSize(1);
    }

    @Test
    void getAllUsers_WithNameFilter_ShouldReturnMatchingUsers() {
        HttpHeaders headers = createAuthHeaders(testAdminToken);
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                baseUrl + "?name=Admin&page=0&size=10",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {}
        );

        List<?> content = (List<?>) response.getBody().get("content");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(content).hasSize(1);
    }

    @Test
    void getAllUsers_WithEmailFilter_ShouldReturnMatchingUsers() {
        HttpHeaders headers = createAuthHeaders(testAdminToken);
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                baseUrl + "?email=manager@test.com&page=0&size=10",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {}
        );

        List<?> content = (List<?>) response.getBody().get("content");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(content).hasSize(1);
    }

    @Test
    void getAllUsers_AsNonAdmin_ShouldReturnForbidden() {
        HttpHeaders headers = createAuthHeaders(testUserToken);
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "?page=0&size=10",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void getUserById_AsAdmin_ShouldReturnUser() {
        HttpHeaders headers = createAuthHeaders(testAdminToken);
        ResponseEntity<UserProfile> response = restTemplate.exchange(
                baseUrl + "/" + testUser.getId(),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                UserProfile.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(testUser.getId());
        assertThat(response.getBody().email()).isEqualTo("user@test.com");
    }

    @Test
    void getUserById_WithNonExistentId_ShouldReturnNotFound() {
        UUID nonExistentId = UUID.randomUUID();
        HttpHeaders headers = createAuthHeaders(testAdminToken);

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/" + nonExistentId,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getUserById_AsNonAdmin_ShouldReturnForbidden() {
        HttpHeaders headers = createAuthHeaders(testUserToken);

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/" + testAdmin.getId(),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void getAllUsers_WithPagination_ShouldReturnCorrectPage() {
        // create additional users for pagination test
        for (int i = 1; i <= 25; i++) {
            User user = createTestUser("user" + i + "@test.com", "User " + i, Role.USER);
            userRepository.save(user);
        }

        HttpHeaders headers = createAuthHeaders(testAdminToken);
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                baseUrl + "?page=1&size=10",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {}
        );

        List<?> content = (List<?>) response.getBody().get("content");
        Map<String, Object> pageInfo = (Map<String, Object>) response.getBody().get("page");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(content).hasSize(10);
        assertThat(pageInfo.get("totalElements")).isEqualTo(28); // 3 original + 25 new
        assertThat(pageInfo.get("number")).isEqualTo(1);
        assertThat(pageInfo.get("totalPages")).isEqualTo(3);
    }

    // helper methods
    private User createTestUser(String email, String name, Role role) {
        User user = new User();
        user.setEmail(email);
        user.setName(name);
        user.setPassword(passwordEncoder.encode("password123"));
        user.setRole(role);
        user.setEnabled(true);
        return user;
    }

    private HttpHeaders createAuthHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return headers;
    }
}
