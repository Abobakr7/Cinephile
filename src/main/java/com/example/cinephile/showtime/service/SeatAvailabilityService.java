package com.example.cinephile.showtime.service;

import com.example.cinephile.cinema.entity.Seat;
import com.example.cinephile.cinema.repository.SeatRepository;
import com.example.cinephile.common.exception.CinephileException;
import com.example.cinephile.booking.entity.BookingSeat;
import com.example.cinephile.booking.entity.SeatStatus;
import com.example.cinephile.booking.repository.BookingSeatRepository;
import com.example.cinephile.showtime.dto.SeatAvailabilityStats;
import com.example.cinephile.showtime.entity.Showtime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeatAvailabilityService {
    private final SeatRepository seatRepository;
    private final BookingSeatRepository bookingSeatRepository;

    @Transactional
    public void initializeSeatsForShowtime(Showtime showtime, BigDecimal price) {
        log.info("Initializing seat availability for showtime {} in screen {}",
                showtime.getId(), showtime.getScreen().getId());

        List<Seat> screenSeats = seatRepository.findByScreenId(showtime.getScreen().getId());
        if (screenSeats.isEmpty()) {
            log.warn("No seats found for screen {}", showtime.getScreen().getId());
            throw new CinephileException("No seats found for screen", HttpStatus.NOT_FOUND);
        }

        List<BookingSeat> availableSeats = screenSeats.stream()
                .filter(Seat::isActive)
                .map(seat -> createAvailableBookedSeat(seat, showtime, price))
                .toList();

        bookingSeatRepository.saveAll(availableSeats);

        log.info("Initialized {} seats as available for showtime {}",
                availableSeats.size(), showtime.getId());
    }

    @Transactional(readOnly = true)
    public List<BookingSeat> getShowtimeSeats(UUID showtimeId) {
        List<BookingSeat> bookingSeats = bookingSeatRepository.findByShowtimeId(showtimeId);
        log.info("Found {} available seats for showtime {}", bookingSeats.size(), showtimeId);
        return bookingSeats;
    }

    @Transactional(readOnly = true)
    public SeatAvailabilityStats getAvailabilityStats(UUID showtimeId) {
        long available = bookingSeatRepository.countAvailableSeats(showtimeId);

        List<BookingSeat> allSeats = bookingSeatRepository.findByShowtimeAndStatuses(
                showtimeId, List.of(SeatStatus.AVAILABLE, SeatStatus.HELD, SeatStatus.BOOKED));

        long held = allSeats.stream().mapToLong(seat -> seat.getStatus() == SeatStatus.HELD ? 1 : 0).sum();
        long booked = allSeats.stream().mapToLong(seat -> seat.getStatus() == SeatStatus.BOOKED ? 1 : 0).sum();
        long total = allSeats.size();

        return new SeatAvailabilityStats(total, available, held, booked);
    }

    private BookingSeat createAvailableBookedSeat(Seat seat, Showtime showtime, BigDecimal price) {
        BookingSeat bookingSeat = new BookingSeat();
        bookingSeat.setSeat(seat);
        bookingSeat.setShowtime(showtime);
        bookingSeat.setStatus(SeatStatus.AVAILABLE);
        bookingSeat.setPrice(price);
        return bookingSeat;
    }

    public void deleteSeatsForShowtime(UUID showtimeId) {
        bookingSeatRepository.deleteByShowtimeId(showtimeId);
        log.info("Deleted booked seats for showtime {}", showtimeId);
    }
}
