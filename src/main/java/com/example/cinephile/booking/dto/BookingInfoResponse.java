package com.example.cinephile.booking.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record BookingInfoResponse(UUID bookingId,
                                  UUID showtimeId,
                                  Integer numberOfSeats,
                                  BigDecimal totalPrice,
                                  LocalDateTime expiresAt,
                                  String status) {
}
