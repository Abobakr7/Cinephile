package com.example.cinephile.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class CinephileException extends RuntimeException {
    private HttpStatus status;

    public CinephileException(String message) {
        super(message);
        this.status = HttpStatus.BAD_REQUEST;
    }

    public CinephileException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }
}
