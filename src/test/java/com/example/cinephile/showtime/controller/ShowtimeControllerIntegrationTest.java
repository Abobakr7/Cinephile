package com.example.cinephile.showtime.controller;

import com.example.cinephile.auth.entity.AuthUser;
import com.example.cinephile.auth.util.JwtUtil;
import com.example.cinephile.booking.repository.BookingSeatRepository;
import com.example.cinephile.cinema.entity.Cinema;
import com.example.cinephile.cinema.entity.Screen;
import com.example.cinephile.cinema.entity.Seat;
import com.example.cinephile.cinema.entity.SeatType;
import com.example.cinephile.cinema.repository.CinemaRepository;
import com.example.cinephile.cinema.repository.ScreenRepository;
import com.example.cinephile.cinema.repository.SeatRepository;
import com.example.cinephile.movie.entity.Movie;
import com.example.cinephile.movie.repository.MovieRepository;
import com.example.cinephile.showtime.dto.*;
import com.example.cinephile.showtime.entity.Showtime;
import com.example.cinephile.showtime.repository.ShowtimeRepository;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ShowtimeControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private CinemaRepository cinemaRepository;

    @Autowired
    private ScreenRepository screenRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private ShowtimeRepository showtimeRepository;

    @Autowired
    private BookingSeatRepository bookingSeatRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    private String baseUrl;
    private User testManager;
    private User testUser;
    private Movie testMovie;
    private Cinema testCinema;
    private Screen testScreen;
    private Showtime testShowtime;
    private String managerToken;
    private String userToken;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api/showtimes";

        bookingSeatRepository.deleteAll();
        showtimeRepository.deleteAll();
        seatRepository.deleteAll();
        screenRepository.deleteAll();
        cinemaRepository.deleteAll();
        movieRepository.deleteAll();
        userRepository.deleteAll();

        testManager = createTestUser("manager@example.com", "Test Manager", Role.MANAGER);
        testUser = createTestUser("user@example.com", "Test User", Role.USER);
        userRepository.saveAll(List.of(testManager, testUser));

        testMovie = createTestMovie("Test Movie", "Action", "PG-13", 120);
        movieRepository.save(testMovie);

        testCinema = createTestCinema("Test Cinema", "123 Test St", "123-456-7890", testManager);
        cinemaRepository.save(testCinema);

        testScreen = createTestScreen("Screen 1", 10, 15, testCinema);
        screenRepository.save(testScreen);

        createTestSeats(testScreen);

        testShowtime = createTestShowtime(testMovie, testScreen,
            LocalDateTime.now().plusHours(2), LocalDateTime.now().plusHours(4));
        showtimeRepository.save(testShowtime);

        managerToken = jwtUtil.generateAccessToken(new AuthUser(testManager));
        userToken = jwtUtil.generateAccessToken(new AuthUser(testUser));
    }

    @Test
    void getAllShowtimes_WithDefaultParameters_ShouldReturnPagedShowtimes() {
        Showtime showtime2 = createTestShowtime(testMovie, testScreen,
            LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(2));
        showtimeRepository.save(showtime2);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
            baseUrl + "?page=0&size=10",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        List<?> content = (List<?>) response.getBody().get("content");
        Map<String, Object> pageInfo = (Map<String, Object>) response.getBody().get("page");
        assertThat(content).hasSize(2);
        assertThat(pageInfo.get("totalElements")).isEqualTo(2);
    }

    @Test
    void getAllShowtimes_WithInvalidPageSize_ShouldUseDefaultValues() {
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
            baseUrl + "?page=-1&size=-1",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        Map<String, Object> pageInfo = (Map<String, Object>) response.getBody().get("page");
        assertThat(pageInfo.get("number")).isEqualTo(0); // page normalized to 0
    }

    @Test
    void getManagedShowtimesByCinema_WithValidManagerAndCinema_ShouldReturnShowtimes() {
        HttpHeaders headers = createAuthHeaders(managerToken);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
            baseUrl + "/managed/" + testCinema.getId() + "?page=0&size=10",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            new ParameterizedTypeReference<>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        List<?> content = (List<?>) response.getBody().get("content");
        assertThat(content).hasSize(1);
    }

    @Test
    void getShowtimeById_WithExistingId_ShouldReturnShowtime() {
        ResponseEntity<ShowtimeResponse> response = restTemplate.getForEntity(
            baseUrl + "/" + testShowtime.getId(), ShowtimeResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(testShowtime.getId());
        assertThat(response.getBody().movieTitle()).isEqualTo(testMovie.getTitle());
    }

    @Test
    void getShowtimeById_WithNonExistentId_ShouldReturnNotFound() {
        UUID nonExistentId = UUID.randomUUID();
        ResponseEntity<String> response = restTemplate.getForEntity(
            baseUrl + "/" + nonExistentId, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getUpcomingShowtimes_WithoutFilters_ShouldReturnUpcomingShowtimes() {
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
            baseUrl + "/now?page=0&size=10",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        List<?> content = (List<?>) response.getBody().get("content");
        assertThat(content).hasSize(1);
    }

    @Test
    void getUpcomingShowtimes_WithTitleFilter_ShouldReturnFilteredShowtimes() {
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
            baseUrl + "/now?title=Test Movie&page=0&size=10",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        List<?> content = (List<?>) response.getBody().get("content");
        assertThat(content).hasSize(1);
    }

    @Test
    void getUpcomingShowtimes_WithGenreFilter_ShouldReturnFilteredShowtimes() {
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
            baseUrl + "/now?genre=Action&page=0&size=10",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        List<?> content = (List<?>) response.getBody().get("content");
        assertThat(content).hasSize(1);
    }

    @Test
    void getUpcomingShowtimes_WithRatingFilter_ShouldReturnFilteredShowtimes() {
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
            baseUrl + "/now?rated=PG-13&page=0&size=10",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        List<?> content = (List<?>) response.getBody().get("content");
        assertThat(content).hasSize(1);
    }

    @Test
    void getShowtimeHostingCinemas_WithValidMovieId_ShouldReturnCinemas() {
        HttpHeaders headers = createAuthHeaders(userToken);

        ResponseEntity<List<ShowtimeCinemaCard>> response = restTemplate.exchange(
            baseUrl + "/movie/" + testMovie.getId() + "/cinemas",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            new ParameterizedTypeReference<>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    void getShowtimeHostingCinemas_WithNonExistentMovieId_ShouldReturnEmptyList() {
        UUID nonExistentMovieId = UUID.randomUUID();

        HttpHeaders headers = createAuthHeaders(userToken);
        ResponseEntity<String> response = restTemplate.exchange(
            baseUrl + "/movie/" + nonExistentMovieId + "/cinemas",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getShowtimeAvailableDates_WithValidMovieAndCinema_ShouldReturnDates() {
        HttpHeaders headers = createAuthHeaders(userToken);
        ResponseEntity<List<LocalDate>> response = restTemplate.exchange(
            baseUrl + "/movie/" + testMovie.getId() + "/cinema/" + testCinema.getId() + "/dates",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            new ParameterizedTypeReference<>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    void getShowtimeAvailableTimes_WithValidMovieCinemaAndDate_ShouldReturnTimes() {
        LocalDate date = testShowtime.getStartTime().toLocalDate();
        HttpHeaders headers = createAuthHeaders(userToken);

        ResponseEntity<List<LocalTime>> response = restTemplate.exchange(
            baseUrl + "/movie/" + testMovie.getId() + "/cinema/" + testCinema.getId() + "/dates/" + date + "/times",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            new ParameterizedTypeReference<>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    void getShowtimeAvailableScreens_WithValidParameters_ShouldReturnScreens() {
        LocalDate date = testShowtime.getStartTime().toLocalDate();
        LocalTime time = testShowtime.getStartTime().toLocalTime();
        HttpHeaders headers = createAuthHeaders(userToken);

        ResponseEntity<List<ShowtimeScreenCard>> response = restTemplate.exchange(
            baseUrl + "/movie/" + testMovie.getId() + "/cinema/" + testCinema.getId() +
            "/dates/" + date + "/times/" + time + "/screens",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            new ParameterizedTypeReference<>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    void getShowtimeLayout_WithValidParameters_ShouldReturnLayout() {
        LocalDate date = testShowtime.getStartTime().toLocalDate();
        LocalTime time = testShowtime.getStartTime().toLocalTime();
        HttpHeaders headers = createAuthHeaders(userToken);

        ResponseEntity<ShowtimeLayoutResponse> response = restTemplate.exchange(
            baseUrl + "/movie/" + testMovie.getId() + "/cinema/" + testCinema.getId() +
            "/dates/" + date + "/times/" + time + "/screens/" + testScreen.getId() + "/layout",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            ShowtimeLayoutResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().showtimeId()).isEqualTo(testShowtime.getId());
    }

    @Test
    void createShowtime_WithValidRequest_ShouldCreateShowtime() {
        CreateShowtimeRequest request = new CreateShowtimeRequest(
            testMovie.getId(),
            testScreen.getId(),
            LocalDateTime.now().plusDays(2),
            LocalDateTime.now().plusDays(2).plusHours(2),
            new BigDecimal("18.00")
        );

        HttpHeaders headers = createAuthHeaders(managerToken);

        ResponseEntity<ShowtimeResponse> response = restTemplate.exchange(
            baseUrl,
            HttpMethod.POST,
            new HttpEntity<>(request, headers),
            ShowtimeResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().movieTitle()).isEqualTo(testMovie.getTitle());
    }

    @Test
    void createShowtime_WithInvalidRequest_ShouldReturnBadRequest() {
        CreateShowtimeRequest request = new CreateShowtimeRequest(
            null, // Invalid: null movieId
            testScreen.getId(),
            LocalDateTime.now().plusDays(1),
            LocalDateTime.now().plusDays(1).plusHours(2),
            new BigDecimal("15.00")
        );

        HttpHeaders headers = createAuthHeaders(managerToken);

        ResponseEntity<String> response = restTemplate.exchange(
            baseUrl,
            HttpMethod.POST,
            new HttpEntity<>(request, headers),
            String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void createShowtime_WithoutAuthentication_ShouldReturnUnauthorized() {
        CreateShowtimeRequest request = new CreateShowtimeRequest(
            testMovie.getId(),
            testScreen.getId(),
            LocalDateTime.now().plusDays(1),
            LocalDateTime.now().plusDays(1).plusHours(2),
            new BigDecimal("15.00")
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response = restTemplate.postForEntity(
            baseUrl, new HttpEntity<>(request, headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void updateShowtime_WithValidRequest_ShouldUpdateShowtime() {
        UpdateShowtimeRequest request = new UpdateShowtimeRequest(
            testScreen.getId(),
            LocalDateTime.now().plusDays(3),
            LocalDateTime.now().plusDays(3).plusHours(2)
        );

        HttpHeaders headers = createAuthHeaders(managerToken);

        ResponseEntity<ShowtimeResponse> response = restTemplate.exchange(
            baseUrl + "/" + testShowtime.getId(),
            HttpMethod.PUT,
            new HttpEntity<>(request, headers),
            ShowtimeResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(testShowtime.getId());
    }

    @Test
    void updateShowtime_WithNonExistentId_ShouldReturnNotFound() {
        UUID nonExistentId = UUID.randomUUID();
        UpdateShowtimeRequest request = new UpdateShowtimeRequest(
            testScreen.getId(),
            LocalDateTime.now().plusDays(1),
            LocalDateTime.now().plusDays(1).plusHours(2)
        );

        HttpHeaders headers = createAuthHeaders(managerToken);

        ResponseEntity<String> response = restTemplate.exchange(
            baseUrl + "/" + nonExistentId,
            HttpMethod.PUT,
            new HttpEntity<>(request, headers),
            String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void updateShowtime_WithoutAuthentication_ShouldReturnUnauthorized() {
        UpdateShowtimeRequest request = new UpdateShowtimeRequest(
            testScreen.getId(),
            LocalDateTime.now().plusDays(1),
            LocalDateTime.now().plusDays(1).plusHours(2)
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response = restTemplate.exchange(
            baseUrl + "/" + testShowtime.getId(),
            HttpMethod.PUT,
            new HttpEntity<>(request, headers),
            String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void deleteShowtime_WithValidId_ShouldDeleteShowtime() {
        HttpHeaders headers = createAuthHeaders(managerToken);

        ResponseEntity<Void> response = restTemplate.exchange(
            baseUrl + "/" + testShowtime.getId(),
            HttpMethod.DELETE,
            new HttpEntity<>(headers),
            Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // verify showtime is deleted
        ResponseEntity<String> getResponse = restTemplate.getForEntity(
            baseUrl + "/" + testShowtime.getId(), String.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void deleteShowtime_WithNonExistentId_ShouldReturnNotFound() {
        UUID nonExistentId = UUID.randomUUID();
        HttpHeaders headers = createAuthHeaders(managerToken);

        ResponseEntity<String> response = restTemplate.exchange(
            baseUrl + "/" + nonExistentId,
            HttpMethod.DELETE,
            new HttpEntity<>(headers),
            String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void deleteShowtime_WithoutAuthentication_ShouldReturnUnauthorized() {
        ResponseEntity<String> response = restTemplate.exchange(
            baseUrl + "/" + testShowtime.getId(),
            HttpMethod.DELETE,
            null,
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
        return user;
    }

    private Movie createTestMovie(String title, String genre, String rated, int duration) {
        Movie movie = new Movie();
        movie.setTitle(title);
        movie.setPlot("Test plot for " + title);
        movie.setImdbId("tt" + String.valueOf((int) (Math.random() * 1_000_000)));
        movie.setPosterUrl("http://test.poster/" + title.replaceAll(" ", "").toLowerCase());
        movie.setRuntime(duration);
        movie.setYear(2023);
        movie.setGenre(genre);
        movie.setRated(rated);
        movie.setRating(8.5);
        movie.setDirector("Test Director");
        movie.setWriter("Test Writer");
        movie.setActors("Test Actor");
        movie.setLanguage("English");
        movie.setCountry("USA");
        return movie;
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

    private Screen createTestScreen(String name, int rows, int seatsPerRow, Cinema cinema) {
        Screen screen = new Screen();
        screen.setName(name);
        screen.setCinema(cinema);
        screen.setCapacity(rows * seatsPerRow);
        screen.setActive(true);
        return screen;
    }

    private void createTestSeats(Screen screen) {
        for (char row = 'A'; row <= 'J'; row++) {
            for (int number = 1; number <= 15; number++) {
                Seat seat = new Seat();
                seat.setRowNumber(row);
                seat.setColNumber(number);
                seat.setSeatNumber(row + String.valueOf(number));
                seat.setType(SeatType.STANDARD);
                seat.setScreen(screen);
                seat.setActive(true);
                seatRepository.save(seat);
            }
        }
    }

    private Showtime createTestShowtime(Movie movie, Screen screen, LocalDateTime startTime, LocalDateTime endTime) {
        Showtime showtime = new Showtime();
        showtime.setMovie(movie);
        showtime.setScreen(screen);
        showtime.setCinema(screen.getCinema());
        showtime.setStartTime(startTime);
        showtime.setEndTime(endTime);
        showtime.setActive(true);
        return showtime;
    }

    private HttpHeaders createAuthHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return headers;
    }
}
