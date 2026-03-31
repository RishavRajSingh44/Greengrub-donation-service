package com.greengrub.donationService.service.Impl;

import com.greengrub.donationService.dto.DonationDTO;
import com.greengrub.donationService.dto.UserDetailDTO;
import com.greengrub.donationService.entity.Donation;
import com.greengrub.donationService.entity.UserDetail;
import com.greengrub.donationService.exception.DonationNotFoundException;
import com.greengrub.donationService.repository.DonationRepository;
import com.greengrub.donationService.service.DonationService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
    public DonationDTO getDonationById(Long id) {
        Donation donation = donationRepository.findById(id)
            .orElseThrow(() -> new DonationNotFoundException(id));
        return mapToDTO(donation);
    }

    @Override
    public DonationDTO updateDonation(Long id, DonationDTO request) {
        Donation donation = donationRepository.findById(id)
            .orElseThrow(() -> new DonationNotFoundException(id));

        donation.setDonationName(request.getDonationName());
        donation.setUserDetail(mapToUserDetailEntity(request.getUserDetail()));
        donation.setStatus(request.getStatus());

        Donation updatedDonation = donationRepository.save(donation);
        return mapToDTO(updatedDonation);
    }

    @Override
    public void deleteDonation(Long id) {
        Donation donation = donationRepository.findById(id)
            .orElseThrow(() -> new DonationNotFoundException(id));
        donationRepository.delete(donation);
    }

    // ---------------- MAPPING METHODS ----------------

    private Donation mapToEntity(DonationDTO dto) {
        Donation donation = new Donation();
        donation.setDonationName(dto.getDonationName());
        donation.setUserDetail(mapToUserDetailEntity(dto.getUserDetail()));
        donation.setStatus(dto.getStatus());
        return donation;
    }

    private DonationDTO mapToDTO(Donation donation) {
        DonationDTO dto = new DonationDTO();
        dto.setId(donation.getId());
        dto.setDonationName(donation.getDonationName());
        dto.setUserDetail(mapToUserDetailDTO(donation.getUserDetail()));
        dto.setCreationDate(donation.getCreationDate());
        dto.setUpdateDate(donation.getUpdateDate());
        dto.setStatus(donation.getStatus());
        return dto;
    }

    private UserDetail mapToUserDetailEntity(UserDetailDTO dto) {
        if (dto == null) return null;
        return new UserDetail(dto.getFirstName(), dto.getLastName(), dto.getPhoneNumber(), dto.getEmailAddress());
    }

    private UserDetailDTO mapToUserDetailDTO(UserDetail userDetail) {
        if (userDetail == null) return null;
        return new UserDetailDTO(userDetail.getFirstName(), userDetail.getLastName(), userDetail.getPhoneNumber(), userDetail.getEmailAddress());
    }
}
