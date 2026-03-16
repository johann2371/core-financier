package com.corefi.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class SeuilPDGRequiredException extends RuntimeException {
    public SeuilPDGRequiredException(String message) {
        super(message);
    }
}
