package com.example.cinephile.showtime.controller;

import com.example.cinephile.auth.entity.AuthUser;
import com.example.cinephile.showtime.dto.*;
import com.example.cinephile.showtime.service.ShowtimeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/showtimes")
@RequiredArgsConstructor
public class ShowtimeController {
    private final ShowtimeService showtimeService;

    @GetMapping
    public ResponseEntity<Page<ShowtimeCard>> getAllShowtimes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        page = Math.max(page, 0);
        size = size < 0 ? 20 : Math.min(size, 20);
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(showtimeService.getAllShowtimes(pageable));
    }

    @GetMapping("/managed/{cinemaId}")
    public ResponseEntity<Page<ShowtimeCard>> getManagedShowtimesByCinema(
            @PathVariable UUID cinemaId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AuthUser authUser
            ) {
        page = Math.max(page, 0);
        size = size < 0 ? 20 : Math.min(size, 20);
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(showtimeService.getManagedShowtimesByCinema(authUser.getUser(), cinemaId, pageable));
    }

    @GetMapping("/{showtimeId}")
    public ResponseEntity<ShowtimeResponse> getShowtimeById(@PathVariable UUID showtimeId) {
        return ResponseEntity.ok(showtimeService.getShowtimeById(showtimeId));
    }

    @GetMapping("/now")
    public ResponseEntity<Page<ShowtimeMovieCard>> getUpcomingShowtimes(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) String rated,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        page = Math.max(page, 0);
        size = size < 0 ? 20 : Math.min(size, 20);
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(showtimeService.getUpcomingShowtimes(title, genre, rated, pageable));
    }

    @GetMapping("/movie/{movieId}/cinemas")
    public ResponseEntity<List<ShowtimeCinemaCard>> getShowtimeHostingCinemas(@PathVariable UUID movieId) {
        return ResponseEntity.ok(showtimeService.getShowtimeHostingCinemas(movieId));
    }

    @GetMapping("/movie/{movieId}/cinema/{cinemaId}/dates")
    public ResponseEntity<List<LocalDate>> getShowtimeAvailableDates(
            @PathVariable UUID movieId,
            @PathVariable UUID cinemaId
    ) {
        return ResponseEntity.ok(showtimeService.getShowtimeAvailableDates(movieId, cinemaId));
    }

    @GetMapping("/movie/{movieId}/cinema/{cinemaId}/dates/{date}/times")
    public ResponseEntity<List<LocalTime>> getShowtimeAvailableTimes(
            @PathVariable UUID movieId,
            @PathVariable UUID cinemaId,
            @PathVariable LocalDate date
    ) {
        return ResponseEntity.ok(showtimeService.getShowtimeAvailableTimes(movieId, cinemaId, date));
    }

    @GetMapping("/movie/{movieId}/cinema/{cinemaId}/dates/{date}/times/{time}/screens")
    public ResponseEntity<List<ShowtimeScreenCard>> getShowtimeAvailableScreens(
            @PathVariable UUID movieId,
            @PathVariable UUID cinemaId,
            @PathVariable LocalDate date,
            @PathVariable LocalTime time
    ) {
        LocalDateTime datetime = LocalDateTime.of(date, time);
        return ResponseEntity.ok(showtimeService.getShowtimeAvailableScreens(movieId, cinemaId, datetime));
    }

    @GetMapping("/movie/{movieId}/cinema/{cinemaId}/dates/{date}/times/{time}/screens/{screenId}/layout")
    public ResponseEntity<ShowtimeLayoutResponse> getShowtimeLayout(
            @PathVariable UUID movieId,
            @PathVariable UUID cinemaId,
            @PathVariable LocalDate date,
            @PathVariable LocalTime time,
            @PathVariable UUID screenId
    ) {
        LocalDateTime datetime = LocalDateTime.of(date, time);
        return ResponseEntity.ok(showtimeService.getShowtimeSeatLayout(movieId, cinemaId, screenId, datetime));
    }

    @PostMapping
    public ResponseEntity<ShowtimeResponse> createShowtime(@Valid @RequestBody CreateShowtimeRequest request) {
        return ResponseEntity.status(201).body(showtimeService.createShowtime(request));
    }

    @PutMapping("/{showtimeId}")
    public ResponseEntity<ShowtimeResponse> updateShowtime(@PathVariable UUID showtimeId,
                                                           @Valid @RequestBody UpdateShowtimeRequest request) {
        return ResponseEntity.ok(showtimeService.updateShowtime(showtimeId, request));
    }

    @DeleteMapping("/{showtimeId}")
    public ResponseEntity<Void> deleteShowtime(@PathVariable UUID showtimeId) {
        showtimeService.deleteShowtime(showtimeId);
        return ResponseEntity.noContent().build();
    }
}
