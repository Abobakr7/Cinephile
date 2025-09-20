package com.example.cinephile.booking.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SeatRequest(@NotNull(message = "Seat ID is required") UUID seatId,
                          @NotNull(message = "Showtime ID is required") UUID showtimeId) {
}
