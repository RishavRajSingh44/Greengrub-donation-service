package com.greengrub.donationService.controller;

import com.greengrub.donationService.dto.DonationDTO;
import com.greengrub.donationService.entity.DonationStatus;
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/donation")
@Tag(name = "Donation", description = "CRUD operations for food donation listings")
public class DonationController {

    @Autowired
    private DonationService donationService;

    @Operation(summary = "Get all donations", description = "Returns a list of all donation listings on the platform.")
    @ApiResponse(responseCode = "200", description = "Donations retrieved successfully")
    @GetMapping
    public ResponseEntity<List<DonationDTO>> getAllDonation() {
        List<DonationDTO> donations = donationService.getAllDonation();
        return ResponseEntity.ok(donations);
    }

    @Operation(summary = "Get donation by ID", description = "Returns a single donation listing by its UUID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Donation found"),
            @ApiResponse(responseCode = "404", description = "Donation not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<DonationDTO> getDonationById(
            @Parameter(description = "UUID of the donation to retrieve", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String id) {
        DonationDTO donation = donationService.getDonationById(id);
        return ResponseEntity.ok(donation);
    }

    @Operation(summary = "Create a donation", description = "Creates a new food donation listing. Status is set to ACTIVE on creation.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Donation created successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed or malformed request body",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<DonationDTO> createDonation(@Valid @RequestBody DonationDTO request) {
        DonationDTO createdDonation = donationService.createDonation(request);
        return ResponseEntity.ok(createdDonation);
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
        DonationDTO updatedDonation = donationService.updateDonation(id, request);
        return ResponseEntity.ok(updatedDonation);
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

    @Operation(summary = "Update donation status", description = "Updates only the status of a donation. Use this when a volunteer claims or cancels a donation.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status updated successfully"),
            @ApiResponse(responseCode = "404", description = "Donation not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{id}/status")
    public ResponseEntity<DonationDTO> updateDonationStatus(
            @Parameter(description = "UUID of the donation", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String id,
            @Parameter(description = "New status value", example = "CLAIMED")
            @RequestParam DonationStatus status) {
        return ResponseEntity.ok(donationService.updateDonationStatus(id, status));
    }

    @Operation(summary = "Get donations by status", description = "Returns all donations matching the given status. Use ACTIVE to browse available donations.")
    @ApiResponse(responseCode = "200", description = "Donations retrieved successfully")
    @GetMapping("/status/{status}")
    public ResponseEntity<List<DonationDTO>> getDonationsByStatus(
            @Parameter(description = "Status to filter by", example = "ACTIVE")
            @PathVariable DonationStatus status) {
        return ResponseEntity.ok(donationService.getDonationsByStatus(status));
    }

    @Operation(summary = "Get donations by donor", description = "Returns all donations created by a specific donor using their user ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Donations retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "No donations found for this donor",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/donor/{userId}")
    public ResponseEntity<List<DonationDTO>> getDonationsByDonorId(
            @Parameter(description = "UUID of the donor", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String userId) {
        return ResponseEntity.ok(donationService.getDonationsByDonorId(userId));
    }
}
