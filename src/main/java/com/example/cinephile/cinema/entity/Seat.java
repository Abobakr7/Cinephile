package com.example.cinephile.cinema.entity;

import com.fasterxml.uuid.Generators;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "seats")
@Getter @Setter
public class Seat {
    @Id
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "screen_id")
    private Screen screen;

    @Column(name = "seat_number")
    private String seatNumber;

    @Column(name = "row_name")
    private Character rowName;

    @Column(name = "seat_position")
    private int seatPosition;

    @Enumerated(EnumType.STRING)
    private SeatType type;

    @Column(name = "is_active")
    private boolean isActive;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = Generators.timeBasedEpochGenerator().generate();
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        isActive = true;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
