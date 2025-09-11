package com.example.cinephile.cinema.repository;

import com.example.cinephile.cinema.entity.Screen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ScreenRepository extends JpaRepository<Screen, UUID> {
    boolean existsByCinemaIdAndNameIgnoreCase(UUID cinemaId, String name);

    boolean existsByCinemaIdAndNameIgnoreCaseAndIdNot(UUID cinemaId, String name, UUID id);

    List<Screen> findByCinemaId(UUID cinemaId);
}
