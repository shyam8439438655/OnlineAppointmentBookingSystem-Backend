package com.bridgelabz.recordservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class RecordEditingWindowExpiredException extends RuntimeException {
    public RecordEditingWindowExpiredException(String message) {
        super(message);
    }
}
