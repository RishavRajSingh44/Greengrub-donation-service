package com.greengrub.donationService.service;

import com.greengrub.donationService.dto.DonationDTO;
import java.util.List;

public interface DonationService {

    DonationDTO createDonation(DonationDTO request);

    List<DonationDTO> getAllDonation();

    DonationDTO getDonationById(String id);

    DonationDTO updateDonation(String id, DonationDTO request);

    void deleteDonation(String id);
}
