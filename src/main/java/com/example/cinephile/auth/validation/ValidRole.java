package com.example.cinephile.auth.validation;

import jakarta.validation.Constraint;

import java.lang.annotation.*;

@Target({ ElementType.TYPE, ElementType.FIELD, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = RoleValidator.class)
@Documented
public @interface ValidRole {
    String message() default "Role must be either 'USER', 'MANAGER', or 'ADMIN";
    Class<?>[] groups() default {};
    Class<? extends jakarta.validation.Payload>[] payload() default {};
}
