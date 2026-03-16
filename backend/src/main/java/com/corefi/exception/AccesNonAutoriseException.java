package com.corefi.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class AccesNonAutoriseException extends RuntimeException {
    public AccesNonAutoriseException(String message) {
        super(message);
    }
}
