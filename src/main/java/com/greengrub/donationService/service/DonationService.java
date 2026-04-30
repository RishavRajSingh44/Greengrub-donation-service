package com.greengrub.donationService.service;

import com.greengrub.donationService.dto.DonationDTO;
import com.greengrub.donationService.entity.DonationStatus;

import java.util.List;

public interface DonationService {

    DonationDTO createDonation(DonationDTO request);

    List<DonationDTO> getAllDonation();

    DonationDTO getDonationById(String id);

    DonationDTO updateDonation(String id, DonationDTO request);

    void deleteDonation(String id);

    DonationDTO updateDonationStatus(String id, DonationStatus status);

    List<DonationDTO> getDonationsByStatus(DonationStatus status);

    List<DonationDTO> getDonationsByDonorId(String userId);
}
