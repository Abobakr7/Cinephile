package com.example.cinephile.booking.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record BookingCard(UUID bookingId,
                          String title,
                          LocalDateTime startTime,
                          Integer numberOfSeats,
                          BigDecimal totalPrice,
                          String status) {
}
