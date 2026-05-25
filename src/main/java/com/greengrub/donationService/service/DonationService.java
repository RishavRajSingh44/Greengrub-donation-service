package com.greengrub.donationService.service;

import com.greengrub.donationService.dto.DonationDTO;
import com.greengrub.donationService.dto.DonationDetailDTO;

import java.util.List;

public interface DonationService {

    DonationDTO createDonation(DonationDTO request);

    List<DonationDTO> getAllDonation();

    // Returns enriched donation detail with paginated food items from food-service
    DonationDetailDTO getDonationDetail(String id, int page, int size);

    DonationDTO updateDonation(String id, DonationDTO request);

    void deleteDonation(String id);
}
