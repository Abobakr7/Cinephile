package com.example.cinephile.movie.dto;

import java.util.UUID;

public record MoviePage(UUID id,
                        String title,
                        String plot,
                        String imdbId,
                        String posterUrl,
                        Integer runtime, // in minutes
                        Integer year, // year of release
                        String genre, // comma separated genres
                        String rated, // e.g., PG-13, R
                        Double rating,
                        String director, // comma separated directors if multiple
                        String writer, // comma separated writers if multiple
                        String actors, // comma separated actors
                        String language, // comma separated languages if multiple
                        String country // comma separated countries if multiple
) {}
