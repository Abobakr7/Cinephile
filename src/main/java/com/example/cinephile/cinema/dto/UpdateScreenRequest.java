package com.example.cinephile.cinema.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateScreenRequest(@NotBlank(message = "Name is required") String name) {
}
