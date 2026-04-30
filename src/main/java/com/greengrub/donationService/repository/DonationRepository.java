package com.greengrub.donationService.repository;

import com.greengrub.donationService.entity.Donation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DonationRepository extends JpaRepository<Donation, String> {
    List<Donation> findAll();
}
