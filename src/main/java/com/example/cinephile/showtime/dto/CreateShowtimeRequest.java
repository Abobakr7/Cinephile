package com.example.cinephile.showtime.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record CreateShowtimeRequest(@NotNull(message = "Movie ID is required") UUID movieId,
                                    @NotNull(message = "Screen ID is required") UUID screenId,
                                    LocalDateTime startTime,
                                    LocalDateTime endTime,
                                    BigDecimal price) {
}
