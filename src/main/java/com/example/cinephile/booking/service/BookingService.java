package com.example.cinephile.booking.service;

import com.example.cinephile.auth.service.EmailService;
import com.example.cinephile.cinema.entity.Seat;
import com.example.cinephile.booking.dto.*;
import com.example.cinephile.booking.entity.*;
import com.example.cinephile.booking.repository.BookingSeatRepository;
import com.example.cinephile.booking.repository.BookingRepository;
import com.example.cinephile.common.exception.CinephileException;
import com.example.cinephile.showtime.entity.Showtime;
import com.example.cinephile.showtime.repository.ShowtimeRepository;
import com.example.cinephile.user.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import lombok.RequiredArgsConstructor;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class BookingService {
    private static final int MAX_SEATS_PER_BOOKING = 12;
    private static final int TIMER = 15; // in minutes

    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final ShowtimeRepository showtimeRepository;
    private final EmailService emailService;

    public BookingInfoResponse createBooking(UUID showtimeId, User user) {
        log.info("Creating new booking for user {} in showtime {}", user.getId(), showtimeId);
        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new CinephileException("Showtime not found", HttpStatus.NOT_FOUND));

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setShowtime(showtime);
        booking.setNumberOfSeats(0);
        booking.setTotalPrice(BigDecimal.ZERO);
        booking.setStatus(BookingStatus.PENDING);
        booking.setExpiresAt(LocalDateTime.now().plusMinutes(TIMER));
        bookingRepository.save(booking);
        log.info("Initial booking created with ID: {}", booking.getId());
        return new BookingInfoResponse(
                booking.getId(),
                showtimeId,
                booking.getNumberOfSeats(),
                booking.getTotalPrice(),
                booking.getExpiresAt(),
                booking.getStatus().name()
        );
    }

    public BookingInfoResponse lockSeat(SeatRequest request, UUID bookingId, UUID userId) {
        log.info("Attempting to lock seat {} for booking {} by user {} in showtime {}",
                request.seatId(), bookingId, userId, request.showtimeId());

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new CinephileException("Booking not found", HttpStatus.NOT_FOUND));
        BookingSeat bookingSeat = bookingSeatRepository.findByIdAndShowtimeIdForUpdate(request.seatId(), request.showtimeId())
                .orElseThrow(() -> new CinephileException("Seat not found in showtime", HttpStatus.NOT_FOUND));

        if (booking.getExpiresAt().isBefore(LocalDateTime.now())) {
            handleExpiredBooking(booking);
            throw new CinephileException("Booking has expired", HttpStatus.BAD_REQUEST);
        }

        if (bookingSeat.getStatus() != SeatStatus.AVAILABLE) {
            throw new CinephileException("Seat is not available", HttpStatus.CONFLICT);
        }

        int numOfHeldSeats = bookingSeatRepository.countHeldSeatsByBookingId(booking.getId());
        if (numOfHeldSeats >= MAX_SEATS_PER_BOOKING) {
            throw new CinephileException("Cannot hold more than " + MAX_SEATS_PER_BOOKING + " seats for this booking",
                    HttpStatus.BAD_REQUEST);
        }

        bookingSeat.setStatus(SeatStatus.HELD);
        bookingSeat.setBooking(booking);
        bookingSeat.setUser(booking.getUser());
        bookingSeat.setHeldUntil(booking.getExpiresAt());
        bookingSeatRepository.save(bookingSeat);

        booking.setNumberOfSeats(booking.getNumberOfSeats() + 1);
        booking.setTotalPrice(booking.getTotalPrice().add(bookingSeat.getPrice()));
        bookingRepository.save(booking);
        log.info("Successfully locked seat {} for booking {}", bookingSeat.getId(), booking.getId());

        return new BookingInfoResponse(
                booking.getId(),
                request.showtimeId(),
                booking.getNumberOfSeats(),
                booking.getTotalPrice(),
                booking.getExpiresAt(),
                booking.getStatus().name()
        );
    }

    public BookingInfoResponse releaseSeat(SeatRequest request, UUID bookingId, UUID userId) {
        log.info("Attempting to release seat {} for booking {} by user {} in showtime {}",
                request.seatId(), bookingId, userId, request.showtimeId());

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new CinephileException("Booking not found", HttpStatus.NOT_FOUND));
        BookingSeat bookingSeat = bookingSeatRepository.findByIdAndShowtimeId(request.seatId(), request.showtimeId())
                .orElseThrow(() -> new CinephileException("Seat not found in showtime", HttpStatus.NOT_FOUND));

        if (bookingSeat.getStatus() != SeatStatus.HELD || !bookingSeat.getBooking().getId().equals(booking.getId())) {
            throw new CinephileException("Seat is not held by this booking", HttpStatus.BAD_REQUEST);
        }

        bookingSeat.setStatus(SeatStatus.AVAILABLE);
        bookingSeat.setBooking(null);
        bookingSeat.setUser(null);
        bookingSeat.setHeldUntil(null);
        bookingSeatRepository.save(bookingSeat);

        booking.setNumberOfSeats(booking.getNumberOfSeats() - 1);
        booking.setTotalPrice(booking.getTotalPrice().subtract(bookingSeat.getPrice()));
        bookingRepository.save(booking);
        log.info("Successfully released seat {} for booking {}", bookingSeat.getId(), booking.getId());

        return new BookingInfoResponse(
                booking.getId(),
                request.showtimeId(),
                booking.getNumberOfSeats(),
                booking.getTotalPrice(),
                booking.getExpiresAt(),
                booking.getStatus().name()
        );
    }

    // upon confirming booking, send email with QR code for ticket (requires service for QR code)
    public BookingConfirmResponse confirmBooking(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new CinephileException("Booking not found", HttpStatus.NOT_FOUND));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new CinephileException("Booking is not in pending state", HttpStatus.BAD_REQUEST);
        }
        if (booking.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new CinephileException("Booking has expired", HttpStatus.BAD_REQUEST);
        }

        List<BookingSeat> heldSeats = bookingSeatRepository.findByBookingId(bookingId)
                .stream()
                .filter(bs -> bs.getStatus() == SeatStatus.HELD)
                .toList();
        if (heldSeats.isEmpty()) {
            throw new CinephileException("No held seats found for this booking");
        }
        heldSeats.forEach(seat -> {
            seat.setStatus(SeatStatus.BOOKED);
            seat.setHeldUntil(null);
        });
        bookingSeatRepository.saveAll(heldSeats);

        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setConfirmedAt(LocalDateTime.now());
        bookingRepository.save(booking);
        log.info("Successfully confirmed booking {} with {} seats", bookingId, heldSeats.size());

        List<BookedSeatDTO> bookedSeatDTOs = heldSeats.stream()
                .map(bs -> {
                    Seat seat = bs.getSeat();
                    return new BookedSeatDTO(
                            bs.getId(),
                            seat.getId(),
                            seat.getSeatNumber(),
                            seat.getType(),
                            bs.getPrice()
                    );
                }).toList();

        BookingConfirmResponse response = new BookingConfirmResponse(
                booking.getId(),
                booking.getShowtime().getId(),
                booking.getShowtime().getMovie().getTitle(),
                booking.getShowtime().getCinema().getName(),
                booking.getShowtime().getScreen().getName(),
                booking.getShowtime().getStartTime(),
                booking.getNumberOfSeats(),
                booking.getTotalPrice(),
                booking.getConfirmedAt(),
                bookedSeatDTOs
        );
        emailService.sendBookingConfirmationEmail(booking.getUser().getEmail(), response);
        return response;
    }

    public void cancelBooking(UUID bookingId) {
        log.info("Cancelling booking {}", bookingId);
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new CinephileException("Booking not found", HttpStatus.NOT_FOUND));

        if (LocalDateTime.now().isAfter(booking.getShowtime().getStartTime().minusHours(1))) {
            throw new CinephileException("Cannot cancel booking within 1 hour of showtime start",
                            HttpStatus.BAD_REQUEST);
        }

        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.EXPIRED) {
            throw new CinephileException("Booking is already cancelled or expired", HttpStatus.BAD_REQUEST);
        }

        List<BookingSeat> bookingSeats = bookingSeatRepository.findByBookingId(bookingId);
        bookingSeats.forEach(seat -> {
            seat.setStatus(SeatStatus.AVAILABLE);
            seat.setUser(null);
            seat.setBooking(null);
            seat.setHeldUntil(null);
        });
        booking.setStatus(BookingStatus.CANCELLED);
        bookingSeatRepository.saveAll(bookingSeats);
        bookingRepository.save(booking);
        log.info("Successfully cancelled booking {} and released {} seats", bookingId, bookingSeats.size());
    }

    public void handleExpiredBooking(Booking booking) {
        List<BookingSeat> heldSeats = bookingSeatRepository.findByBookingId(booking.getId())
                .stream()
                .filter(bs -> bs.getStatus() == SeatStatus.HELD)
                .toList();
        heldSeats.forEach(seat -> {
            seat.setStatus(SeatStatus.AVAILABLE);
            seat.setBooking(null);
            seat.setUser(null);
            seat.setHeldUntil(null);
        });
        bookingSeatRepository.saveAll(heldSeats);
        booking.setStatus(BookingStatus.EXPIRED);
        bookingRepository.save(booking);
        log.info("Released {} held seats for expired booking {}", heldSeats.size(), booking.getId());
    }

    @Scheduled(fixedRate = 1_800_000) // every 30 minutes
    public void cleanupExpiredBookings() {
        log.info("Running scheduled cleanup of expired bookings");
        List<Booking> expiredBookings = bookingRepository.findByStatusAndExpiresAtBefore(
                BookingStatus.PENDING, LocalDateTime.now());
        log.info("Found {} expired bookings to clean up", expiredBookings.size());
        expiredBookings.forEach(this::handleExpiredBooking);
        log.info("Completed cleanup of expired bookings");
    }

    public Page<BookingCard> getUserBookings(User user, Pageable pageable) {
        Page<Booking> page  = bookingRepository.findAllByUser(user, pageable);
        return page.map(booking -> new BookingCard(
                booking.getId(),
                booking.getShowtime().getMovie().getTitle(),
                booking.getShowtime().getStartTime(),
                booking.getNumberOfSeats(),
                booking.getTotalPrice(),
                booking.getStatus().name()
        ));
    }

    public BookingDetailsResponse getBookingById(UUID bookingId, User user) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new CinephileException("Booking not found", HttpStatus.NOT_FOUND));
        if (!booking.getUser().getId().equals(user.getId())) {
            throw new CinephileException("Access denied to this booking", HttpStatus.FORBIDDEN);
        }
        List<BookedSeatDTO> bookedSeats = bookingSeatRepository.findByBookingId(bookingId).stream()
                .map(bs -> new BookedSeatDTO(
                        bs.getId(),
                        bs.getSeat().getId(),
                        bs.getSeat().getSeatNumber(),
                        bs.getSeat().getType(),
                        bs.getPrice()
                )).toList();
        return new BookingDetailsResponse(
                booking.getId(),
                booking.getShowtime().getMovie().getTitle(),
                booking.getShowtime().getStartTime(),
                booking.getNumberOfSeats(),
                booking.getTotalPrice(),
                booking.getStatus().name(),
                bookedSeats
        );
    }
}



