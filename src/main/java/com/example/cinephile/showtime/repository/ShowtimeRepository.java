package com.example.cinephile.showtime.repository;

import com.example.cinephile.cinema.entity.Screen;
import com.example.cinephile.showtime.entity.Showtime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShowtimeRepository extends JpaRepository<Showtime, UUID> {
    boolean existsByScreenAndStartTimeLessThanAndEndTimeGreaterThan(Screen screen, LocalDateTime startTime, LocalDateTime endTime);

    @Query(value = """
            SELECT DISTINCT st.startTime FROM Showtime st
            WHERE st.movie.id = :movieId
                AND st.cinema.id = :cinemaId
                AND st.isActive = true
                AND st.startTime >= :now
            ORDER BY st.startTime ASC
            """)
    List<LocalDateTime> findViewingDaysByMovieAndCinema(UUID movieId, UUID cinemaId, LocalDateTime now);

    @Query(value = """
            SELECT DISTINCT st.startTime FROM Showtime st
            WHERE st.movie.id = :movieId
                AND st.cinema.id = :cinemaId
                AND FUNCTION('DATE', st.startTime) = :day
                AND st.isActive = true
                AND st.startTime >= :now
            ORDER BY st.startTime ASC
            """)
    List<LocalDateTime> findViewingTimesByMovieAndCinemaAndDay(UUID movieId, UUID cinemaId, LocalDate day, LocalDateTime now);

    @Query("""
            SELECT st.screen FROM Showtime st
            WHERE st.movie.id = :movieId
                AND st.cinema.id = :cinemaId
                AND st.startTime >= :start
                AND st.startTime <= :end
                AND st.isActive = true
            """)
    List<Screen> findScreensByMovieAndCinemaAndDateTime(UUID movieId, UUID cinemaId, LocalDateTime start, LocalDateTime end);

    @Query("""
        SELECT st FROM Showtime st
        WHERE st.movie.id = :movieId
            AND st.cinema.id = :cinemaId
            AND st.screen.id = :screenId
            AND st.startTime >= :start
            AND st.startTime <= :end
            AND st.isActive = true
    """)
    Optional<Showtime> findShowtimeByMovieIdAndCinemaIdAndScreenIdAndStartTime(UUID movieId, UUID cinemaId, UUID screenId,
                                                                               LocalDateTime start, LocalDateTime end);

    Page<Showtime> findAllByCinemaId(UUID cinemaId, Pageable pageable);
}
