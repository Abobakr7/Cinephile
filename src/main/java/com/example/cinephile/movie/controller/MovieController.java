package com.example.cinephile.movie.controller;

import com.example.cinephile.movie.dto.MovieCard;
import com.example.cinephile.movie.dto.MoviePage;
import com.example.cinephile.movie.dto.MovieRequest;
import com.example.cinephile.movie.service.MovieService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/movies")
@RequiredArgsConstructor
public class MovieController {
    private final MovieService movieService;

    @GetMapping
    public ResponseEntity<Page<MovieCard>> getAllMovies(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) String rated,
            @RequestParam(required = false, defaultValue = "0.0") Double rating,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        page = Math.max(page, 0);
        size = size < 0 ? 20 : Math.min(size, 20);
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(movieService.getAllMovies(title, genre, rated, rating, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MoviePage> getMovieById(@PathVariable UUID id) {
        return ResponseEntity.ok(movieService.getMovieById(id));
    }

    @PostMapping
    public ResponseEntity<MovieCard> addMovie(@Valid @RequestBody MovieRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(movieService.addMovie(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MovieCard> updateMovie(@PathVariable UUID id, @RequestBody MovieRequest request) {
        return ResponseEntity.ok(movieService.updateMovie(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMovie(@PathVariable UUID id) {
        movieService.deleteMovie(id);
        return ResponseEntity.noContent().build();
    }
}
