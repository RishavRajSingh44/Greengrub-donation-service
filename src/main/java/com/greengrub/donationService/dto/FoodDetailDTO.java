package com.greengrub.donationService.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodDetailDTO {

    private String id;
    private String foodName;
    private Double quantityAmount;
    private String quantityUnit;
    private String status;
    private String requestedBy;
    private String requestedDate;
    private String usedByDate;
}
