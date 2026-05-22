package com.greengrub.donationService.exception;

public class KafkaPublishException extends RuntimeException {

    public KafkaPublishException(String donationId, Throwable cause) {
        super("Failed to publish Kafka event for donation " + donationId, cause);
    }
}
