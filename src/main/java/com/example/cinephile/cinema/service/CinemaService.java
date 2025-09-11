package com.example.cinephile.cinema.service;

import com.example.cinephile.auth.entity.AuthUser;
import com.example.cinephile.common.exception.CinephileException;
import com.example.cinephile.cinema.dto.CinemaRequest;
import com.example.cinephile.cinema.dto.CinemaResponse;
import com.example.cinephile.cinema.entity.Cinema;
import com.example.cinephile.cinema.entity.Screen;
import com.example.cinephile.cinema.repository.CinemaRepository;
import com.example.cinephile.cinema.repository.ScreenRepository;
import com.example.cinephile.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CinemaService {
    private final CinemaRepository cinemaRepository;
    private final ScreenService screenService;
    private final ScreenRepository screenRepository;

    // get all cinemas
    @Transactional(readOnly = true)
    public Page<CinemaResponse> getCinemas(String search, Pageable pageable) {
        Page<Cinema> page = (search == null || search.isBlank())
                ? cinemaRepository.findAll(pageable)
                : cinemaRepository.findByNameContainingIgnoreCase(search, pageable);

        return page.map(cinema -> new CinemaResponse(
                cinema.getId(), cinema.getName(), cinema.getAddress(),
                cinema.getPhone(), null
        ));
    }

    // get all cinemas managed by manager [owner]
    @Transactional(readOnly = true)
    public List<CinemaResponse> getCinemasManagedByManager() {
        AuthUser authUser = (AuthUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = authUser.getUser();
        List<Cinema> cinemas = cinemaRepository.findByManagerId(user.getId());
        if (cinemas.isEmpty()) {
            throw new CinephileException("No cinemas found for this manager", HttpStatus.NOT_FOUND);
        }

        return cinemas.stream().map(cinema -> new CinemaResponse(
                cinema.getId(), cinema.getName(), cinema.getAddress(),
                cinema.getPhone(), null
        )).toList();
    }

    @Transactional(readOnly = true)
    public CinemaResponse getCinemaById(UUID cinemaId) {
        Cinema cinema = cinemaRepository.findById(cinemaId)
                .orElseThrow(() -> new CinephileException("Cinema not found", HttpStatus.NOT_FOUND));
        return new CinemaResponse(
                cinema.getId(), cinema.getName(), cinema.getAddress(),
                cinema.getPhone(), cinema.getManager().getId()
        );
    }

    @Transactional
    public CinemaResponse createCinema(CinemaRequest request) {
        AuthUser authUser = (AuthUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = authUser.getUser();
        Cinema cinema = new Cinema();
        cinema.setName(request.name());
        cinema.setAddress(request.address());
        cinema.setPhone(request.phone());
        cinema.setManager(user);
        cinemaRepository.save(cinema);
        return new CinemaResponse(
                cinema.getId(), cinema.getName(), cinema.getAddress(),
                cinema.getPhone(), null
        );
    }

    @Transactional
    public CinemaResponse updateCinema(UUID cinemaId, CinemaRequest request) {
        Cinema cinema = cinemaRepository.findById(cinemaId)
                .orElseThrow(() -> new CinephileException("Cinema not found", HttpStatus.NOT_FOUND));
        cinema.setName(request.name());
        cinema.setAddress(request.address());
        cinema.setPhone(request.phone());
        cinemaRepository.save(cinema);
        return new CinemaResponse(
                cinema.getId(), cinema.getName(), cinema.getAddress(),
                cinema.getPhone(), null
        );
    }

    @Transactional
    public void deleteCinema(UUID cinemaId) {
        Cinema cinema = cinemaRepository.findById(cinemaId)
                .orElseThrow(() -> new CinephileException("Cinema not found", HttpStatus.NOT_FOUND));

        List<UUID> screenIds = screenRepository.findByCinemaId(cinema.getId())
                .stream().map(Screen::getId).toList();
        screenIds.forEach(screenService::deleteScreen);
        cinemaRepository.delete(cinema);
    }
}
