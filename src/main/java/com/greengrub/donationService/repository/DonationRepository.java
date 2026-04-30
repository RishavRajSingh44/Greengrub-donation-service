package com.greengrub.donationService.repository;

import com.greengrub.donationService.entity.Donation;
import com.greengrub.donationService.entity.DonationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DonationRepository extends JpaRepository<Donation, String> {
    List<Donation> findAll();
    List<Donation> findByStatus(DonationStatus status);
    List<Donation> findByDonarDetailsUserId(String userId);
}
