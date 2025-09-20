package com.example.cinephile.booking.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record BookingDetailsResponse(UUID bookingId, String title, LocalDateTime startTime, Integer numberOfSeats,
                                     BigDecimal totalPrice, String status, List<BookedSeatDTO> bookedSeats) {
}
