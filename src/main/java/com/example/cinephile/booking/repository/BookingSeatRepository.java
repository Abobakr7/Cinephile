package com.example.cinephile.booking.repository;

import com.example.cinephile.booking.entity.BookingSeat;
import com.example.cinephile.booking.entity.SeatStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookingSeatRepository extends JpaRepository<BookingSeat, UUID> {

    Optional<BookingSeat> findByIdAndShowtimeId(UUID bookedSeatId, UUID showtimeId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT bs FROM BookingSeat bs WHERE bs.id = :bookedSeatId AND bs.showtime.id = :showtimeId")
    Optional<BookingSeat> findByIdAndShowtimeIdForUpdate(UUID bookedSeatId,
                                                         UUID showtimeId);

    @Query("SELECT bs FROM BookingSeat bs WHERE bs.showtime.id = :showtimeId AND bs.status IN :statuses")
    List<BookingSeat> findByShowtimeAndStatuses(UUID showtimeId,
                                                List<SeatStatus> statuses);

    @Query("SELECT bs FROM BookingSeat bs WHERE bs.booking.id = :bookingId")
    List<BookingSeat> findByBookingId(UUID bookingId);

    @Query("SELECT COUNT(bs) FROM BookingSeat bs WHERE bs.showtime.id = :showtimeId AND bs.status = 'AVAILABLE'")
    long countAvailableSeats(UUID showtimeId);

    void deleteByShowtimeId(UUID showtimeId);

    List<BookingSeat> findByShowtimeId(UUID showtimeId);

    @Query("SELECT COUNT(bs) FROM BookingSeat bs WHERE bs.booking.id = :id AND bs.status = 'HELD'")
    int countHeldSeatsByBookingId(UUID id);
}
