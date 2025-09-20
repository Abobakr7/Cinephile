package com.example.cinephile.booking.dto;

import com.example.cinephile.cinema.entity.SeatType;

import java.math.BigDecimal;
import java.util.UUID;

public record BookedSeatDTO(UUID bookedSatId, UUID seatId, String seatNumber, SeatType type, BigDecimal price) {
}
