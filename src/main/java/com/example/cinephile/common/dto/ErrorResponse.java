package com.example.cinephile.common.dto;

import java.time.LocalDateTime;

public record ErrorResponse(int status, String message, LocalDateTime timestamp) {
}
