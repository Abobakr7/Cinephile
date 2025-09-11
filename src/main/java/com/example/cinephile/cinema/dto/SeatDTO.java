package com.example.cinephile.cinema.dto;

import com.example.cinephile.cinema.entity.SeatType;

import java.util.UUID;

public record SeatDTO(UUID id, String seatNumber, Character rowName, int seatPosition, SeatType type) {
}
