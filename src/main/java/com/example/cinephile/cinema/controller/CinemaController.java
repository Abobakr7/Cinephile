package com.example.cinephile.cinema.controller;

import com.example.cinephile.cinema.dto.*;
import com.example.cinephile.cinema.service.CinemaService;
import com.example.cinephile.cinema.service.ScreenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/cinemas")
@RequiredArgsConstructor
public class CinemaController {
    private final CinemaService cinemaService;
    private final ScreenService screenService;

    // cinemas controllers
    @GetMapping
    public ResponseEntity<Page<CinemaResponse>> getCinemas(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        page = Math.max(page, 0);
        size = size < 0 ? 20 : Math.min(size, 20);
        Pageable pageable = Pageable.ofSize(size).withPage(page);
        return ResponseEntity.ok(cinemaService.getCinemas(search, pageable));
    }

    @GetMapping("/managed")
    public ResponseEntity<List<CinemaResponse>> getCinemasManagedByManager() {
        return ResponseEntity.ok(cinemaService.getCinemasManagedByManager());
    }

    @GetMapping("/{cinemaId}")
    public ResponseEntity<CinemaResponse> getCinemaById(@PathVariable UUID cinemaId) {
        return ResponseEntity.ok(cinemaService.getCinemaById(cinemaId));
    }

    @PostMapping
    public ResponseEntity<CinemaResponse> createCinema(@Valid @RequestBody CinemaRequest request) {
        return ResponseEntity.status(201).body(cinemaService.createCinema(request));
    }

    @PutMapping("/{cinemaId}")
    public ResponseEntity<CinemaResponse> updateCinema(@PathVariable UUID cinemaId, @Valid @RequestBody CinemaRequest request) {
        return ResponseEntity.ok(cinemaService.updateCinema(cinemaId, request));
    }

    @DeleteMapping("/{cinemaId}")
    public ResponseEntity<?> deleteCinema(@PathVariable UUID cinemaId) {
        cinemaService.deleteCinema(cinemaId);
        return ResponseEntity.noContent().build();
    }

    // screens controllers
    @GetMapping("/{cinemaId}/screens")
    public ResponseEntity<List<ScreenCard>> getScreens(@PathVariable UUID cinemaId) {
        return ResponseEntity.ok(screenService.getScreens(cinemaId));
    }

    @GetMapping("/screens/{screenId}")
    public ResponseEntity<ScreenDetail> getScreenById(@PathVariable UUID screenId) {
        return ResponseEntity.ok(screenService.getScreenById(screenId));
    }

    @PostMapping("/{cinemaId}/screens")
    public ResponseEntity<?> createScreen(@PathVariable UUID cinemaId, @Valid @RequestBody CreateScreenRequest request) {
        screenService.createScreen(cinemaId, request);
        return ResponseEntity.status(201).build();
    }

    @PutMapping("/screens/{screenId}")
    public ResponseEntity<?> updateScreen(@PathVariable UUID screenId, @Valid @RequestBody UpdateScreenRequest request) {
        screenService.updateScreen(screenId, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/screens/{screenId}")
    public ResponseEntity<?> deleteScreen(@PathVariable UUID screenId) {
        screenService.deleteScreen(screenId);
        return ResponseEntity.noContent().build();
    }
}
