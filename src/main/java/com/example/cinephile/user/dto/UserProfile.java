package com.example.cinephile.user.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserProfile(UUID id, String email, String name, String role, LocalDateTime createdAt) {
}
