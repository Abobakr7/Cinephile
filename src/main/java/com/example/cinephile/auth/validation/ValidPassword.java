package com.example.cinephile.auth.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ ElementType.TYPE, ElementType.FIELD, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PasswordValidator.class)
@Documented
public @interface ValidPassword {
    String message() default
            "Password must be 6-30 characters long, contain at least one uppercase letter, " +
            "one lowercase letter, one digit, and no special characters";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

}
