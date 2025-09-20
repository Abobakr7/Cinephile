package com.example.cinephile.cinema.controller;

import com.example.cinephile.auth.entity.AuthUser;
import com.example.cinephile.auth.util.JwtUtil;
import com.example.cinephile.cinema.dto.*;
import com.example.cinephile.cinema.entity.Cinema;
import com.example.cinephile.cinema.entity.Screen;
import com.example.cinephile.cinema.repository.CinemaRepository;
import com.example.cinephile.cinema.repository.ScreenRepository;
import com.example.cinephile.cinema.repository.SeatRepository;
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
class CinemaControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CinemaRepository cinemaRepository;

    @Autowired
    private ScreenRepository screenRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    private String baseUrl;
    private User testManager;
    private Cinema testCinema;
    private String managerToken;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api/cinemas";

        seatRepository.deleteAll();
        screenRepository.deleteAll();
        cinemaRepository.deleteAll();
        userRepository.deleteAll();

        testManager = createTestUser("manager@example.com", "Test Manager", Role.MANAGER);
        userRepository.save(testManager);

        testCinema = createTestCinema("Test Cinema", "123 Test St", "123-456-7890", testManager);
        cinemaRepository.save(testCinema);

        managerToken = jwtUtil.generateAccessToken(new AuthUser(testManager));
    }

    @Test
    void getCinemas_WithoutAuthentication_ShouldReturnCinemas() {
        Cinema cinema2 = createTestCinema("Cinema 2", "456 Test Ave", "987-654-3210", testManager);
        cinemaRepository.save(cinema2);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
            baseUrl + "?page=0&size=10",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<>(){});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        List<?> content = (List<?>) response.getBody().get("content");
        assertThat(content).isNotNull().hasSize(2);
    }

    @Test
    void getCinemas_WithSearchParameter_ShouldReturnFilteredCinemas() {
        Cinema cinema2 = createTestCinema("Different Cinema", "456 Test Ave", "987-654-3210", testManager);
        cinemaRepository.save(cinema2);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                baseUrl + "?search=Test Cinema",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>(){});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        List<?> content = (List<?>) response.getBody().get("content");
        assertThat(content).isNotNull().hasSize(1);
    }

    @Test
    void getCinemasManagedByManager_WithoutAuth_ShouldReturnUnauthorized() {
        ResponseEntity<String> response = restTemplate.getForEntity(baseUrl + "/managed", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void getCinemaById_WithValidId_ShouldReturnCinema() {
        ResponseEntity<CinemaResponse> response = restTemplate.getForEntity(
                baseUrl + "/" + testCinema.getId(),
                CinemaResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().name()).isEqualTo("Test Cinema");
        assertThat(response.getBody().address()).isEqualTo("123 Test St");
        assertThat(response.getBody().phone()).isEqualTo("123-456-7890");
    }

    @Test
    void getCinemaById_WithInvalidId_ShouldReturnNotFound() {
        UUID nonExistentId = UUID.randomUUID();

        ResponseEntity<String> response = restTemplate.getForEntity(
                baseUrl + "/" + nonExistentId,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getCinemasManagedByManager_WithManagerAuth_ShouldReturnManagedCinemas() {
        HttpHeaders headers = createAuthHeaders(managerToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<List<CinemaResponse>> response = restTemplate.exchange(
                baseUrl + "/managed",
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    void createCinema_WithValidDataAndAuth_ShouldCreateCinema() {
        CinemaRequest request = new CinemaRequest(
                "New Cinema",
                "789 New St",
                "555-123-4567"
        );

        HttpHeaders headers = createAuthHeaders(managerToken);
        HttpEntity<CinemaRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<CinemaResponse> response = restTemplate.exchange(
                baseUrl,
                HttpMethod.POST,
                entity,
                CinemaResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().name()).isEqualTo("New Cinema");
    }

    @Test
    void createCinema_WithInvalidData_ShouldReturnBadRequest() {
        CinemaRequest request = new CinemaRequest("", "", "");

        HttpHeaders headers = createAuthHeaders(managerToken);
        HttpEntity<CinemaRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl,
                HttpMethod.POST,
                entity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void createCinema_WithoutAuth_ShouldReturnUnauthorized() {
        CinemaRequest request = new CinemaRequest("New Cinema", "789 New St", "555-123-4567");

        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl,
                request,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void updateCinema_WithValidDataAndAuth_ShouldUpdateCinema() {
        CinemaRequest request = new CinemaRequest(
                "Updated Cinema",
                "Updated Address",
                "999-888-7777"
        );

        HttpHeaders headers = createAuthHeaders(managerToken);
        HttpEntity<CinemaRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<CinemaResponse> response = restTemplate.exchange(
                baseUrl + "/" + testCinema.getId(),
                HttpMethod.PUT,
                entity,
                CinemaResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().name()).isEqualTo("Updated Cinema");
        assertThat(response.getBody().address()).isEqualTo("Updated Address");
        assertThat(response.getBody().phone()).isEqualTo("999-888-7777");
    }

    @Test
    void updateCinema_WithInvalidId_ShouldReturnNotFound() {
        CinemaRequest request = new CinemaRequest("Updated Cinema", "Updated Address", "999-888-7777");
        UUID nonExistentId = UUID.randomUUID();

        HttpHeaders headers = createAuthHeaders(managerToken);
        HttpEntity<CinemaRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/" + nonExistentId,
                HttpMethod.PUT,
                entity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void deleteCinema_WithValidIdAndAuth_ShouldDeleteCinema() {
        HttpHeaders headers = createAuthHeaders(managerToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/" + testCinema.getId(),
                HttpMethod.DELETE,
                entity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(cinemaRepository.findById(testCinema.getId())).isEmpty();
    }

    @Test
    void deleteCinema_WithInvalidId_ShouldReturnNotFound() {
        UUID nonExistentId = UUID.randomUUID();

        HttpHeaders headers = createAuthHeaders(managerToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/" + nonExistentId,
                HttpMethod.DELETE,
                entity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }


    @Test
    void getScreens_WithValidCinemaId_ShouldReturnScreens() {
        Screen testScreen = createTestScreen("Screen 1", 100, testCinema);
        screenRepository.save(testScreen);

        HttpHeaders headers = createAuthHeaders(managerToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<List<ScreenCard>> response = restTemplate.exchange(
                baseUrl + "/" + testCinema.getId() + "/screens",
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    void getScreenById_WithValidId_ShouldReturnScreen() {
        Screen testScreen = createTestScreen("Screen 1", 100, testCinema);
        screenRepository.save(testScreen);

        HttpHeaders headers = createAuthHeaders(managerToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<ScreenDetail> response = restTemplate.exchange(
                baseUrl + "/screens/" + testScreen.getId(),
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().name()).isEqualTo("Screen 1");
    }

    @Test
    void createScreen_WithValidDataAndAuth_ShouldCreateScreen() {
        CreateScreenRequest request = new CreateScreenRequest(
                "New Screen",
                180,
                12,
                15
        );

        HttpHeaders headers = createAuthHeaders(managerToken);
        HttpEntity<CreateScreenRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<Void> response = restTemplate.exchange(
                baseUrl + "/" + testCinema.getId() + "/screens",
                HttpMethod.POST,
                entity,
                void.class
        );
        System.out.println(response);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(screenRepository.findByCinemaId(testCinema.getId())).hasSize(1);
    }

    @Test
    void createScreen_WithInvalidData_ShouldReturnBadRequest() {
        CreateScreenRequest request = new CreateScreenRequest("", -1, 0, 50);

        HttpHeaders headers = createAuthHeaders(managerToken);
        HttpEntity<CreateScreenRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/" + testCinema.getId() + "/screens",
                HttpMethod.POST,
                entity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void updateScreen_WithValidDataAndAuth_ShouldUpdateScreen() {
        Screen testScreen = createTestScreen("Screen 1", 100, testCinema);
        screenRepository.save(testScreen);

        UpdateScreenRequest request = new UpdateScreenRequest("Updated Screen");

        HttpHeaders headers = createAuthHeaders(managerToken);
        HttpEntity<UpdateScreenRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<Void> response = restTemplate.exchange(
                baseUrl + "/screens/" + testScreen.getId(),
                HttpMethod.PUT,
                entity,
                Void.class
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

        // verify screen was updated
        Screen updatedScreen = screenRepository.findById(testScreen.getId()).orElseThrow();
        assertThat(updatedScreen.getName()).isEqualTo("Updated Screen");
        assertThat(updatedScreen.getCapacity()).isEqualTo(100);
    }

    @Test
    void deleteScreen_WithValidIdAndAuth_ShouldDeleteScreen() {
        Screen testScreen = createTestScreen("Screen 1", 100, testCinema);
        screenRepository.save(testScreen);

        HttpHeaders headers = createAuthHeaders(managerToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/screens/" + testScreen.getId(),
                HttpMethod.DELETE,
                entity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(screenRepository.findById(testScreen.getId())).isEmpty();
    }

    // helper methods
    private User createTestUser(String email, String name, Role role) {
        User user = new User();
        user.setEmail(email);
        user.setName(name);
        user.setPassword(passwordEncoder.encode("Password123"));
        user.setRole(role);
        user.setEnabled(true);
        return user;
    }

    private Cinema createTestCinema(String name, String address, String phone, User manager) {
        Cinema cinema = new Cinema();
        cinema.setName(name);
        cinema.setAddress(address);
        cinema.setPhone(phone);
        cinema.setManager(manager);
        cinema.setActive(true);
        return cinema;
    }

    private Screen createTestScreen(String name, Integer capacity, Cinema cinema) {
        Screen screen = new Screen();
        screen.setName(name);
        screen.setCapacity(capacity);
        screen.setCinema(cinema);
        screen.setActive(true);
        return screen;
    }

    private HttpHeaders createAuthHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return headers;
    }
}
