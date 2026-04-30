package com.greengrub.donationService.dto;

import com.greengrub.donationService.entity.Unit;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Quantity of food being donated")
public class QuantityDTO {

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    @Schema(description = "Numeric amount of food", example = "5.0")
    private Double amount;

    @NotNull(message = "Unit is required. Accepted values: KG, SERVINGS")
    @Schema(description = "Unit of measurement", example = "KG", allowableValues = {"KG", "SERVINGS"})
    private Unit unit;
}
