package com.example.cinephile.cinema.service;

import com.example.cinephile.common.exception.CinephileException;
import com.example.cinephile.cinema.entity.Seat;
import com.example.cinephile.cinema.entity.SeatType;
import com.example.cinephile.cinema.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class SeatService {
    private final SeatRepository seatRepository;

    public void updateSeatType(UUID seatId, String newType) {
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new CinephileException("Seat not found", HttpStatus.NOT_FOUND));
        try {
            SeatType seatType = SeatType.valueOf(newType.toUpperCase());
            seat.setType(seatType);
            seatRepository.save(seat);
        } catch (IllegalArgumentException e) {
            throw new CinephileException("Invalid seat type", HttpStatus.BAD_REQUEST);
        }
    }
}
