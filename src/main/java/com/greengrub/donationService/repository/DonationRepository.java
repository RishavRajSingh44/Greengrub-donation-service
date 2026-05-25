package com.greengrub.donationService.repository;

import com.greengrub.donationService.entity.Donation;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DonationRepository extends JpaRepository<Donation, String> {

    // JOIN FETCH foodItemsId to prevent N+1 on single-record load
    @EntityGraph(attributePaths = "foodItemsId")
    Optional<Donation> findById(String id);

    // JOIN FETCH foodItemsId for batch lookup (used by getAllDonation)
    @EntityGraph(attributePaths = "foodItemsId")
    List<Donation> findAll();
}
