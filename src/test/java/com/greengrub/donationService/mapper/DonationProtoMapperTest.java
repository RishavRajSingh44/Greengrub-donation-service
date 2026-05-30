package com.greengrub.donationService.mapper;

import com.greengrub.donationService.dto.DonationDTO;
import com.greengrub.donationService.dto.QuantityDTO;
import com.greengrub.donationService.dto.UserDetailDTO;
import com.greengrub.donationService.entity.DonationStatus;
import com.greengrub.donationService.entity.Unit;
import com.greengrub.proto.donation.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class DonationProtoMapperTest {

    DonationProtoMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new DonationProtoMapper();
    }

    // ── fromProto(CreateDonationRequest) ──────────────────────────────────────

    @Test
    void fromCreateRequest_mapsAllFields() {
        CreateDonationRequest req = CreateDonationRequest.newBuilder()
                .setDonationName("Soup")
                .setDonerDetails(UserDetail.newBuilder()
                        .setUserId("u1").setFirstName("Jane").setLastName("Doe")
                        .setEmail("jane@example.com").setPhone("9876543210").build())
                .setPickUpAddress("123 Main")
                .setPickUpTime("2026-06-01T10:00:00")
                .setEstimatedQuantity(Quantity.newBuilder().setAmount(3.0)
                        .setUnit(com.greengrub.proto.donation.Unit.KG).build())
                .addFoodItemsId("f1")
                .setStatus(com.greengrub.proto.donation.DonationStatus.ACTIVE)
                .build();

        DonationDTO dto = mapper.fromProto(req);

        assertThat(dto.getDonationName()).isEqualTo("Soup");
        assertThat(dto.getDonarDetails().getEmail()).isEqualTo("jane@example.com");
        assertThat(dto.getPickUpAddress()).isEqualTo("123 Main");
        assertThat(dto.getPickUpTime()).isEqualTo(LocalDateTime.of(2026, 6, 1, 10, 0));
        assertThat(dto.getEstimatedQuantity().getAmount()).isEqualTo(3.0);
        assertThat(dto.getEstimatedQuantity().getUnit()).isEqualTo(Unit.KG);
        assertThat(dto.getFoodItemsId()).containsExactly("f1");
        assertThat(dto.getStatus()).isEqualTo(DonationStatus.ACTIVE);
    }

    @Test
    void fromCreateRequest_claimedStatus_mapsToClaimed() {
        CreateDonationRequest req = CreateDonationRequest.newBuilder()
                .setStatus(com.greengrub.proto.donation.DonationStatus.CLAIMED)
                .build();

        assertThat(mapper.fromProto(req).getStatus()).isEqualTo(DonationStatus.CLAIMED);
    }

    @Test
    void fromCreateRequest_cancelledStatus_mapsToCancelled() {
        CreateDonationRequest req = CreateDonationRequest.newBuilder()
                .setStatus(com.greengrub.proto.donation.DonationStatus.CANCELLED)
                .build();

        assertThat(mapper.fromProto(req).getStatus()).isEqualTo(DonationStatus.CANCELLED);
    }

    @Test
    void fromCreateRequest_defaultStatus_mapsToActive() {
        // Default proto enum value (no status set) should map to ACTIVE
        CreateDonationRequest req = CreateDonationRequest.newBuilder().build();

        assertThat(mapper.fromProto(req).getStatus()).isEqualTo(DonationStatus.ACTIVE);
    }

    // ── fromProto(UpdateDonationRequest) ──────────────────────────────────────

    @Test
    void fromUpdateRequest_mapsAllFields() {
        UpdateDonationRequest req = UpdateDonationRequest.newBuilder()
                .setId("id-1")
                .setDonationName("Updated")
                .setDonerDetails(UserDetail.newBuilder()
                        .setUserId("u1").setFirstName("A").setLastName("B")
                        .setEmail("a@b.com").setPhone("111").build())
                .setPickUpAddress("456 Side St")
                .setPickUpTime("2026-07-01T08:00:00")
                .setEstimatedQuantity(Quantity.newBuilder().setAmount(1.5)
                        .setUnit(com.greengrub.proto.donation.Unit.SERVINGS).build())
                .setStatus(com.greengrub.proto.donation.DonationStatus.CANCELLED)
                .build();

        DonationDTO dto = mapper.fromProto(req);

        assertThat(dto.getDonationName()).isEqualTo("Updated");
        assertThat(dto.getDonarDetails().getFirstName()).isEqualTo("A");
        assertThat(dto.getEstimatedQuantity().getUnit()).isEqualTo(Unit.SERVINGS);
        assertThat(dto.getStatus()).isEqualTo(DonationStatus.CANCELLED);
    }

    // ── fromQuantityProto ─────────────────────────────────────────────────────

    @Test
    void fromQuantityProto_null_returnsNull() {
        assertThat(mapper.fromQuantityProto(null)).isNull();
    }

    @Test
    void fromQuantityProto_servingsUnit_mapsCorrectly() {
        Quantity q = Quantity.newBuilder().setAmount(2.0)
                .setUnit(com.greengrub.proto.donation.Unit.SERVINGS).build();
        QuantityDTO dto = mapper.fromQuantityProto(q);
        assertThat(dto.getUnit()).isEqualTo(Unit.SERVINGS);
    }

    @Test
    void fromQuantityProto_unspecifiedUnit_defaultsToKg() {
        Quantity q = Quantity.newBuilder().setAmount(2.0)
                .setUnit(com.greengrub.proto.donation.Unit.UNIT_UNSPECIFIED).build();
        QuantityDTO dto = mapper.fromQuantityProto(q);
        assertThat(dto.getUnit()).isEqualTo(Unit.KG);
    }

    // ── fromUserDetailProto ───────────────────────────────────────────────────

    @Test
    void fromUserDetailProto_null_returnsNull() {
        assertThat(mapper.fromUserDetailProto(null)).isNull();
    }

    @Test
    void fromUserDetailProto_mapsAllFields() {
        UserDetail ud = UserDetail.newBuilder()
                .setUserId("u1").setFirstName("Jane").setLastName("Doe")
                .setEmail("jane@x.com").setPhone("555").build();
        UserDetailDTO dto = mapper.fromUserDetailProto(ud);
        assertThat(dto.getUserId()).isEqualTo("u1");
        assertThat(dto.getEmail()).isEqualTo("jane@x.com");
    }

    // ── toProto(DonationDTO) ──────────────────────────────────────────────────

    @Test
    void toProto_mapsAllFields() {
        DonationDTO dto = buildDonationDTO("id-1", DonationStatus.ACTIVE);

        DonationResponse resp = mapper.toProto(dto);

        assertThat(resp.getId()).isEqualTo("id-1");
        assertThat(resp.getDonationName()).isEqualTo("Birthday Party");
        assertThat(resp.getPickUpAddress()).isEqualTo("99 Park Ave");
        assertThat(resp.getStatus()).isEqualTo(com.greengrub.proto.donation.DonationStatus.ACTIVE);
        assertThat(resp.getDonerDetails().getEmail()).isEqualTo("donor@example.com");
        assertThat(resp.getEstimatedQuantity().getAmount()).isEqualTo(10.0);
        assertThat(resp.getFoodItemsIdList()).containsExactly("f1", "f2");
    }

    @Test
    void toProto_nullId_setsEmptyString() {
        DonationDTO dto = buildDonationDTO(null, DonationStatus.ACTIVE);
        assertThat(mapper.toProto(dto).getId()).isEqualTo("");
    }

    @Test
    void toProto_nullPickUpTime_setsEmptyString() {
        DonationDTO dto = buildDonationDTO("id-1", DonationStatus.ACTIVE);
        dto.setPickUpTime(null);
        assertThat(mapper.toProto(dto).getPickUpTime()).isEqualTo("");
    }

    @Test
    void toProto_nullCreationDate_setsEmptyString() {
        DonationDTO dto = buildDonationDTO("id-1", DonationStatus.ACTIVE);
        dto.setCreationDate(null);
        assertThat(mapper.toProto(dto).getCreationDate()).isEqualTo("");
    }

    @Test
    void toProto_nullUpdateDate_setsEmptyString() {
        DonationDTO dto = buildDonationDTO("id-1", DonationStatus.ACTIVE);
        dto.setUpdateDate(null);
        assertThat(mapper.toProto(dto).getUpdateDate()).isEqualTo("");
    }

    @Test
    void toProto_nullDonorDetails_noDonerDetailsSet() {
        DonationDTO dto = buildDonationDTO("id-1", DonationStatus.ACTIVE);
        dto.setDonarDetails(null);
        DonationResponse resp = mapper.toProto(dto);
        assertThat(resp.hasDonerDetails()).isFalse();
    }

    @Test
    void toProto_nullQuantity_noQuantitySet() {
        DonationDTO dto = buildDonationDTO("id-1", DonationStatus.ACTIVE);
        dto.setEstimatedQuantity(null);
        DonationResponse resp = mapper.toProto(dto);
        assertThat(resp.hasEstimatedQuantity()).isFalse();
    }

    @Test
    void toProto_nullFoodItems_noFoodItemsSet() {
        DonationDTO dto = buildDonationDTO("id-1", DonationStatus.ACTIVE);
        dto.setFoodItemsId(null);
        assertThat(mapper.toProto(dto).getFoodItemsIdList()).isEmpty();
    }

    @Test
    void toProto_claimedStatus_mapsCorrectly() {
        DonationDTO dto = buildDonationDTO("id-1", DonationStatus.CLAIMED);
        assertThat(mapper.toProto(dto).getStatus())
                .isEqualTo(com.greengrub.proto.donation.DonationStatus.CLAIMED);
    }

    @Test
    void toProto_cancelledStatus_mapsCorrectly() {
        DonationDTO dto = buildDonationDTO("id-1", DonationStatus.CANCELLED);
        assertThat(mapper.toProto(dto).getStatus())
                .isEqualTo(com.greengrub.proto.donation.DonationStatus.CANCELLED);
    }

    @Test
    void toProto_nullStatus_defaultsToActive() {
        DonationDTO dto = buildDonationDTO("id-1", null);
        assertThat(mapper.toProto(dto).getStatus())
                .isEqualTo(com.greengrub.proto.donation.DonationStatus.ACTIVE);
    }

    // ── toQuantityProto ───────────────────────────────────────────────────────

    @Test
    void toQuantityProto_nullAmount_setsZero() {
        QuantityDTO qDto = new QuantityDTO(null, Unit.KG);
        Quantity q = mapper.toQuantityProto(qDto);
        assertThat(q.getAmount()).isEqualTo(0.0);
    }

    @Test
    void toQuantityProto_servingsUnit_mapsCorrectly() {
        QuantityDTO qDto = new QuantityDTO(3.0, Unit.SERVINGS);
        assertThat(mapper.toQuantityProto(qDto).getUnit())
                .isEqualTo(com.greengrub.proto.donation.Unit.SERVINGS);
    }

    @Test
    void toQuantityProto_kgUnit_mapsCorrectly() {
        QuantityDTO qDto = new QuantityDTO(3.0, Unit.KG);
        assertThat(mapper.toQuantityProto(qDto).getUnit())
                .isEqualTo(com.greengrub.proto.donation.Unit.KG);
    }

    @Test
    void toQuantityProto_nullUnit_mapsToUnspecified() {
        QuantityDTO qDto = new QuantityDTO(3.0, null);
        assertThat(mapper.toQuantityProto(qDto).getUnit())
                .isEqualTo(com.greengrub.proto.donation.Unit.UNIT_UNSPECIFIED);
    }

    // ── toUserDetailProto ─────────────────────────────────────────────────────

    @Test
    void toUserDetailProto_mapsAllFields() {
        UserDetailDTO dto = new UserDetailDTO("u1", "A", "B", "a@b.com", "123");
        UserDetail ud = mapper.toUserDetailProto(dto);
        assertThat(ud.getUserId()).isEqualTo("u1");
        assertThat(ud.getEmail()).isEqualTo("a@b.com");
    }

    @Test
    void toUserDetailProto_nullFields_mapsToEmptyStrings() {
        UserDetailDTO dto = new UserDetailDTO(null, null, null, null, null);
        UserDetail ud = mapper.toUserDetailProto(dto);
        assertThat(ud.getUserId()).isEqualTo("");
        assertThat(ud.getEmail()).isEqualTo("");
    }

    // ── parseDateTime ─────────────────────────────────────────────────────────

    @Test
    void parseDateTime_validString_parsesCorrectly() {
        LocalDateTime dt = mapper.parseDateTime("2026-06-01T10:00:00");
        assertThat(dt).isEqualTo(LocalDateTime.of(2026, 6, 1, 10, 0));
    }

    @Test
    void parseDateTime_null_returnsNull() {
        assertThat(mapper.parseDateTime(null)).isNull();
    }

    @Test
    void parseDateTime_blank_returnsNull() {
        assertThat(mapper.parseDateTime("  ")).isNull();
    }

    @Test
    void parseDateTime_invalidFormat_returnsNull() {
        assertThat(mapper.parseDateTime("not-a-date")).isNull();
    }

    // ── helper ────────────────────────────────────────────────────────────────

    private DonationDTO buildDonationDTO(String id, DonationStatus status) {
        DonationDTO dto = new DonationDTO();
        dto.setId(id);
        dto.setDonationName("Birthday Party");
        dto.setDonarDetails(new UserDetailDTO("u1", "Jane", "Doe", "donor@example.com", "9876543210"));
        dto.setPickUpAddress("99 Park Ave");
        dto.setPickUpTime(LocalDateTime.of(2026, 7, 1, 12, 0));
        dto.setEstimatedQuantity(new QuantityDTO(10.0, Unit.KG));
        dto.setFoodItemsId(List.of("f1", "f2"));
        dto.setStatus(status);
        dto.setCreationDate(LocalDateTime.of(2026, 5, 1, 9, 0));
        dto.setUpdateDate(LocalDateTime.of(2026, 5, 2, 9, 0));
        return dto;
    }
}
