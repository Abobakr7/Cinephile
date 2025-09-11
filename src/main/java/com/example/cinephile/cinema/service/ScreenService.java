package com.example.cinephile.cinema.service;

import com.example.cinephile.common.exception.CinephileException;
import com.example.cinephile.cinema.dto.*;
import com.example.cinephile.cinema.entity.Cinema;
import com.example.cinephile.cinema.entity.Screen;
import com.example.cinephile.cinema.entity.Seat;
import com.example.cinephile.cinema.entity.SeatType;
import com.example.cinephile.cinema.repository.CinemaRepository;
import com.example.cinephile.cinema.repository.ScreenRepository;
import com.example.cinephile.cinema.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ScreenService {
    private final CinemaRepository cinemaRepository;
    private final ScreenRepository screenRepository;
    private final SeatRepository seatRepository;

    @Transactional(readOnly = true)
    public List<ScreenCard> getScreens(UUID cinemaId) {
        if (!cinemaRepository.existsById(cinemaId)) {
            throw new CinephileException("Cinema not found", HttpStatus.NOT_FOUND);
        }

        List<Screen> screens = screenRepository.findByCinemaId(cinemaId);
        if (screens.isEmpty()) {
            throw new CinephileException("No screens found for this cinema", HttpStatus.NOT_FOUND);
        }

        return screens.stream().map(screen -> new ScreenCard(
                screen.getId(), screen.getName(), screen.getCapacity()
        )).toList();
    }

    @Transactional(readOnly = true)
    public ScreenDetail getScreenById(UUID screenId) {
        Screen screen = screenRepository.findById(screenId)
                .orElseThrow(() -> new CinephileException("Screen not found", HttpStatus.NOT_FOUND));

        List<Seat> seats = seatRepository.findByScreenId(screenId);

        return new ScreenDetail(
                screen.getId(), screen.getName(), screen.getCapacity(),
                seats.stream().map(seat -> new SeatDTO(
                        seat.getId(), seat.getSeatNumber(), seat.getRowName(),
                        seat.getSeatPosition(), seat.getType()
                )).toList()
        );
    }

    @Transactional
    public void createScreen(UUID cinemaId, CreateScreenRequest request) {
        Cinema cinema = cinemaRepository.findById(cinemaId)
                .orElseThrow(() -> new CinephileException("Cinema not found", HttpStatus.NOT_FOUND));

        if (screenRepository.existsByCinemaIdAndNameIgnoreCase(cinemaId, request.name())) {
            throw new CinephileException("Screen name already exists in this cinema", HttpStatus.BAD_REQUEST);
        }

        Screen screen = new Screen();
        screen.setName(request.name());
        screen.setCapacity(request.capacity());
        screen.setCinema(cinema);
        screenRepository.save(screen);

        int numRows = request.numRows();
        int numCols = request.numCols();
        if (numRows * numCols != request.capacity()) {
            throw new CinephileException("Capacity does not match number of rows and columns", HttpStatus.BAD_REQUEST);
        }

        List<Seat> seats = new ArrayList<>();
        for (int row = 65; row < 65 + numRows; row++) { // ASCII A=65
            for (int col = 1; col <= numCols; col++) {
                Seat seat = new Seat();
                seat.setRowName((char) row);
                seat.setSeatPosition(col);
                seat.setSeatNumber((char) row + String.valueOf(col));
                seat.setType(SeatType.STANDARD); // default type
                seat.setScreen(screen);
                seats.add(seat);
            }
        }
        seatRepository.saveAll(seats);
    }

    @Transactional
    public void updateScreen(UUID screenId, UpdateScreenRequest request) {
        Screen screen = screenRepository.findById(screenId)
                .orElseThrow(() -> new CinephileException("Screen not found", HttpStatus.NOT_FOUND));
        UUID cinemaId = screen.getCinema().getId();
        if (screenRepository.existsByCinemaIdAndNameIgnoreCaseAndIdNot(cinemaId, request.name(), screenId)) {
            throw new CinephileException("Screen name already exists in this cinema", HttpStatus.BAD_REQUEST);
        }
        screen.setName(request.name());
        screenRepository.save(screen);
    }

    @Transactional
    public void deleteScreen(UUID screenId) {
        Screen screen = screenRepository.findById(screenId)
                .orElseThrow(() -> new CinephileException("Screen not found", HttpStatus.NOT_FOUND));
        seatRepository.deleteByScreenId(screenId);
        screenRepository.delete(screen);
    }
}
