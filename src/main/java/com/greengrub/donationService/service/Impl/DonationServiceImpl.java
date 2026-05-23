package com.greengrub.donationService.service.Impl;

import com.greengrub.donationService.dto.DonationDTO;
import com.greengrub.donationService.dto.QuantityDTO;
import com.greengrub.donationService.dto.UserDetailDTO;
import com.greengrub.donationService.entity.Donation;
import com.greengrub.donationService.entity.DonationStatus;
import com.greengrub.donationService.entity.Quantity;
import com.greengrub.donationService.entity.UserDetail;
import com.greengrub.donationService.exception.DonationNotFoundException;
import com.greengrub.donationService.exception.KafkaPublishException;
import com.greengrub.donationService.kafka.DonationEventDTO;
import com.greengrub.donationService.kafka.DonationKafkaProducer;
import com.greengrub.donationService.repository.DonationRepository;
import com.greengrub.donationService.service.DonationService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DonationServiceImpl implements DonationService {

    private final DonationRepository donationRepository;
    private final DonationKafkaProducer kafkaProducer;

    public DonationServiceImpl(DonationRepository donationRepository, DonationKafkaProducer kafkaProducer) {
        this.donationRepository = donationRepository;
        this.kafkaProducer = kafkaProducer;
    }

    @Override
    @Transactional
    public DonationDTO createDonation(DonationDTO request) {
        Donation donation = mapToEntity(request);
        Donation savedDonation = donationRepository.saveAndFlush(donation);
        DonationDTO saved = mapToDTO(savedDonation);
        publishEvent(saved);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DonationDTO> getAllDonation() {
        return donationRepository.findAll()
            .stream()
            .map(this::mapToDTO)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public DonationDTO getDonationById(String id) {
        Donation donation = donationRepository.findById(id)
            .orElseThrow(() -> new DonationNotFoundException(id));
        return mapToDTO(donation);
    }

    @Override
    @Transactional
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
        DonationDTO updated = mapToDTO(updatedDonation);
        publishEvent(updated);
        return updated;
    }

    @Override
    @Transactional
    public void deleteDonation(String id) {
        Donation donation = donationRepository.findById(id)
            .orElseThrow(() -> new DonationNotFoundException(id));
        DonationDTO dto = mapToDTO(donation);
        publishEvent(dto);
        donationRepository.delete(donation);
    }

    // Kafka publish is best-effort — DB write is already committed before this runs.
    // A Kafka failure is logged but never propagated to the caller.
    private void publishEvent(DonationDTO dto) {
        try {
            kafkaProducer.publish(toKafkaEvent(dto));
        } catch (KafkaPublishException e) {
            log.warn("Kafka event not published for donation [{}] — donation saved: {}",
                     dto.getId(), e.getMessage());
        }
    }

    // ---------------- KAFKA MAPPING ----------------

    private DonationEventDTO toKafkaEvent(DonationDTO dto) {
        UserDetailDTO donor = dto.getDonarDetails();
        String donorName = donor != null
                ? (donor.getFirstName() + " " + donor.getLastName()).trim()
                : "";
        String donorEmail = donor != null ? donor.getEmail() : "";

        BigDecimal totalAmount = dto.getEstimatedQuantity() != null && dto.getEstimatedQuantity().getAmount() != null
                ? BigDecimal.valueOf(dto.getEstimatedQuantity().getAmount())
                : BigDecimal.ZERO;

        String unit = dto.getEstimatedQuantity() != null && dto.getEstimatedQuantity().getUnit() != null
                ? dto.getEstimatedQuantity().getUnit().name()
                : "";

        int qty = dto.getEstimatedQuantity() != null && dto.getEstimatedQuantity().getAmount() != null
                ? dto.getEstimatedQuantity().getAmount().intValue()
                : 0;

        DonationEventDTO.CustomerDTO customer = donor != null
                ? new DonationEventDTO.CustomerDTO(
                        donor.getUserId(),
                        donor.getFirstName(),
                        donor.getLastName(),
                        donor.getEmail(),
                        donor.getPhone())
                : null;

        List<DonationEventDTO.DonationItemDTO> items = List.of(
                new DonationEventDTO.DonationItemDTO(dto.getDonationName(), qty, unit, null)
        );

        return new DonationEventDTO(
                dto.getId(),
                donorName,
                donorEmail,
                totalAmount,
                dto.getCreationDate() != null ? dto.getCreationDate() : LocalDateTime.now(),
                "GreenGrub",
                customer,
                items
        );
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
