package com.example.cinephile.showtime.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record UpdateShowtimeRequest(UUID screenId,
                                    LocalDateTime startTime,
                                    LocalDateTime endTime) {
}
