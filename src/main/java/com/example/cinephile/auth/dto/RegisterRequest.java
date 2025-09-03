package com.example.cinephile.auth.dto;

import com.example.cinephile.auth.validation.ValidPassword;
import com.example.cinephile.auth.validation.ValidRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(@NotBlank(message = "Email is required") @Email(message = "Invalid email format") String email,
                              @NotBlank(message = "Name is required") String name,
                              @ValidPassword String password,
                              @ValidRole String role) {
}
