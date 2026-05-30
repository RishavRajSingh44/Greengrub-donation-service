package com.greengrub.donationService.service;

import com.greengrub.donationService.client.FoodServiceClient;
import com.greengrub.donationService.dto.*;
import com.greengrub.donationService.entity.*;
import java.math.BigDecimal;
import com.greengrub.donationService.exception.DonationNotFoundException;
import com.greengrub.donationService.exception.KafkaPublishException;
import com.greengrub.donationService.kafka.DonationEventDTO;
import com.greengrub.donationService.kafka.DonationKafkaProducer;
import com.greengrub.donationService.repository.DonationRepository;
import com.greengrub.donationService.service.Impl.DonationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DonationServiceImplTest {

    @Mock DonationRepository donationRepository;
    @Mock DonationKafkaProducer kafkaProducer;
    @Mock FoodServiceClient foodServiceClient;

    @InjectMocks DonationServiceImpl service;

    private Donation sampleDonation;
    private DonationDTO sampleDTO;

    @BeforeEach
    void setUp() {
        sampleDonation = buildDonation("id-1", "Pizza Leftovers", DonationStatus.ACTIVE);
        sampleDTO = buildDonationDTO("id-1", "Pizza Leftovers", DonationStatus.ACTIVE);
    }

    // ── createDonation ────────────────────────────────────────────────────────

    @Test
    void createDonation_savesAndPublishesEvent() {
        when(donationRepository.saveAndFlush(any())).thenReturn(sampleDonation);

        DonationDTO result = service.createDonation(sampleDTO);

        assertThat(result.getDonationName()).isEqualTo("Pizza Leftovers");
        assertThat(result.getStatus()).isEqualTo(DonationStatus.ACTIVE);
        verify(kafkaProducer, times(1)).publish(any(DonationEventDTO.class));
    }

    @Test
    void createDonation_kafkaFailureDoesNotPropagateException() {
        when(donationRepository.saveAndFlush(any())).thenReturn(sampleDonation);
        doThrow(new KafkaPublishException("id-1", new RuntimeException("broker down")))
                .when(kafkaProducer).publish(any());

        // Should NOT throw — Kafka failure is swallowed
        assertThatCode(() -> service.createDonation(sampleDTO)).doesNotThrowAnyException();
    }

    @Test
    void createDonation_withNullDonorAndQuantity_savesSuccessfully() {
        DonationDTO dto = buildDonationDTO("id-2", "Bread", DonationStatus.ACTIVE);
        dto.setDonarDetails(null);
        dto.setEstimatedQuantity(null);
        Donation entity = buildDonation("id-2", "Bread", DonationStatus.ACTIVE);
        entity.setDonerDetails(null);
        entity.setEstimatedQuantity(null);
        when(donationRepository.saveAndFlush(any())).thenReturn(entity);

        DonationDTO result = service.createDonation(dto);

        assertThat(result.getDonarDetails()).isNull();
        assertThat(result.getEstimatedQuantity()).isNull();
    }

    @Test
    void createDonation_withNullFoodItems_defaultsToEmptyList() {
        sampleDTO.setFoodItemsId(null);
        when(donationRepository.saveAndFlush(any())).thenReturn(sampleDonation);

        ArgumentCaptor<Donation> cap = ArgumentCaptor.forClass(Donation.class);
        service.createDonation(sampleDTO);
        verify(donationRepository).saveAndFlush(cap.capture());
        assertThat(cap.getValue().getFoodItemsId()).isNotNull().isEmpty();
    }

    // ── getAllDonation ────────────────────────────────────────────────────────

    @Test
    void getAllDonation_returnsMappedList() {
        when(donationRepository.findAll()).thenReturn(List.of(sampleDonation));

        List<DonationDTO> result = service.getAllDonation();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("id-1");
    }

    @Test
    void getAllDonation_emptyRepository_returnsEmptyList() {
        when(donationRepository.findAll()).thenReturn(List.of());

        assertThat(service.getAllDonation()).isEmpty();
    }

    // ── getDonationDetail ─────────────────────────────────────────────────────

    @Test
    void getDonationDetail_returnsDonationWithFoodItems() {
        sampleDonation.setFoodItemsId(List.of("f1", "f2", "f3"));
        FoodDetailDTO food = FoodDetailDTO.builder().id("f1").foodName("Apple").build();
        when(donationRepository.findById("id-1")).thenReturn(Optional.of(sampleDonation));
        when(foodServiceClient.getFoodsPage(any(), eq(0), eq(10))).thenReturn(List.of(food));

        DonationDetailDTO detail = service.getDonationDetail("id-1", 0, 10);

        assertThat(detail.getDonation().getId()).isEqualTo("id-1");
        assertThat(detail.getTotalFoodItems()).isEqualTo(3);
        assertThat(detail.getFoodItems()).hasSize(1);
        assertThat(detail.getTotalPages()).isEqualTo(1);
        assertThat(detail.getCurrentPage()).isEqualTo(0);
        assertThat(detail.getPageSize()).isEqualTo(10);
    }

    @Test
    void getDonationDetail_notFound_throwsDonationNotFoundException() {
        when(donationRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getDonationDetail("missing", 0, 10))
                .isInstanceOf(DonationNotFoundException.class);
    }

    @Test
    void getDonationDetail_nullFoodIds_totalFoodItemsIsZero() {
        sampleDonation.setFoodItemsId(null);
        when(donationRepository.findById("id-1")).thenReturn(Optional.of(sampleDonation));
        when(foodServiceClient.getFoodsPage(any(), anyInt(), anyInt())).thenReturn(List.of());

        DonationDetailDTO detail = service.getDonationDetail("id-1", 0, 10);

        assertThat(detail.getTotalFoodItems()).isEqualTo(0);
        assertThat(detail.getTotalPages()).isEqualTo(0);
    }

    @Test
    void getDonationDetail_negativePage_normalisedToZero() {
        sampleDonation.setFoodItemsId(List.of("f1"));
        when(donationRepository.findById("id-1")).thenReturn(Optional.of(sampleDonation));
        when(foodServiceClient.getFoodsPage(any(), eq(0), eq(5))).thenReturn(List.of());

        DonationDetailDTO detail = service.getDonationDetail("id-1", -3, 5);

        assertThat(detail.getCurrentPage()).isEqualTo(0);
    }

    @Test
    void getDonationDetail_zeroSize_defaultsToTen() {
        sampleDonation.setFoodItemsId(List.of("f1"));
        when(donationRepository.findById("id-1")).thenReturn(Optional.of(sampleDonation));
        when(foodServiceClient.getFoodsPage(any(), eq(0), eq(10))).thenReturn(List.of());

        DonationDetailDTO detail = service.getDonationDetail("id-1", 0, 0);

        assertThat(detail.getPageSize()).isEqualTo(10);
    }

    @Test
    void getDonationDetail_multiplePages_calculatesCorrectTotalPages() {
        sampleDonation.setFoodItemsId(List.of("f1","f2","f3","f4","f5"));
        when(donationRepository.findById("id-1")).thenReturn(Optional.of(sampleDonation));
        when(foodServiceClient.getFoodsPage(any(), eq(0), eq(2))).thenReturn(List.of());

        DonationDetailDTO detail = service.getDonationDetail("id-1", 0, 2);

        assertThat(detail.getTotalPages()).isEqualTo(3);
    }

    // ── updateDonation ────────────────────────────────────────────────────────

    @Test
    void updateDonation_updatesFieldsAndPublishesEvent() {
        DonationDTO updateReq = buildDonationDTO("id-1", "Updated Name", DonationStatus.CLAIMED);
        when(donationRepository.findById("id-1")).thenReturn(Optional.of(sampleDonation));
        when(donationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DonationDTO result = service.updateDonation("id-1", updateReq);

        assertThat(result.getDonationName()).isEqualTo("Updated Name");
        assertThat(result.getStatus()).isEqualTo(DonationStatus.CLAIMED);
        verify(kafkaProducer, times(1)).publish(any(DonationEventDTO.class));
    }

    @Test
    void updateDonation_notFound_throwsDonationNotFoundException() {
        when(donationRepository.findById("bad-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateDonation("bad-id", sampleDTO))
                .isInstanceOf(DonationNotFoundException.class);
    }

    @Test
    void updateDonation_nullFoodItems_setsEmptyList() {
        sampleDTO.setFoodItemsId(null);
        when(donationRepository.findById("id-1")).thenReturn(Optional.of(sampleDonation));
        when(donationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DonationDTO result = service.updateDonation("id-1", sampleDTO);

        assertThat(result.getFoodItemsId()).isNotNull().isEmpty();
    }

    // ── deleteDonation ────────────────────────────────────────────────────────

    @Test
    void deleteDonation_deletesAndPublishesEvent() {
        when(donationRepository.findById("id-1")).thenReturn(Optional.of(sampleDonation));

        service.deleteDonation("id-1");

        verify(donationRepository, times(1)).delete(sampleDonation);
        verify(kafkaProducer, times(1)).publish(any(DonationEventDTO.class));
    }

    @Test
    void deleteDonation_notFound_throwsDonationNotFoundException() {
        when(donationRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteDonation("missing"))
                .isInstanceOf(DonationNotFoundException.class);
        verify(donationRepository, never()).delete(any());
    }

    // ── Kafka event mapping edge cases ────────────────────────────────────────

    @Test
    void createDonation_nullDonorInEvent_handledGracefully() {
        DonationDTO dto = buildDonationDTO("id-3", "Soup", DonationStatus.ACTIVE);
        dto.setDonarDetails(null);
        dto.setEstimatedQuantity(null);
        Donation entity = buildDonation("id-3", "Soup", DonationStatus.ACTIVE);
        entity.setDonerDetails(null);
        entity.setEstimatedQuantity(null);
        when(donationRepository.saveAndFlush(any())).thenReturn(entity);

        ArgumentCaptor<DonationEventDTO> cap = ArgumentCaptor.forClass(DonationEventDTO.class);
        service.createDonation(dto);
        verify(kafkaProducer).publish(cap.capture());

        assertThat(cap.getValue().donorName()).isEqualTo("");
        assertThat(cap.getValue().donorEmail()).isEqualTo("");
        assertThat(cap.getValue().customer()).isNull();
    }

    @Test
    void createDonation_withCreationDate_usesExistingDate() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 1, 12, 0);
        sampleDonation.setCreationDate(createdAt);
        when(donationRepository.saveAndFlush(any())).thenReturn(sampleDonation);

        ArgumentCaptor<DonationEventDTO> cap = ArgumentCaptor.forClass(DonationEventDTO.class);
        service.createDonation(sampleDTO);
        verify(kafkaProducer).publish(cap.capture());

        assertThat(cap.getValue().createdAt()).isEqualTo(createdAt);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Donation buildDonation(String id, String name, DonationStatus status) {
        Donation d = new Donation();
        d.setId(id);
        d.setDonationName(name);
        d.setDonerDetails(new UserDetail("u1", "Jane", "Doe", "jane@example.com", "9876543210"));
        d.setPickUpAddress("123 Main St");
        d.setPickUpTime(LocalDateTime.of(2026, 6, 1, 10, 0));
        d.setEstimatedQuantity(new Quantity(BigDecimal.valueOf(5.0), Unit.KG));
        d.setFoodItemsId(new ArrayList<>(List.of("f1")));
        d.setStatus(status);
        d.setCreationDate(LocalDateTime.now());
        d.setUpdateDate(LocalDateTime.now());
        return d;
    }

    private DonationDTO buildDonationDTO(String id, String name, DonationStatus status) {
        DonationDTO dto = new DonationDTO();
        dto.setId(id);
        dto.setDonationName(name);
        dto.setDonarDetails(new UserDetailDTO("u1", "Jane", "Doe", "jane@example.com", "9876543210"));
        dto.setPickUpAddress("123 Main St");
        dto.setPickUpTime(LocalDateTime.of(2026, 6, 1, 10, 0));
        dto.setEstimatedQuantity(new QuantityDTO(5.0, Unit.KG));
        dto.setFoodItemsId(new ArrayList<>(List.of("f1")));
        dto.setStatus(status);
        return dto;
    }
}
