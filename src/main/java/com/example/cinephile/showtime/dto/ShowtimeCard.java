package com.example.cinephile.showtime.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ShowtimeCard(UUID showtimeId,
                           String title,
                           String posterUrl,
                           String cinemaName,
                           String screenName,
                           LocalDateTime startTime,
                           LocalDateTime endTime) {
}
