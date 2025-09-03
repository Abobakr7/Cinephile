package com.example.cinephile.auth.dto;

import com.example.cinephile.auth.validation.ValidPassword;
import jakarta.validation.constraints.NotBlank;

public record ResetPasswordRequest(@ValidPassword String newPassword,
                                   @NotBlank(message = "Token must not be blank") String token) {
}
