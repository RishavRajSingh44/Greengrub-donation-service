package com.greengrub.donationService.exception;

public class FoodServiceException extends RuntimeException {

    public FoodServiceException(String message) {
        super(message);
    }

    public FoodServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
