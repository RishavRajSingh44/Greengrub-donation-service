package com.greengrub.donationService.dto;

import com.greengrub.donationService.entity.DonationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Donation listing details")
public class DonationDTO {

    @Schema(description = "Unique identifier of the donation", example = "550e8400-e29b-41d4-a716-446655440000", accessMode = Schema.AccessMode.READ_ONLY)
    private String id;

    @NotBlank(message = "Donation name is required")
    @Schema(description = "Name or description of the food being donated", example = "Wedding Buffet Leftovers")
    private String donationName;

    @NotNull(message = "Donor details are required")
    @Valid
    @Schema(description = "Personal details of the donor")
    private UserDetailDTO donarDetails;

    @NotBlank(message = "Pick-up address is required")
    @Schema(description = "Address where the food can be picked up", example = "123 Main St, Springfield")
    private String pickUpAddress;

    @NotNull(message = "Pick-up time is required")
    @Schema(description = "Scheduled pick-up date and time", example = "2026-05-01T14:30:00")
    private LocalDateTime pickUpTime;

    @NotNull(message = "Estimated quantity is required")
    @Valid
    @Schema(description = "Estimated net quantity of all food items")
    private QuantityDTO estimatedQuantity;

    @Schema(description = "List of food item IDs associated with this donation")
    private List<String> foodItemsId = new ArrayList<>();

    @NotNull(message = "Status is required. Accepted values: ACTIVE, CLAIMED, CANCELLED")
    @Schema(description = "Current status of the donation", example = "ACTIVE", allowableValues = {"ACTIVE", "CLAIMED", "CANCELLED"})
    private DonationStatus status;

    @Schema(description = "Timestamp when the donation was created", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime creationDate;

    @Schema(description = "Timestamp when the donation was last updated", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime updateDate;
}
