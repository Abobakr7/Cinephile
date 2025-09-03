package com.example.cinephile.auth.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class RoleValidator implements ConstraintValidator<ValidRole, String> {
    @Override
    public boolean isValid(String role, ConstraintValidatorContext context) {
        return role != null && (role.equals("USER") || role.equals("MANAGER") || role.equals("ADMIN"));
    }
}
