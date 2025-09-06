package com.example.cinephile.movie.dto;

import java.util.UUID;

public record MovieCard(UUID id,
                        String title,
                        String posterUrl,
                        Integer year,
                        String rated,
                        Double rating
) {}
