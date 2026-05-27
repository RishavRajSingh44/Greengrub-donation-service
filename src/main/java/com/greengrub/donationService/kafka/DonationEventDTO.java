package com.greengrub.donationService.kafka;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record DonationEventDTO(
        String donationId,
        String donorName,
        String donorEmail,
        BigDecimal totalAmount,
        LocalDateTime createdAt,
        String organizationName,
        String status,
        CustomerDTO customer,
        List<DonationItemDTO> items
) {
    public record DonationItemDTO(
            String foodName,
            Integer quantity,
            String unit,
            String category
    ) {}

    public record CustomerDTO(
            String id,
            String firstname,
            String lastname,
            String email,
            String phone
    ) {}
}
