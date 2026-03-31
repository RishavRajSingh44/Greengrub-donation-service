package com.greengrub.donationService.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Contact email of the donor")
public class EmailDTO {

    @NotBlank(message = "Email address is required")
    @Email(message = "Email address must be a valid format (e.g. user@example.com)")
    @Schema(description = "A valid email address", example = "jane.doe@example.com")
    private String emailAddress;
}
