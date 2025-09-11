package com.example.cinephile.cinema.repository;

import com.example.cinephile.cinema.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SeatRepository extends JpaRepository<Seat, UUID> {
    void deleteByScreenId(UUID screenId);

    List<Seat> findByScreenId(UUID screenId);
}
