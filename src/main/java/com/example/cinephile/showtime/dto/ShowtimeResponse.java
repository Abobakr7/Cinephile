package com.example.cinephile.showtime.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ShowtimeResponse(UUID id, String movieTitle, String cinemaName,
                               String screenName, LocalDateTime startTime, LocalDateTime endTime) {
}
