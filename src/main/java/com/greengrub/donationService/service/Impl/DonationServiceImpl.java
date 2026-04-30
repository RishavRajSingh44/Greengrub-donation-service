package com.greengrub.donationService.service.Impl;

import com.greengrub.donationService.dto.DonationDTO;
import com.greengrub.donationService.dto.QuantityDTO;
import com.greengrub.donationService.dto.UserDetailDTO;
import com.greengrub.donationService.entity.Donation;
import com.greengrub.donationService.entity.Quantity;
import com.greengrub.donationService.entity.UserDetail;
import com.greengrub.donationService.exception.DonationNotFoundException;
import com.greengrub.donationService.repository.DonationRepository;
import com.greengrub.donationService.service.DonationService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DonationServiceImpl implements DonationService {

    @Autowired
    private DonationRepository donationRepository;

    @Override
    public DonationDTO createDonation(DonationDTO request) {
        Donation donation = mapToEntity(request);
        Donation savedDonation = donationRepository.save(donation);
        return mapToDTO(savedDonation);
    }

    @Override
    public List<DonationDTO> getAllDonation() {
        return donationRepository.findAll()
            .stream()
            .map(this::mapToDTO)
            .collect(Collectors.toList());
    }

    @Override
    public DonationDTO getDonationById(String id) {
        Donation donation = donationRepository.findById(id)
            .orElseThrow(() -> new DonationNotFoundException(id));
        return mapToDTO(donation);
    }

    @Override
    public DonationDTO updateDonation(String id, DonationDTO request) {
        Donation donation = donationRepository.findById(id)
            .orElseThrow(() -> new DonationNotFoundException(id));

        donation.setDonationName(request.getDonationName());
        donation.setDonarDetails(mapToUserDetailEntity(request.getDonarDetails()));
        donation.setPickUpAddress(request.getPickUpAddress());
        donation.setPickUpTime(request.getPickUpTime());
        donation.setEstimatedQuantity(mapToQuantityEntity(request.getEstimatedQuantity()));
        donation.setFoodItemsId(request.getFoodItemsId() != null ? request.getFoodItemsId() : new ArrayList<>());
        donation.setStatus(request.getStatus());

        Donation updatedDonation = donationRepository.save(donation);
        return mapToDTO(updatedDonation);
    }

    @Override
    public void deleteDonation(String id) {
        Donation donation = donationRepository.findById(id)
            .orElseThrow(() -> new DonationNotFoundException(id));
        donationRepository.delete(donation);
    }

    // ---------------- MAPPING METHODS ----------------

    private Donation mapToEntity(DonationDTO dto) {
        Donation donation = new Donation();
        donation.setDonationName(dto.getDonationName());
        donation.setDonarDetails(mapToUserDetailEntity(dto.getDonarDetails()));
        donation.setPickUpAddress(dto.getPickUpAddress());
        donation.setPickUpTime(dto.getPickUpTime());
        donation.setEstimatedQuantity(mapToQuantityEntity(dto.getEstimatedQuantity()));
        donation.setFoodItemsId(dto.getFoodItemsId() != null ? dto.getFoodItemsId() : new ArrayList<>());
        donation.setStatus(dto.getStatus());
        return donation;
    }

    private DonationDTO mapToDTO(Donation donation) {
        DonationDTO dto = new DonationDTO();
        dto.setId(donation.getId());
        dto.setDonationName(donation.getDonationName());
        dto.setDonarDetails(mapToUserDetailDTO(donation.getDonarDetails()));
        dto.setPickUpAddress(donation.getPickUpAddress());
        dto.setPickUpTime(donation.getPickUpTime());
        dto.setEstimatedQuantity(mapToQuantityDTO(donation.getEstimatedQuantity()));
        dto.setFoodItemsId(donation.getFoodItemsId());
        dto.setStatus(donation.getStatus());
        dto.setCreationDate(donation.getCreationDate());
        dto.setUpdateDate(donation.getUpdateDate());
        return dto;
    }

    private UserDetail mapToUserDetailEntity(UserDetailDTO dto) {
        if (dto == null) return null;
        return new UserDetail(dto.getUserId(), dto.getFirstName(), dto.getLastName(), dto.getEmail(), dto.getPhone());
    }

    private UserDetailDTO mapToUserDetailDTO(UserDetail userDetail) {
        if (userDetail == null) return null;
        return new UserDetailDTO(userDetail.getUserId(), userDetail.getFirstName(), userDetail.getLastName(), userDetail.getEmail(), userDetail.getPhone());
    }

    private Quantity mapToQuantityEntity(QuantityDTO dto) {
        if (dto == null) return null;
        return new Quantity(dto.getAmount(), dto.getUnit());
    }

    private QuantityDTO mapToQuantityDTO(Quantity quantity) {
        if (quantity == null) return null;
        return new QuantityDTO(quantity.getAmount(), quantity.getUnit());
    }
}
