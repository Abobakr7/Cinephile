package com.example.cinephile.movie.controller;

import com.example.cinephile.auth.entity.AuthUser;
import com.example.cinephile.auth.util.JwtUtil;
import com.example.cinephile.movie.dto.MovieCard;
import com.example.cinephile.movie.dto.MoviePage;
import com.example.cinephile.movie.dto.MovieRequest;
import com.example.cinephile.movie.entity.Movie;
import com.example.cinephile.movie.repository.MovieRepository;
import com.example.cinephile.user.entity.Role;
import com.example.cinephile.user.entity.User;
import com.example.cinephile.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class MovieControllerIntegrationTest {
    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    private String baseUrl;
    private User testAdmin;
    private String adminToken;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api/movies";

        movieRepository.deleteAll();
        userRepository.deleteAll();

        testAdmin = createTestUser("adminone@cinephile.com", "Admin One", Role.ADMIN);
        userRepository.save(testAdmin);
        adminToken = jwtUtil.generateAccessToken(new AuthUser(testAdmin));
    }

    @Test
    void getAllMovies_WhenNoMovies_ShouldReturnEmptyPage() {
        ResponseEntity<String> response = restTemplate.getForEntity(baseUrl, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"content\":[]");
        assertThat(response.getBody()).contains("\"totalElements\":0");
    }

    @Test
    void getAllMovies_WhenMoviesExist_ShouldReturnPageOfMovies() {
        Movie movie1 = createTestMovie("The Matrix", "Sci-Fi", "R", 8.7);
        Movie movie2 = createTestMovie("Inception", "Action", "PG-13", 8.8);
        movieRepository.saveAll(List.of(movie1, movie2));

        ResponseEntity<String> response = restTemplate.getForEntity(baseUrl, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("The Matrix");
        assertThat(response.getBody()).contains("Inception");
        assertThat(response.getBody()).contains("\"totalElements\":2");
    }

    @Test
    void getAllMovies_WithTitleFilter_ShouldReturnFilteredMovies() {
        Movie matrix = createTestMovie("The Matrix", "Sci-Fi", "R", 8.7);
        Movie inception = createTestMovie("Inception", "Action", "PG-13", 8.8);
        movieRepository.saveAll(List.of(matrix, inception));

        ResponseEntity<String> response = restTemplate.getForEntity(
            baseUrl + "?title=Matrix", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("The Matrix");
        assertThat(response.getBody()).doesNotContain("Inception");
    }

    @Test
    void getAllMovies_WithGenreFilter_ShouldReturnFilteredMovies() {
        Movie matrix = createTestMovie("The Matrix", "Sci-Fi", "R", 8.7);
        Movie inception = createTestMovie("Inception", "Action", "PG-13", 8.8);
        movieRepository.saveAll(List.of(matrix, inception));

        ResponseEntity<String> response = restTemplate.getForEntity(
            baseUrl + "?genre=Action", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Inception");
        assertThat(response.getBody()).doesNotContain("The Matrix");
    }

    @Test
    void getAllMovies_WithRatingFilter_ShouldReturnFilteredMovies() {
        Movie goodMovie = createTestMovie("Great Movie", "Drama", "PG", 9.0);
        Movie okMovie = createTestMovie("OK Movie", "Comedy", "PG", 7.0);
        movieRepository.saveAll(List.of(goodMovie, okMovie));

        ResponseEntity<String> response = restTemplate.getForEntity(
            baseUrl + "?rating=8.0", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Great Movie");
        assertThat(response.getBody()).doesNotContain("OK Movie");
    }

    @Test
    void getAllMovies_WithPagination_ShouldReturnCorrectPage() {
        List<Movie> movies = List.of(
            createTestMovie("Movie 1", "Action", "PG", 7.5),
            createTestMovie("Movie 2", "Drama", "PG-13", 8.0),
            createTestMovie("Movie 3", "Comedy", "R", 6.5)
        );
        movieRepository.saveAll(movies);

        ResponseEntity<String> response = restTemplate.getForEntity(
            baseUrl + "?page=0&size=2", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"size\":2");
        assertThat(response.getBody()).contains("\"totalElements\":3");
        assertThat(response.getBody()).contains("\"totalPages\":2");
    }

    @Test
    void getMovieById_WhenMovieExists_ShouldReturnMovieDetails() {
        Movie movie = createTestMovie("The Matrix", "Sci-Fi", "R", 8.7);
        Movie savedMovie = movieRepository.save(movie);

        ResponseEntity<MoviePage> response = restTemplate.getForEntity(
            baseUrl + "/" + savedMovie.getId(), MoviePage.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().title()).isEqualTo("The Matrix");
        assertThat(response.getBody().genre()).isEqualTo("Sci-Fi");
        assertThat(response.getBody().rated()).isEqualTo("R");
        assertThat(response.getBody().rating()).isEqualTo(8.7);
    }

    @Test
    void getMovieById_WhenMovieNotExists_ShouldReturn404() {
        UUID nonExistentId = UUID.randomUUID();

        ResponseEntity<String> response = restTemplate.getForEntity(
            baseUrl + "/" + nonExistentId, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void addMovie_WithValidData_ShouldCreateMovie() {
        MovieRequest request = new MovieRequest(
            "New Movie",
            "A great plot",
            "tt1234567",
            "http://poster.url",
            120,
            2023,
            "Action",
            "PG-13",
            8.5,
            "Great Director",
            "Amazing Writer",
            "Star Actor",
            "English",
            "USA"
        );

        HttpHeaders headers = createAuthHeaders(adminToken);
        HttpEntity<MovieRequest> requestEntity = new HttpEntity<>(request, headers);

        ResponseEntity<MovieCard> response = restTemplate.exchange(
            baseUrl, HttpMethod.POST, requestEntity, MovieCard.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().title()).isEqualTo("New Movie");
        assertThat(response.getBody().rating()).isEqualTo(8.5);
        assertThat(response.getBody().id()).isNotNull();

        // verify movie was saved in database
        assertThat(movieRepository.findById(response.getBody().id())).isPresent();
    }

    @Test
    void updateMovie_WhenMovieExists_ShouldUpdateMovie() {
        Movie movie = createTestMovie("Original Title", "Drama", "PG", 7.0);
        Movie savedMovie = movieRepository.save(movie);

        MovieRequest updateRequest = new MovieRequest(
            "Updated Title",
            "Updated plot",
            "tt7654321",
            "http://updated.poster.url",
            150,
            2024,
            "Action",
            "R",
            9.0,
            "Updated Director",
            "Updated Writer",
            "Updated Actor",
            "French",
            "France"
        );

        HttpHeaders headers = createAuthHeaders(adminToken);
        HttpEntity<MovieRequest> requestEntity = new HttpEntity<>(updateRequest, headers);

        ResponseEntity<MovieCard> response = restTemplate.exchange(
            baseUrl + "/" + savedMovie.getId(),
            HttpMethod.PUT,
            requestEntity,
            MovieCard.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().title()).isEqualTo("Updated Title");
        assertThat(response.getBody().rating()).isEqualTo(9.0);
    }

    @Test
    void updateMovie_WhenMovieNotExists_ShouldReturn404() {
        UUID nonExistentId = UUID.randomUUID();
        MovieRequest updateRequest = new MovieRequest(
            "Updated Title", "Updated plot", "tt7654321", "http://poster.url",
            150, 2024, "Action", "R", 9.0, "Director", "Writer", "Actor", "English", "USA"
        );

        HttpHeaders headers = createAuthHeaders(adminToken);
        HttpEntity<MovieRequest> requestEntity = new HttpEntity<>(updateRequest, headers);

        ResponseEntity<String> response = restTemplate.exchange(
            baseUrl + "/" + nonExistentId,
            HttpMethod.PUT,
            requestEntity,
            String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void deleteMovie_WhenMovieExists_ShouldDeleteMovie() {
        Movie movie = createTestMovie("To Delete", "Horror", "R", 6.0);
        Movie savedMovie = movieRepository.save(movie);

        HttpHeaders headers = createAuthHeaders(adminToken);
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<Void> response = restTemplate.exchange(
            baseUrl + "/" + savedMovie.getId(),
            HttpMethod.DELETE,
            requestEntity,
            Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(movieRepository.findById(savedMovie.getId())).isEmpty();
    }

    @Test
    void deleteMovie_WhenMovieNotExists_ShouldReturn404() {
        UUID nonExistentId = UUID.randomUUID();

        HttpHeaders headers = createAuthHeaders(adminToken);
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
            baseUrl + "/" + nonExistentId,
            HttpMethod.DELETE,
            requestEntity,
            String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getAllMovies_WithMultipleFilters_ShouldReturnCorrectResults() {
        Movie matrix = createTestMovie("The Matrix", "Sci-Fi,Action", "R", 8.7);
        Movie inception = createTestMovie("Inception", "Action,Thriller", "PG-13", 8.8);
        Movie comedy = createTestMovie("Funny Movie", "Comedy", "PG", 7.5);
        movieRepository.saveAll(List.of(matrix, inception, comedy));

        // filter by genre=Action and rating>=8.0
        ResponseEntity<String> response = restTemplate.getForEntity(
            baseUrl + "?genre=Action&rating=8.0", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("The Matrix");
        assertThat(response.getBody()).contains("Inception");
        assertThat(response.getBody()).doesNotContain("Funny Movie");
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

    private HttpHeaders createAuthHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return headers;
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
