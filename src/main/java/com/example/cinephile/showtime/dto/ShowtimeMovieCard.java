package com.example.cinephile.showtime.dto;

import java.util.UUID;

public record ShowtimeMovieCard(UUID movieId,
                                String title,
                                String posterUrl,
                                String plot,
                                String genre,
                                String rated) {
}
