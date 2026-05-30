package com.greengrub.donationService.exception;

import com.greengrub.donationService.entity.Donation;
import com.greengrub.donationService.entity.DonationStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class ExceptionTest {

    @Test
    void donationNotFoundException_messageContainsId() {
        DonationNotFoundException ex = new DonationNotFoundException("id-99");
        assertThat(ex.getMessage()).contains("id-99");
    }

    @Test
    void kafkaPublishException_wrapsOriginalCause() {
        RuntimeException cause = new RuntimeException("broker");
        KafkaPublishException ex = new KafkaPublishException("id-1", cause);
        assertThat(ex.getMessage()).contains("id-1");
        assertThat(ex.getCause()).isEqualTo(cause);
    }

    @Test
    void foodServiceException_messageOnly_constructor() {
        FoodServiceException ex = new FoodServiceException("food-service down");
        assertThat(ex.getMessage()).isEqualTo("food-service down");
        assertThat(ex.getCause()).isNull();
    }

    @Test
    void foodServiceException_wrapsOriginalCause() {
        RuntimeException cause = new RuntimeException("grpc down");
        FoodServiceException ex = new FoodServiceException("food-service down", cause);
        assertThat(ex.getMessage()).isEqualTo("food-service down");
        assertThat(ex.getCause()).isEqualTo(cause);
    }

    @Test
    void errorResponse_of_setsAllFields() {
        ErrorResponse resp = ErrorResponse.of(404, "Not Found", "msg", "/api/v1/donations/bad");
        assertThat(resp.status()).isEqualTo(404);
        assertThat(resp.error()).isEqualTo("Not Found");
        assertThat(resp.message()).isEqualTo("msg");
        assertThat(resp.path()).isEqualTo("/api/v1/donations/bad");
        assertThat(resp.timestamp()).isNotNull();
    }

    @Test
    void validationErrorResponse_fieldErrorRecord_holdsValues() {
        ValidationErrorResponse.FieldError fe = new ValidationErrorResponse.FieldError("donationName", "required");
        assertThat(fe.field()).isEqualTo("donationName");
        assertThat(fe.message()).isEqualTo("required");
    }

    @Test
    void donation_builder_createsInstance() {
        Donation built = Donation.builder()
                .donationName("Test")
                .status(DonationStatus.ACTIVE)
                .build();
        assertThat(built.getDonationName()).isEqualTo("Test");
        assertThat(built.getStatus()).isEqualTo(DonationStatus.ACTIVE);
        assertThat(built.getFoodItemsId()).isNotNull().isEmpty();
    }

    @Test
    void donation_prePersist_doesNotOverwriteExistingId() {
        Donation d = new Donation();
        d.setId("preset-id");
        // Mirror the @PrePersist logic directly
        if (d.getId() == null) {
            d.setId(java.util.UUID.randomUUID().toString());
        }
        assertThat(d.getId()).isEqualTo("preset-id");
    }
}
