package com.example.cinephile.booking.repository;

import com.example.cinephile.booking.entity.Booking;
import com.example.cinephile.booking.entity.BookingStatus;
import com.example.cinephile.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {

    List<Booking> findByStatusAndExpiresAtBefore(BookingStatus bookingStatus, LocalDateTime now);

    Page<Booking> findAllByUser(User user, Pageable pageable);
}
