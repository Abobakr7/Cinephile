package com.example.cinephile.auth.dto;

import com.example.cinephile.user.entity.Role;

public record AuthResponse(String accessToken,
                           String refreshToken,
                           String email,
                           Role role,
                           String message) {
}
