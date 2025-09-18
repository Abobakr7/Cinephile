package com.example.cinephile.cinema.repository;

import com.example.cinephile.cinema.entity.Cinema;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CinemaRepository extends JpaRepository<Cinema, UUID> {
    Page<Cinema> findByNameContainingIgnoreCase(String search, Pageable pageable);

    List<Cinema> findByManagerId(UUID managerId);

    @Query("""
            SELECT DISTINCT c FROM Cinema c
            JOIN Showtime st ON st.cinema = c
            WHERE st.movie.id = :movieId
                AND st.isActive = true
                AND st.startTime > CURRENT_TIMESTAMP
            """)
    List<Cinema> findCinemasByMovieId(UUID movieId);

    boolean existsByIdAndManagerId(UUID cinemaId, UUID managerId);
}
