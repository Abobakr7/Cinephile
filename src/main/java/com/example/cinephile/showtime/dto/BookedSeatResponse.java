package com.example.cinephile.showtime.dto;

import com.example.cinephile.booking.entity.SeatStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record BookedSeatResponse(UUID id, Character rowNumber, Integer colNumber,
                                 String seatNumber, BigDecimal price, SeatStatus status) {
}
