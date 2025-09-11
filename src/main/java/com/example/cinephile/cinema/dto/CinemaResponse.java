package com.example.cinephile.cinema.dto;

import java.util.UUID;

public record CinemaResponse(UUID id, String name, String address, String phone, UUID managerId) {
}
