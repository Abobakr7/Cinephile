package com.example.cinephile.movie.entity;

import com.fasterxml.uuid.Generators;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "movies")
@Getter @Setter
public class Movie {
    @Id
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID id;
    private String title;
    private String plot;
    @Column(name = "imdb_id", unique = true)
    private String imdbId;
    @Column(name = "poster_url")
    private String posterUrl;
    private Integer runtime; // in minutes
    private Integer year; // year of release
    private String genre; // comma separated genres
    private String rated; // e.g., PG-13, R
    private Double rating;
    private String director; // comma separated directors if multiple
    private String writer; // comma separated writers if multiple
    private String actors; // comma separated actors
    private String language; // comma separated languages if multiple
    private String country; // comma separated countries if multiple

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = Generators.timeBasedEpochGenerator().generate();
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
