package com.example.cinephile.user.service;

import com.example.cinephile.auth.entity.AuthUser;
import com.example.cinephile.auth.validation.PasswordValidator;
import com.example.cinephile.common.exception.CinephileException;
import com.example.cinephile.user.dto.UpdateProfileRequest;
import com.example.cinephile.user.dto.UserProfile;
import com.example.cinephile.user.entity.Role;
import com.example.cinephile.user.entity.User;
import com.example.cinephile.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public UserProfile getUserProfile() {
        AuthUser authUser = (AuthUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = authUser.getUser();
        return new UserProfile(user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole().name(),
                user.getCreatedAt());
    }

    @Transactional
    public UserProfile updateUserProfile(UpdateProfileRequest request) {
        AuthUser authUser = (AuthUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = authUser.getUser();

        // check if user wants to update name
        if (request.name() != null && !request.name().isBlank()) {
            user.setName(request.name());
        }

        // check if user wants to update password
        if (request.oldPassword() != null && request.newPassword() != null) {
            // check if old password matches
            if (!passwordEncoder.matches(request.oldPassword(), user.getPassword())) {
                throw new IllegalArgumentException("Old password is incorrect");
            }

            // validate new password
            PasswordValidator passwordValidator = new PasswordValidator();
            if (!passwordValidator.isValid(request.newPassword(), null)) {
                throw new IllegalArgumentException(
                    "Password must be 6-30 characters long, contain at least one uppercase letter, " +
                    "one lowercase letter, one digit, and no special characters");
            }

            // encode and set new password
            user.setPassword(passwordEncoder.encode(request.newPassword()));
        }

        User updatedUser = userRepository.save(user);

        return new UserProfile(updatedUser.getId(),
                updatedUser.getEmail(),
                updatedUser.getName(),
                updatedUser.getRole().name(),
                updatedUser.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public Page<UserProfile> getAllUsers(String name, String email, Role role, Boolean enabled, Pageable pageable) {
        Page<User> page = userRepository.findAllUsers(name, email, role, enabled, pageable);
        return page.map(user -> new UserProfile(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole().name(),
                user.getCreatedAt()
        ));
    }

    @Transactional(readOnly = true)
    public UserProfile getUserById(UUID id) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            throw new CinephileException("User not found", HttpStatus.NOT_FOUND);
        }
        User user = userOpt.get();
        return new UserProfile(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole().name(),
                user.getCreatedAt()
        );
    }
}
