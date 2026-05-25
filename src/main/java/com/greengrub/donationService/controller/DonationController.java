package com.greengrub.donationService.controller;

import com.greengrub.donationService.dto.DonationDTO;
import com.greengrub.donationService.dto.DonationDetailDTO;
import com.greengrub.donationService.exception.ErrorResponse;
import com.greengrub.donationService.service.DonationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/donations")
@Tag(name = "Donation", description = "CRUD operations for food donation listings")
public class DonationController {

    private final DonationService donationService;

    public DonationController(DonationService donationService) {
        this.donationService = donationService;
    }

    @Operation(summary = "Get all donations", description = "Returns a list of all donation listings on the platform.")
    @ApiResponse(responseCode = "200", description = "Donations retrieved successfully")
    @GetMapping
    public ResponseEntity<List<DonationDTO>> getAllDonation() {
        return ResponseEntity.ok(donationService.getAllDonation());
    }

    @Operation(
        summary = "Get donation detail by ID",
        description = "Returns a donation with its paginated food items hydrated from food-service. " +
                      "totalFoodItems in the response is the full count for the table heading. " +
                      "Food items degrade to an empty list if food-service is temporarily unavailable."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Donation found"),
            @ApiResponse(responseCode = "404", description = "Donation not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "503", description = "Dependent service temporarily unavailable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<DonationDetailDTO> getDonationDetail(
            @Parameter(description = "UUID of the donation to retrieve", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String id,
            @Parameter(description = "0-based page index for food items", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of food items per page (default 10)", example = "10")
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(donationService.getDonationDetail(id, page, size));
    }

    @Operation(summary = "Create a donation", description = "Creates a new food donation listing. Status is set to ACTIVE on creation.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Donation created successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed or malformed request body",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<DonationDTO> createDonation(@Valid @RequestBody DonationDTO request) {
        return ResponseEntity.ok(donationService.createDonation(request));
    }

    @Operation(summary = "Update a donation", description = "Updates an existing donation listing. Use this to change status to CLAIMED or CANCELLED.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Donation updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed or malformed request body",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Donation not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<DonationDTO> updateDonation(
            @Parameter(description = "UUID of the donation to update", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String id,
            @Valid @RequestBody DonationDTO request) {
        return ResponseEntity.ok(donationService.updateDonation(id, request));
    }

    @Operation(summary = "Delete a donation", description = "Permanently removes a donation listing by its UUID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Donation deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Donation not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteDonation(
            @Parameter(description = "UUID of the donation to delete", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String id) {
        donationService.deleteDonation(id);
        return ResponseEntity.ok("Donation deleted successfully");
    }
}
