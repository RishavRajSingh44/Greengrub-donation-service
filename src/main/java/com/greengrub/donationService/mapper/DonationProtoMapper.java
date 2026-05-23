package com.greengrub.donationService.mapper;

import com.greengrub.donationService.dto.DonationDTO;
import com.greengrub.donationService.dto.QuantityDTO;
import com.greengrub.donationService.dto.UserDetailDTO;
import com.greengrub.donationService.entity.DonationStatus;
import com.greengrub.donationService.entity.Unit;
import com.greengrub.proto.donation.*;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;

@Component
public class DonationProtoMapper {

    // ---------------- Proto → DTO ----------------

    public DonationDTO fromProto(CreateDonationRequest r) {
        DonationDTO dto = new DonationDTO();
        dto.setDonationName(r.getDonationName());
        dto.setDonarDetails(fromUserDetailProto(r.getDonerDetails()));
        dto.setPickUpAddress(r.getPickUpAddress());
        dto.setPickUpTime(parseDateTime(r.getPickUpTime()));
        dto.setEstimatedQuantity(fromQuantityProto(r.getEstimatedQuantity()));
        dto.setFoodItemsId(new ArrayList<>(r.getFoodItemsIdList()));
        dto.setStatus(fromProtoStatus(r.getStatus()));
        return dto;
    }

    public DonationDTO fromProto(UpdateDonationRequest r) {
        DonationDTO dto = new DonationDTO();
        dto.setDonationName(r.getDonationName());
        dto.setDonarDetails(fromUserDetailProto(r.getDonerDetails()));
        dto.setPickUpAddress(r.getPickUpAddress());
        dto.setPickUpTime(parseDateTime(r.getPickUpTime()));
        dto.setEstimatedQuantity(fromQuantityProto(r.getEstimatedQuantity()));
        dto.setFoodItemsId(new ArrayList<>(r.getFoodItemsIdList()));
        dto.setStatus(fromProtoStatus(r.getStatus()));
        return dto;
    }

    public UserDetailDTO fromUserDetailProto(UserDetail ud) {
        if (ud == null) return null;
        return new UserDetailDTO(ud.getUserId(), ud.getFirstName(), ud.getLastName(), ud.getEmail(), ud.getPhone());
    }

    public QuantityDTO fromQuantityProto(Quantity q) {
        if (q == null) return null;
        return new QuantityDTO(q.getAmount(), fromProtoUnit(q.getUnit()));
    }

    public DonationStatus fromProtoStatus(com.greengrub.proto.donation.DonationStatus s) {
        return switch (s) {
            case CLAIMED -> DonationStatus.CLAIMED;
            case CANCELLED -> DonationStatus.CANCELLED;
            default -> DonationStatus.ACTIVE;
        };
    }

    public Unit fromProtoUnit(com.greengrub.proto.donation.Unit u) {
        return switch (u) {
            case SERVINGS -> Unit.SERVINGS;
            default -> Unit.KG;
        };
    }

    // ---------------- DTO → Proto ----------------

    public DonationResponse toProto(DonationDTO dto) {
        DonationResponse.Builder builder = DonationResponse.newBuilder()
                .setId(nullSafe(dto.getId()))
                .setDonationName(nullSafe(dto.getDonationName()))
                .setPickUpAddress(nullSafe(dto.getPickUpAddress()))
                .setPickUpTime(dto.getPickUpTime() != null ? dto.getPickUpTime().toString() : "")
                .setStatus(toProtoStatus(dto.getStatus()))
                .setCreationDate(dto.getCreationDate() != null ? dto.getCreationDate().toString() : "")
                .setUpdateDate(dto.getUpdateDate() != null ? dto.getUpdateDate().toString() : "");

        if (dto.getDonarDetails() != null) {
            builder.setDonerDetails(toUserDetailProto(dto.getDonarDetails()));
        }
        if (dto.getEstimatedQuantity() != null) {
            builder.setEstimatedQuantity(toQuantityProto(dto.getEstimatedQuantity()));
        }
        if (dto.getFoodItemsId() != null) {
            builder.addAllFoodItemsId(dto.getFoodItemsId());
        }
        return builder.build();
    }

    public UserDetail toUserDetailProto(UserDetailDTO dto) {
        return UserDetail.newBuilder()
                .setUserId(nullSafe(dto.getUserId()))
                .setFirstName(nullSafe(dto.getFirstName()))
                .setLastName(nullSafe(dto.getLastName()))
                .setEmail(nullSafe(dto.getEmail()))
                .setPhone(nullSafe(dto.getPhone()))
                .build();
    }

    public Quantity toQuantityProto(QuantityDTO dto) {
        return Quantity.newBuilder()
                .setAmount(dto.getAmount() != null ? dto.getAmount() : 0.0)
                .setUnit(toProtoUnit(dto.getUnit()))
                .build();
    }

    public com.greengrub.proto.donation.DonationStatus toProtoStatus(DonationStatus s) {
        if (s == null) return com.greengrub.proto.donation.DonationStatus.ACTIVE;
        return switch (s) {
            case CLAIMED -> com.greengrub.proto.donation.DonationStatus.CLAIMED;
            case CANCELLED -> com.greengrub.proto.donation.DonationStatus.CANCELLED;
            default -> com.greengrub.proto.donation.DonationStatus.ACTIVE;
        };
    }

    public com.greengrub.proto.donation.Unit toProtoUnit(Unit u) {
        if (u == null) return com.greengrub.proto.donation.Unit.UNIT_UNSPECIFIED;
        return switch (u) {
            case SERVINGS -> com.greengrub.proto.donation.Unit.SERVINGS;
            default -> com.greengrub.proto.donation.Unit.KG;
        };
    }

    // ---------------- Utilities ----------------

    public LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDateTime.parse(value);
        } catch (Exception e) {
            return null;
        }
    }

    private String nullSafe(String s) {
        return s != null ? s : "";
    }
}
