package com.example.cinephile.booking.controller;

import com.example.cinephile.auth.entity.AuthUser;
import com.example.cinephile.auth.util.JwtUtil;
import com.example.cinephile.booking.dto.*;
import com.example.cinephile.booking.entity.Booking;
import com.example.cinephile.booking.entity.BookingSeat;
import com.example.cinephile.booking.entity.BookingStatus;
import com.example.cinephile.booking.entity.SeatStatus;
import com.example.cinephile.booking.repository.BookingRepository;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class BookingControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private BookingSeatRepository bookingSeatRepository;

    @Autowired
    private ShowtimeRepository showtimeRepository;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private CinemaRepository cinemaRepository;

    @Autowired
    private ScreenRepository screenRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    private String baseUrl;
    private User testUser;
    private User testManager;
    private String testUserToken;
    private Showtime testShowtime;
    private Seat testSeat;
    private Cinema testCinema;
    private Screen testScreen;
    private Movie testMovie;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api/bookings";

        bookingSeatRepository.deleteAll();
        bookingRepository.deleteAll();
        showtimeRepository.deleteAll();
        seatRepository.deleteAll();
        screenRepository.deleteAll();
        cinemaRepository.deleteAll();
        movieRepository.deleteAll();
        userRepository.deleteAll();

        testUser = createTestUser("user@test.com", "Test User", Role.USER);
        testManager = createTestUser("manager@test.com", "Test Manager", Role.MANAGER);
        userRepository.saveAll(List.of(testUser, testManager));
        testUserToken = jwtUtil.generateAccessToken(new AuthUser(testUser));

        setupTestEntities();
    }

    private void setupTestEntities() {
        testCinema = new Cinema();
        testCinema.setManager(testManager);
        testCinema.setName("Test Cinema");
        testCinema.setAddress("Test Location");
        testCinema.setPhone("1234567890");
        testCinema.setActive(true);
        cinemaRepository.save(testCinema);

        testScreen = new Screen();
        testScreen.setName("Screen 1");
        testScreen.setCinema(testCinema);
        testScreen.setCapacity(100);
        screenRepository.save(testScreen);

        testSeat = new Seat();
        testSeat.setColNumber(1);
        testSeat.setRowNumber('A');
        testSeat.setSeatNumber("A1");
        testSeat.setType(SeatType.STANDARD);
        testSeat.setScreen(testScreen);
        seatRepository.save(testSeat);

        testMovie = createTestMovie("Test Movie", "Action", "PG-13", 7.5);
        movieRepository.save(testMovie);

        testShowtime = new Showtime();
        testShowtime.setMovie(testMovie);
        testShowtime.setScreen(testScreen);
        testShowtime.setCinema(testCinema);
        testShowtime.setActive(true);
        testShowtime.setStartTime(LocalDateTime.now().plusDays(1));
        testShowtime.setEndTime(LocalDateTime.now().plusDays(1).plusHours(2));
        showtimeRepository.save(testShowtime);
    }

    @Test
    void createBooking_WithValidShowtimeId_ShouldReturnCreated() {
        HttpHeaders headers = createAuthHeaders(testUserToken);

        ResponseEntity<BookingInfoResponse> response = restTemplate.exchange(
                baseUrl + "/" + testShowtime.getId(),
                HttpMethod.POST,
                new HttpEntity<>(headers),
                BookingInfoResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().showtimeId()).isEqualTo(testShowtime.getId());
        assertThat(response.getBody().numberOfSeats()).isEqualTo(0);
        assertThat(response.getBody().status()).isEqualTo("PENDING");

        // verify booking was created in database
        List<Booking> bookings = bookingRepository.findAll();
        assertThat(bookings).hasSize(1);
        assertThat(bookings.getFirst().getUser().getId()).isEqualTo(testUser.getId());
    }

    @Test
    void createBooking_WithInvalidShowtimeId_ShouldReturnNotFound() {
        HttpHeaders headers = createAuthHeaders(testUserToken);
        UUID invalidId = UUID.randomUUID();

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/" + invalidId,
                HttpMethod.POST,
                new HttpEntity<>(headers),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void lockSeat_WithValidRequest_ShouldReturnOk() throws Exception {
        Booking booking = createTestBooking(testUser);
        BookingSeat bookingSeat = createAvailableBookedSeat();

        SeatRequest seatRequest = new SeatRequest(bookingSeat.getId(), testShowtime.getId());
        HttpHeaders headers = createAuthHeaders(testUserToken);

        ResponseEntity<BookingInfoResponse> response = restTemplate.exchange(
                baseUrl + "/" + booking.getId() + "/lock-seat",
                HttpMethod.POST,
                new HttpEntity<>(seatRequest, headers),
                BookingInfoResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().numberOfSeats()).isEqualTo(1);

        // verify seat was locked
        BookingSeat updatedSeat = bookingSeatRepository.findById(bookingSeat.getId()).orElseThrow();
        assertThat(updatedSeat.getStatus()).isEqualTo(SeatStatus.HELD);
        assertThat(updatedSeat.getBooking().getId()).isEqualTo(booking.getId());
    }

    @Test
    void releaseSeat_WithValidRequest_ShouldReturnOk() throws Exception {
        Booking booking = createTestBooking(testUser);
        BookingSeat bookingSeat = createHeldBookedSeat(booking);

        SeatRequest seatRequest = new SeatRequest(bookingSeat.getId(), testShowtime.getId());
        HttpHeaders headers = createAuthHeaders(testUserToken);

        ResponseEntity<BookingInfoResponse> response = restTemplate.exchange(
                baseUrl + "/" + booking.getId() + "/release-seat",
                HttpMethod.POST,
                new HttpEntity<>(seatRequest, headers),
                BookingInfoResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().numberOfSeats()).isEqualTo(0);

        // verify seat was released
        BookingSeat updatedSeat = bookingSeatRepository.findById(bookingSeat.getId()).orElseThrow();
        assertThat(updatedSeat.getStatus()).isEqualTo(SeatStatus.AVAILABLE);
        assertThat(updatedSeat.getBooking()).isNull();
    }

    @Test
    void confirmBooking_WithValidBooking_ShouldReturnOk() {
        Booking booking = createTestBooking(testUser);
        BookingSeat bookingSeat = createHeldBookedSeat(booking);

        HttpHeaders headers = createAuthHeaders(testUserToken);

        ResponseEntity<BookingConfirmResponse> response = restTemplate.exchange(
                baseUrl + "/" + booking.getId() + "/confirm",
                HttpMethod.POST,
                new HttpEntity<>(headers),
                BookingConfirmResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().bookingId()).isEqualTo(booking.getId());
        assertThat(response.getBody().bookedSeatDTOs()).hasSize(1);

        // verify booking was confirmed
        Booking updatedBooking = bookingRepository.findById(booking.getId()).orElseThrow();
        assertThat(updatedBooking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(updatedBooking.getConfirmedAt()).isNotNull();

        // verify seat was booked
        BookingSeat updatedSeat = bookingSeatRepository.findById(bookingSeat.getId()).orElseThrow();
        assertThat(updatedSeat.getStatus()).isEqualTo(SeatStatus.BOOKED);
    }

    @Test
    void confirmBooking_WithExpiredBooking_ShouldReturnBadRequest() {
        Booking booking = createTestBooking(testUser);
        booking.setExpiresAt(LocalDateTime.now().minusHours(1));
        bookingRepository.save(booking);

        HttpHeaders headers = createAuthHeaders(testUserToken);

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/" + booking.getId() + "/confirm",
                HttpMethod.POST,
                new HttpEntity<>(headers),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void cancelBooking_WithValidBooking_ShouldReturnNoContent() {
        Booking booking = createTestBooking(testUser);
        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);

        BookingSeat bookingSeat = createHeldBookedSeat(booking);
        bookingSeat.setStatus(SeatStatus.BOOKED);
        bookingSeatRepository.save(bookingSeat);

        HttpHeaders headers = createAuthHeaders(testUserToken);

        ResponseEntity<Void> response = restTemplate.exchange(
                baseUrl + "/" + booking.getId() + "/cancel",
                HttpMethod.POST,
                new HttpEntity<>(headers),
                Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // verify booking was cancelled
        Booking updatedBooking = bookingRepository.findById(booking.getId()).orElseThrow();
        assertThat(updatedBooking.getStatus()).isEqualTo(BookingStatus.CANCELLED);

        // verify seats were released
        BookingSeat updatedSeat = bookingSeatRepository.findById(bookingSeat.getId()).orElseThrow();
        assertThat(updatedSeat.getStatus()).isEqualTo(SeatStatus.AVAILABLE);
    }

    @Test
    void getUserBookings_WithExistingBookings_ShouldReturnPageOfBookings() throws Exception {
        Booking booking1 = createTestBooking(testUser);
        Booking booking2 = createTestBooking(testUser);

        HttpHeaders headers = createAuthHeaders(testUserToken);

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/me",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"totalElements\":2");
        assertThat(response.getBody()).contains(booking1.getId().toString());
        assertThat(response.getBody()).contains(booking2.getId().toString());
    }

    @Test
    void getUserBookings_WithPagination_ShouldReturnCorrectPage() throws Exception {
        for (int i = 0; i < 5; i++) {
            createTestBooking(testUser);
        }

        HttpHeaders headers = createAuthHeaders(testUserToken);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                baseUrl + "/me?page=0&size=2",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<>(){}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        List<?> content = (List<?>) response.getBody().get("content");
        Map<String, Object> pageInfo = (Map<String, Object>) response.getBody().get("page");
        assertThat(content).hasSize(2);
        assertThat(pageInfo.get("totalElements")).isEqualTo(5);
    }

    @Test
    void getBookingById_WithValidId_ShouldReturnBookingDetails() {
        Booking booking = createTestBooking(testUser);
        BookingSeat bookingSeat = createHeldBookedSeat(booking);

        HttpHeaders headers = createAuthHeaders(testUserToken);

        ResponseEntity<BookingDetailsResponse> response = restTemplate.exchange(
                baseUrl + "/me/" + booking.getId(),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                BookingDetailsResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().bookingId()).isEqualTo(booking.getId());
        assertThat(response.getBody().title()).isEqualTo(testMovie.getTitle());
        assertThat(response.getBody().bookedSeats()).hasSize(1);
    }

    @Test
    void getBookingById_WithInvalidId_ShouldReturnNotFound() {
        HttpHeaders headers = createAuthHeaders(testUserToken);
        UUID invalidId = UUID.randomUUID();

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/me/" + invalidId,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void createBooking_WithoutAuthentication_ShouldReturnUnauthorized() {
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/" + testShowtime.getId(),
                HttpMethod.POST,
                new HttpEntity<>(new HttpHeaders()),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void lockSeat_WithConcurrentRequests_ShouldAllowOnlyOneToSucceed() throws Exception {
        User user1 = createTestUser("user1@test.com", "User One", Role.USER);
        User user2 = createTestUser("user2@test.com", "User Two", Role.USER);
        userRepository.saveAll(List.of(user1, user2));

        Booking booking1 = createTestBooking(user1);
        Booking booking2 = createTestBooking(user2);

        BookingSeat availableSeat = createAvailableBookedSeat();
        SeatRequest seatRequest = new SeatRequest(availableSeat.getId(), testShowtime.getId());

        HttpHeaders headers1 = createAuthHeaders(jwtUtil.generateAccessToken(new AuthUser(user1)));
        HttpHeaders headers2 = createAuthHeaders(jwtUtil.generateAccessToken(new AuthUser(user2)));

        // use CountDownLatch to synchronize the concurrent requests
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(2);

        ExecutorService executorService = Executors.newFixedThreadPool(2);

        // submit concurrent seat booking requests
        CompletableFuture<ResponseEntity<BookingInfoResponse>> future1 = CompletableFuture.supplyAsync(() -> {
            try {
                startLatch.await(); // wait for both threads to be ready
                return restTemplate.exchange(
                        baseUrl + "/" + booking1.getId() + "/lock-seat",
                        HttpMethod.POST,
                        new HttpEntity<>(seatRequest, headers1),
                        BookingInfoResponse.class
                );
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                finishLatch.countDown();
            }
        }, executorService);

        CompletableFuture<ResponseEntity<BookingInfoResponse>> future2 = CompletableFuture.supplyAsync(() -> {
            try {
                startLatch.await(); // wait for both threads to be ready
                return restTemplate.exchange(
                        baseUrl + "/" + booking2.getId() + "/lock-seat",
                        HttpMethod.POST,
                        new HttpEntity<>(seatRequest, headers2),
                        BookingInfoResponse.class
                );
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                finishLatch.countDown();
            }
        }, executorService);

        // start both requests simultaneously
        startLatch.countDown();

        // wait for both requests to complete (with timeout)
        assertThat(finishLatch.await(10, TimeUnit.SECONDS)).isTrue();

        ResponseEntity<BookingInfoResponse> response1 = future1.get();
        ResponseEntity<BookingInfoResponse> response2 = future2.get();

        executorService.shutdown();

        // verify that exactly one request succeeded and one failed
        int successCount = 0;
        int failureCount = 0;
        if (response1.getStatusCode() == HttpStatus.OK) {
            successCount++;
            assertThat(response1.getBody()).isNotNull();
            assertThat(response1.getBody().numberOfSeats()).isEqualTo(1);
        } else {
            failureCount++;
            // should return conflict or bad request status
            assertThat(response1.getStatusCode()).isIn(HttpStatus.CONFLICT, HttpStatus.BAD_REQUEST);
        }

        if (response2.getStatusCode() == HttpStatus.OK) {
            successCount++;
            assertThat(response2.getBody()).isNotNull();
            assertThat(response2.getBody().numberOfSeats()).isEqualTo(1);
        } else {
            failureCount++;
            // Should return conflict or bad request status
            assertThat(response2.getStatusCode()).isIn(HttpStatus.CONFLICT, HttpStatus.BAD_REQUEST);
        }

        // verify exactly one succeeded and one failed
        assertThat(successCount).isEqualTo(1);
        assertThat(failureCount).isEqualTo(1);

        // verify seat state in database - should be held by exactly one booking
        BookingSeat updatedSeat = bookingSeatRepository.findById(availableSeat.getId()).orElseThrow();
        assertThat(updatedSeat.getStatus()).isEqualTo(SeatStatus.HELD);
        assertThat(updatedSeat.getBooking()).isNotNull();

        // verify that only one booking has the seat
        List<Booking> updatedBookings = bookingRepository.findAll();
        int bookingsWithSeats = 0;
        for (Booking booking : updatedBookings) {
            if (booking.getNumberOfSeats() == 1) {
                bookingsWithSeats++;
            }
        }
        assertThat(bookingsWithSeats).isEqualTo(1);
    }

    // helper methods
    private HttpHeaders createAuthHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return headers;
    }

    private User createTestUser(String email, String name, Role role) {
        User user = new User();
        user.setEmail(email);
        user.setName(name);
        user.setPassword(passwordEncoder.encode("password123"));
        user.setRole(role);
        user.setEnabled(true);
        return user;
    }

    private Booking createTestBooking(User user) {
        Booking booking = new Booking();
        booking.setUser(user);
        booking.setShowtime(testShowtime);
        booking.setNumberOfSeats(0);
        booking.setTotalPrice(BigDecimal.ZERO);
        booking.setStatus(BookingStatus.PENDING);
        booking.setExpiresAt(LocalDateTime.now().plusMinutes(15));
        return bookingRepository.save(booking);
    }

    private BookingSeat createAvailableBookedSeat() {
        BookingSeat bookingSeat = new BookingSeat();
        bookingSeat.setSeat(testSeat);
        bookingSeat.setShowtime(testShowtime);
        bookingSeat.setStatus(SeatStatus.AVAILABLE);
        bookingSeat.setPrice(BigDecimal.valueOf(15.00));
        return bookingSeatRepository.save(bookingSeat);
    }

    private BookingSeat createHeldBookedSeat(Booking booking) {
        BookingSeat bookingSeat = new BookingSeat();
        bookingSeat.setSeat(testSeat);
        bookingSeat.setShowtime(testShowtime);
        bookingSeat.setBooking(booking);
        bookingSeat.setUser(testUser);
        bookingSeat.setStatus(SeatStatus.HELD);
        bookingSeat.setPrice(BigDecimal.valueOf(15.00));
        bookingSeat.setHeldUntil(LocalDateTime.now().plusMinutes(15));

        // Update booking totals
        booking.setNumberOfSeats(1);
        booking.setTotalPrice(BigDecimal.valueOf(15.00));
        bookingRepository.save(booking);

        return bookingSeatRepository.save(bookingSeat);
    }

    private Movie createTestMovie(String title, String genre, String rated, Double rating) {
        Movie movie = new Movie();
        movie.setTitle(title);
        movie.setPlot("Test plot for " + title);
        movie.setImdbId("tt" + String.valueOf((int) (Math.random() * 1_000_000)));
        movie.setPosterUrl("http://test.poster/" + title.replaceAll(" ", "").toLowerCase());
        movie.setRuntime(120);
        movie.setYear(2023);
        movie.setGenre(genre);
        movie.setRated(rated);
        movie.setRating(rating);
        movie.setDirector("Test Director");
        movie.setWriter("Test Writer");
        movie.setActors("Test Actor");
        movie.setLanguage("English");
        movie.setCountry("USA");
        return movie;
    }
}
