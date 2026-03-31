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

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Donation listing details")
public class DonationDTO {

    @Schema(description = "Unique identifier of the donation", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @NotBlank(message = "Donation name is required")
    @Schema(description = "Name or description of the food being donated", example = "Wedding Buffet Leftovers")
    private String donationName;

    @NotNull(message = "Donor details are required")
    @Valid
    @Schema(description = "Personal details of the donor")
    private UserDetailDTO userDetail;

    @NotNull(message = "Email is required")
    @Valid
    @Schema(description = "Contact email of the donor")
    private EmailDTO email;

    @Schema(description = "Timestamp when the donation was created", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime creationDate;

    @Schema(description = "Timestamp when the donation was last updated", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime updateDate;

    @NotNull(message = "Status is required. Accepted values: ACTIVE, CLAIMED, CANCELLED")
    @Schema(description = "Current status of the donation", example = "ACTIVE", allowableValues = {"ACTIVE", "CLAIMED", "CANCELLED"})
    private DonationStatus status;
}
