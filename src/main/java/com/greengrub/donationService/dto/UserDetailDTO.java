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

    @NotBlank(message = "First name is required")
    @Schema(description = "Donor's first name", example = "Jane")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Schema(description = "Donor's last name", example = "Doe")
    private String lastName;

    @NotBlank(message = "Phone number is required")
    @Schema(description = "Donor's contact phone number", example = "9876543210")
    private String phoneNumber;

    @NotBlank(message = "Email address is required")
    @Email(message = "Email address must be a valid format (e.g. user@example.com)")
    @Schema(description = "Donor's email address", example = "jane.doe@example.com")
    private String emailAddress;
}
