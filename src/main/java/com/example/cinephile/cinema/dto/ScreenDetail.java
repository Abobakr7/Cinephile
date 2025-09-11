package com.example.cinephile.cinema.dto;

import java.util.List;
import java.util.UUID;

public record ScreenDetail(UUID id, String name, int capacity, List<SeatDTO> list) {
}
