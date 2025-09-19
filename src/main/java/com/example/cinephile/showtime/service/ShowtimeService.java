package com.example.cinephile.showtime.service;

import com.example.cinephile.cinema.entity.Cinema;
import com.example.cinephile.cinema.entity.Screen;
import com.example.cinephile.cinema.repository.CinemaRepository;
import com.example.cinephile.cinema.repository.ScreenRepository;
import com.example.cinephile.common.exception.CinephileException;
import com.example.cinephile.movie.entity.Movie;
import com.example.cinephile.movie.repository.MovieRepository;
import com.example.cinephile.booking.entity.BookingSeat;
import com.example.cinephile.showtime.dto.*;
import com.example.cinephile.showtime.entity.Showtime;
import com.example.cinephile.showtime.repository.ShowtimeRepository;
import com.example.cinephile.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShowtimeService {
    private final ShowtimeRepository showtimeRepository;
    private final MovieRepository movieRepository;
    private final ScreenRepository screenRepository;
    private final CinemaRepository cinemaRepository;
    private final SeatAvailabilityService seatAvailabilityService;

    @Transactional(readOnly = true)
    public Page<ShowtimeCard> getAllShowtimes(Pageable pageable) {
        Page<Showtime> page = showtimeRepository.findAll(pageable);
        return page.map(showtime -> new ShowtimeCard(
                showtime.getId(),
                showtime.getMovie().getTitle(),
                showtime.getMovie().getPosterUrl(),
                showtime.getCinema().getName(),
                showtime.getScreen().getName(),
                showtime.getStartTime(),
                showtime.getEndTime()
        ));
    }

    @Transactional(readOnly = true)
    public Page<ShowtimeCard> getManagedShowtimesByCinema(User user, UUID cinemaId, Pageable pageable) {
        if (!cinemaRepository.existsByIdAndManagerId(cinemaId, user.getId())) {
            throw new CinephileException("Cinema not found or access denied", HttpStatus.NOT_FOUND);
        }
        Page<Showtime> page = showtimeRepository.findAllByCinemaId(cinemaId, pageable);
        return page.map(showtime -> new ShowtimeCard(
                showtime.getId(),
                showtime.getMovie().getTitle(),
                showtime.getMovie().getPosterUrl(),
                showtime.getCinema().getName(),
                showtime.getScreen().getName(),
                showtime.getStartTime(),
                showtime.getEndTime()
        ));
    }

    @Transactional(readOnly = true)
    public ShowtimeResponse getShowtimeById(UUID showtimeId) {
        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new CinephileException("Showtime not found", HttpStatus.NOT_FOUND));
        return new ShowtimeResponse(
                showtime.getId(), showtime.getMovie().getTitle(), showtime.getCinema().getName(),
                showtime.getScreen().getName(), showtime.getStartTime(), showtime.getEndTime()
        );
    }

    @Transactional(readOnly = true)
    public Page<ShowtimeMovieCard> getUpcomingShowtimes(String title, String genre, String rated, Pageable pageable) {
        return movieRepository.findMoviesWithUpcomingShowtimes(title, genre, rated, pageable);
    }

    @Transactional(readOnly = true) // get cinemas hosting a specific movie (booking start point)
    public List<ShowtimeCinemaCard> getShowtimeHostingCinemas(UUID movieId) {
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new CinephileException("Movie not found", HttpStatus.NOT_FOUND));
        List<Cinema> cinemas = cinemaRepository.findCinemasByMovieId(movieId);

        return cinemas.stream()
                .map(cinema -> new ShowtimeCinemaCard(cinema.getId(), cinema.getName()))
                .toList();
    }

    @Transactional(readOnly = true) // get days when the movie is showing in the selected cinema
    public List<LocalDate> getShowtimeAvailableDates(UUID movieId, UUID cinemaId) {
        return showtimeRepository
                .findViewingDaysByMovieAndCinema(movieId, cinemaId, LocalDateTime.now())
                .stream()
                .map(LocalDateTime::toLocalDate)
                .toList();
    }

    @Transactional(readOnly = true) // get times when the movie is showing in the selected cinema on the selected day
    public List<LocalTime> getShowtimeAvailableTimes(UUID movieId, UUID cinemaId, LocalDate day) {
        return showtimeRepository
                .findViewingTimesByMovieAndCinemaAndDay(movieId, cinemaId, day, LocalDateTime.now())
                .stream()
                .map(LocalDateTime::toLocalTime)
                .toList();
    }

    @Transactional(readOnly = true) // get screens showing the movie based on the selected cinema, day, and time
    public List<ShowtimeScreenCard> getShowtimeAvailableScreens(UUID movieId, UUID cinemaId, LocalDateTime datetime) {
        LocalDateTime start = datetime.withNano(0);
        LocalDateTime end = start.plusSeconds(1);
        List<Screen> screens = showtimeRepository.findScreensByMovieAndCinemaAndDateTime(movieId, cinemaId, start, end);
        return screens.stream()
                .map(screen -> new ShowtimeScreenCard(screen.getId(), screen.getName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ShowtimeLayoutResponse getShowtimeSeatLayout(UUID movieId, UUID cinemaId, UUID screenId, LocalDateTime datetime) {
        LocalDateTime start = datetime.withNano(0);
        LocalDateTime end = start.plusSeconds(1);
        Showtime showtime = showtimeRepository.findShowtimeByMovieIdAndCinemaIdAndScreenIdAndStartTime(
                        movieId, cinemaId, screenId, start, end
                ).orElseThrow(() -> new CinephileException("Showtime not found", HttpStatus.NOT_FOUND));

        List<BookingSeat> bookingSeats = seatAvailabilityService.getShowtimeSeats(showtime.getId());
        List<BookedSeatResponse> seatResponses = bookingSeats.stream()
                .map(seat -> new BookedSeatResponse(
                        seat.getId(),
                        seat.getSeat().getRowNumber(),
                        seat.getSeat().getColNumber(),
                        seat.getSeat().getSeatNumber(),
                        seat.getPrice(),
                        seat.getStatus()
                ))
                .toList();
        return new ShowtimeLayoutResponse(showtime.getId(), seatResponses);
    }

    @Transactional
    public ShowtimeResponse createShowtime(CreateShowtimeRequest request) {
        Movie movie = movieRepository.findById(request.movieId())
                .orElseThrow(() -> new CinephileException("Movie not found", HttpStatus.NOT_FOUND));
        Screen screen = screenRepository.findById(request.screenId())
                .orElseThrow(() -> new CinephileException("Screen not found", HttpStatus.NOT_FOUND));
        Cinema cinema = screen.getCinema();

        // check for scheduling conflicts
        boolean hasConflict = showtimeRepository.existsByScreenAndStartTimeLessThanAndEndTimeGreaterThan(
                screen, request.endTime(), request.startTime());
        if (hasConflict) {
            throw new CinephileException("Scheduling conflict detected", HttpStatus.CONFLICT);
        }

        Showtime showtime = new Showtime();
        showtime.setMovie(movie);
        showtime.setScreen(screen);
        showtime.setCinema(cinema);
        showtime.setStartTime(request.startTime());
        showtime.setEndTime(request.endTime());
        showtimeRepository.save(showtime);

        // create seats (`booked_seats`) for the showtime based on the screen's seating arrangement
        seatAvailabilityService.initializeSeatsForShowtime(showtime, request.price());

        return new ShowtimeResponse(
                showtime.getId(), movie.getTitle(), cinema.getName(),
                screen.getName(), showtime.getStartTime(), showtime.getEndTime()
        );
    }

    // consider sending notification or email to users if a showtime is updated or deleted
    @Transactional
    public ShowtimeResponse updateShowtime(UUID showtimeId, UpdateShowtimeRequest request) {
        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new CinephileException("Showtime not found", HttpStatus.NOT_FOUND));

        Screen screen = request.screenId() != null
                ? screenRepository.findById(request.screenId())
                    .orElseThrow(() -> new CinephileException("Screen not found", HttpStatus.NOT_FOUND))
                : showtime.getScreen();
        LocalDateTime startTime = request.startTime() != null ? request.startTime() : showtime.getStartTime();
        LocalDateTime endTime = request.endTime() != null ? request.endTime() : showtime.getEndTime();

        // check if only one of startTime or endTime is provided
        if ((request.startTime() != null || request.endTime() != null) &&
                !(request.startTime() != null && request.endTime() != null)) {
            throw new CinephileException("Start time and end time are required", HttpStatus.BAD_REQUEST);
        }

        if (request.screenId() != null && !screen.getCinema().getId().equals(showtime.getCinema().getId())) {
            throw new CinephileException("Screen does not belong to the same cinema", HttpStatus.BAD_REQUEST);
        }

        boolean hasConflict = showtimeRepository.existsByScreenAndStartTimeLessThanAndEndTimeGreaterThan(
                screen, startTime, endTime);
        if (hasConflict) {
            throw new CinephileException("Scheduling conflict detected", HttpStatus.CONFLICT);
        }

        showtime.setScreen(screen);
        showtime.setStartTime(startTime);
        showtime.setEndTime(endTime);
        showtimeRepository.save(showtime);

        return new ShowtimeResponse(
                showtime.getId(), showtime.getMovie().getTitle(), showtime.getCinema().getName(),
                screen.getName(), startTime, endTime
        );
    }

    @Transactional
    public void deleteShowtime(UUID showtimeId) {
        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new CinephileException("Showtime not found", HttpStatus.NOT_FOUND));
        seatAvailabilityService.deleteSeatsForShowtime(showtimeId);
        showtimeRepository.delete(showtime);
    }
}
