package com.greengrub.donationService.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DonationDetailDTO {

    // Core donation fields (mirrors DonationDTO)
    private DonationDTO donation;

    // Paginated food items hydrated from food-service
    private List<FoodDetailDTO> foodItems;

    // Pagination metadata — totalFoodItems drives the table heading count in UI
    private int totalFoodItems;
    private int currentPage;
    private int pageSize;
    private int totalPages;
}
