package com.example.cinephile.showtime.dto;

import java.util.List;
import java.util.UUID;

public record ShowtimeLayoutResponse(UUID showtimeId, List<BookedSeatResponse> seatResponses) {
}
