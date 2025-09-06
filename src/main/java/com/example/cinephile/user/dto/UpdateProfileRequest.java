package com.example.cinephile.user.dto;

public record UpdateProfileRequest(String name, String oldPassword, String newPassword) {
}
