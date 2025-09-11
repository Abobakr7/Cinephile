package com.example.cinephile.cinema.dto;

import jakarta.validation.constraints.NotBlank;

public record CinemaRequest(@NotBlank(message = "Name is required") String name,
                            @NotBlank(message = "Address is required") String address,
                            @NotBlank(message = "Phone is required") String phone) {
}
