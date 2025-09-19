package com.example.cinephile.user.controller;

import com.example.cinephile.user.dto.UpdateProfileRequest;
import com.example.cinephile.user.dto.UserProfile;
import com.example.cinephile.user.entity.Role;
import com.example.cinephile.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserProfile> getUserProfile() {
        return ResponseEntity.ok(userService.getUserProfile());
    }

    @PutMapping("/me")
    public ResponseEntity<UserProfile> updateUserProfile(@RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateUserProfile(request));
    }

    @GetMapping
    public ResponseEntity<Page<UserProfile>> getAllUsers(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        page = Math.max(page, 0);
        size = size < 0 ? 20 : Math.min(size, 20);
        Pageable pageable = PageRequest.of(page, size);
        Role roleEnum = role != null ? Role.valueOf(role) : null;
        return ResponseEntity.ok(userService.getAllUsers(name, email, roleEnum, enabled, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserProfile> getUserById(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }
}
