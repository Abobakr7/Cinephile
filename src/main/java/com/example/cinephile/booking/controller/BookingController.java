package com.example.cinephile.booking.controller;

import com.example.cinephile.auth.entity.AuthUser;
import com.example.cinephile.booking.dto.*;
import com.example.cinephile.booking.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {
    private final BookingService bookingService;

    @PostMapping("/{showtimeId}")
    public ResponseEntity<BookingInfoResponse> createBooking(@PathVariable("showtimeId") UUID showtimeId,
                                                             @AuthenticationPrincipal AuthUser authUser) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(bookingService.createBooking(showtimeId, authUser.getUser()));
    }

    @PostMapping("/{bookingId}/lock-seat")
    public ResponseEntity<BookingInfoResponse> lockSeat(@Valid @RequestBody SeatRequest request,
                                                        @PathVariable("bookingId") UUID bookingId,
                                                        @AuthenticationPrincipal AuthUser authUser) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(bookingService.lockSeat(request, bookingId, authUser.getUser().getId()));
    }

    @PostMapping("/{bookingId}/release-seat")
    public ResponseEntity<BookingInfoResponse> releaseSeat(@Valid @RequestBody SeatRequest request,
                                                           @PathVariable("bookingId") UUID bookingId,
                                                           @AuthenticationPrincipal AuthUser authUser) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(bookingService.releaseSeat(request, bookingId, authUser.getUser().getId()));
    }

    @PostMapping("/{bookingId}/confirm")
    public ResponseEntity<BookingConfirmResponse> confirmBooking(@PathVariable UUID bookingId) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(bookingService.confirmBooking(bookingId));
    }

    @PostMapping("/{bookingId}/cancel")
    public ResponseEntity<?> cancelBooking(@PathVariable UUID bookingId) {
        bookingService.cancelBooking(bookingId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<Page<BookingCard>> getUserBookings(@RequestParam(defaultValue = "0") int page,
                                                          @RequestParam(defaultValue = "10") int size,
                                                          @AuthenticationPrincipal AuthUser authUser) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(bookingService.getUserBookings(authUser.getUser(), PageRequest.of(page, size)));
    }

    @GetMapping("/me/{bookingId}")
    public ResponseEntity<BookingDetailsResponse> getBookingById(@PathVariable UUID bookingId,
                                                                 @AuthenticationPrincipal AuthUser authUser) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(bookingService.getBookingById(bookingId, authUser.getUser()));
    }
}
