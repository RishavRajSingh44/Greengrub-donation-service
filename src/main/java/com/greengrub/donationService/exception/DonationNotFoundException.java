package com.greengrub.donationService.exception;

public class DonationNotFoundException extends RuntimeException {

    public DonationNotFoundException(String id) {
        super("No donation found with ID " + id + ". It may have been deleted or never existed.");
    }
}
