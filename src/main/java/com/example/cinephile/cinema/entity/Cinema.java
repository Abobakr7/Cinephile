package com.example.cinephile.cinema.entity;

import com.example.cinephile.user.entity.User;
import com.fasterxml.uuid.Generators;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "cinemas")
@Getter @Setter
public class Cinema {
    @Id
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID id;
    private String name;
    private String address;
    private String phone;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User manager;

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
