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
@Schema(description = "Personal details of the donor")
public class UserDetailDTO {

    @Schema(description = "Unique identifier of the donor", example = "550e8400-e29b-41d4-a716-446655440000", accessMode = Schema.AccessMode.READ_ONLY)
    private String userId;

    @NotBlank(message = "First name is required")
    @Schema(description = "Donor's first name", example = "Jane")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Schema(description = "Donor's last name", example = "Doe")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid format (e.g. user@example.com)")
    @Schema(description = "Donor's email address", example = "jane.doe@example.com")
    private String email;

    @NotBlank(message = "Phone number is required")
    @Schema(description = "Donor's contact phone number", example = "9876543210")
    private String phone;
}
