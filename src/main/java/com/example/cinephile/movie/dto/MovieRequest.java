package com.example.cinephile.movie.dto;


public record MovieRequest(String title,
                           String plot,
                           String imdbId,
                           String posterUrl,
                           Integer runtime,
                           Integer year,
                           String genre,
                           String rated,
                           Double rating,
                           String director,
                           String writer,
                           String actors,
                           String language,
                           String country) {
}
