package com.bridgelabz.appointmentservice.exception;

public class InvalidSlotException extends RuntimeException {
    public InvalidSlotException(String message) {
        super(message);
    }
}
