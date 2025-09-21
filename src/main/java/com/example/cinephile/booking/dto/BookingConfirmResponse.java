package com.example.cinephile.booking.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record BookingConfirmResponse(UUID bookingId,
                                     UUID showtimeId,
                                     String movieTitle,
                                     String cinemaName,
                                     String screenName,
                                     LocalDateTime startTime,
                                     Integer numberOfSeats,
                                     BigDecimal totalPrice,
                                     LocalDateTime confirmedAt,
                                     List<BookedSeatDTO> bookedSeatDTOs) {
}
