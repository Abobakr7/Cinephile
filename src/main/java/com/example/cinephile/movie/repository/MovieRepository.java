package com.example.cinephile.movie.repository;

import com.example.cinephile.movie.dto.MovieCard;
import com.example.cinephile.movie.entity.Movie;
import com.example.cinephile.showtime.dto.ShowtimeMovieCard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MovieRepository extends JpaRepository<Movie, UUID> {
    Page<Movie> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    @Query("""
            SELECT new com.example.cinephile.movie.dto.MovieCard(
            m.id, m.title, m.posterUrl, m.year, m.rated, m.rating
            )
            FROM Movie m
            WHERE (:title IS NULL OR LOWER(m.title) LIKE LOWER(CONCAT('%', :title, '%')))
            AND (:genre IS NULL OR LOWER(m.genre) LIKE LOWER(CONCAT('%', :genre, '%')))
            AND (:rated IS NULL OR LOWER(m.rated) = LOWER(:rated))
            AND (:rating IS NULL OR m.rating >= :rating)
            """)
    Page<MovieCard> findAllMovies(String title, String genre, String rated, Double rating, Pageable pageable);

    @Query("""
            SELECT new com.example.cinephile.showtime.dto.ShowtimeMovieCard(
                m.id, m.title, m.posterUrl, m.plot, m.genre, m.rated
            )
            FROM Movie m
            WHERE EXISTS (
                SELECT 1 FROM Showtime s
                WHERE s.movie = m AND s.startTime > CURRENT_TIMESTAMP
            )
            AND (:title IS NULL OR LOWER(m.title) LIKE LOWER(CONCAT('%', :title, '%')))
            AND (:genre IS NULL OR LOWER(m.genre) LIKE LOWER(CONCAT('%', :genre, '%')))
            AND (:rated IS NULL OR LOWER(m.rated) = LOWER(:rated))
            """)
    Page<ShowtimeMovieCard> findMoviesWithUpcomingShowtimes(String title, String genre, String rated, Pageable pageable);
}
