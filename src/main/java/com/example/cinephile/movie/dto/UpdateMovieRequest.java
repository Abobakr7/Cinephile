package com.example.cinephile.movie.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record UpdateMovieRequest(@NotBlank UUID id,
                                 MovieRequest movie) {
}
