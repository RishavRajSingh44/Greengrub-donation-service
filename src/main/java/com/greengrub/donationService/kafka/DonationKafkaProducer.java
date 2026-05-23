package com.greengrub.donationService.kafka;

import com.greengrub.donationService.exception.KafkaPublishException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class DonationKafkaProducer {

    private final KafkaTemplate<String, DonationEventDTO> kafkaTemplate;
    private final String topic;

    public DonationKafkaProducer(
            KafkaTemplate<String, DonationEventDTO> kafkaTemplate,
            @Value("${donation.kafka.topic}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    @Retry(name = "kafkaPublishRetry")
    @CircuitBreaker(name = "kafkaPublishBreaker", fallbackMethod = "publishFallback")
    public void publish(DonationEventDTO event) {
        try {
            kafkaTemplate.send(topic, event.donationId(), event)
                         .get(5, TimeUnit.SECONDS);
            log.info("Published donation event [{}] to topic {}", event.donationId(), topic);
        } catch (Exception e) {
            throw new KafkaPublishException(event.donationId(), e);
        }
    }

    // package-private — must be visible to Spring AOP proxy
    void publishFallback(DonationEventDTO event, Throwable t) {
        log.warn("Kafka unavailable for donation [{}] — circuit open or retries exhausted: {}",
                 event.donationId(), t.getMessage());
        throw new KafkaPublishException(event.donationId(), t);
    }
}
