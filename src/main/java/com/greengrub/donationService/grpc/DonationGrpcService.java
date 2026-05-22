package com.greengrub.donationService.grpc;

import com.greengrub.donationService.dto.DonationDTO;
import com.greengrub.donationService.dto.QuantityDTO;
import com.greengrub.donationService.dto.UserDetailDTO;
import com.greengrub.donationService.entity.DonationStatus;
import com.greengrub.donationService.entity.Unit;
import com.greengrub.donationService.exception.DonationNotFoundException;
import com.greengrub.donationService.service.DonationService;
import com.greengrub.proto.donation.*;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@GrpcService
public class DonationGrpcService extends DonationServiceGrpc.DonationServiceImplBase {

    private final DonationService donationService;

    public DonationGrpcService(DonationService donationService) {
        this.donationService = donationService;
    }

    @Override
    public void createDonation(CreateDonationRequest request, StreamObserver<DonationResponse> responseObserver) {
        DonationDTO dto = fromProto(request);
        DonationDTO created = donationService.createDonation(dto);
        responseObserver.onNext(toProto(created));
        responseObserver.onCompleted();
    }

    @Override
    public void getAllDonations(DonationListRequest request, StreamObserver<DonationListResponse> responseObserver) {
        List<DonationDTO> all = donationService.getAllDonation();
        DonationListResponse response = DonationListResponse.newBuilder()
                .addAllDonations(all.stream().map(this::toProto).toList())
                .setTotalCount(all.size())
                .setPage(request.getPage())
                .setPageSize(request.getPageSize())
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getDonationById(DonationByIdRequest request, StreamObserver<DonationResponse> responseObserver) {
        DonationDTO dto = donationService.getDonationById(request.getId());
        responseObserver.onNext(toProto(dto));
        responseObserver.onCompleted();
    }

    @Override
    public void getDonationsByUserId(DonationByUserIdRequest request, StreamObserver<DonationListResponse> responseObserver) {
        List<DonationDTO> all = donationService.getAllDonation().stream()
                .filter(d -> d.getDonarDetails() != null
                        && request.getUserId().equals(d.getDonarDetails().getUserId()))
                .toList();
        DonationListResponse response = DonationListResponse.newBuilder()
                .addAllDonations(all.stream().map(this::toProto).toList())
                .setTotalCount(all.size())
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void updateDonation(UpdateDonationRequest request, StreamObserver<DonationResponse> responseObserver) {
        DonationDTO dto = fromProto(request);
        DonationDTO updated = donationService.updateDonation(request.getId(), dto);
        responseObserver.onNext(toProto(updated));
        responseObserver.onCompleted();
    }

    @Override
    public void deleteDonation(DonationByIdRequest request, StreamObserver<DeleteDonationResponse> responseObserver) {
        donationService.deleteDonation(request.getId());
        responseObserver.onNext(DeleteDonationResponse.newBuilder()
                .setMessage("Donation " + request.getId() + " deleted")
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void getFoodItemsByDonationId(FoodListRequest request, StreamObserver<FoodListResponse> responseObserver) {
        FoodListResponse.Builder builder = FoodListResponse.newBuilder();
        for (String foodId : request.getFoodItemIdList()) {
            builder.addFoods(Food.newBuilder().setFoodId(foodId).build());
        }
        builder.setTotalCount(request.getFoodItemIdCount());
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void getUsersByDonationId(DonationByIdRequest request, StreamObserver<ListUsersResponse> responseObserver) {
        DonationDTO dto = donationService.getDonationById(request.getId());
        ListUsersResponse.Builder builder = ListUsersResponse.newBuilder();
        if (dto.getDonarDetails() != null) {
            builder.addUsers(toUserDetailProto(dto.getDonarDetails()));
            builder.setTotalCount(1);
        }
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    // ---------------- Proto → DTO ----------------

    private DonationDTO fromProto(CreateDonationRequest r) {
        DonationDTO dto = new DonationDTO();
        dto.setDonationName(r.getDonationName());
        dto.setDonarDetails(fromUserDetailProto(r.getDonerDetails()));
        dto.setPickUpAddress(r.getPickUpAddress());
        dto.setPickUpTime(parseDateTime(r.getPickUpTime()));
        dto.setEstimatedQuantity(fromQuantityProto(r.getEstimatedQuantity()));
        dto.setFoodItemsId(new java.util.ArrayList<>(r.getFoodItemsIdList()));
        dto.setStatus(fromProtoStatus(r.getStatus()));
        return dto;
    }

    private DonationDTO fromProto(UpdateDonationRequest r) {
        DonationDTO dto = new DonationDTO();
        dto.setDonationName(r.getDonationName());
        dto.setDonarDetails(fromUserDetailProto(r.getDonerDetails()));
        dto.setPickUpAddress(r.getPickUpAddress());
        dto.setPickUpTime(parseDateTime(r.getPickUpTime()));
        dto.setEstimatedQuantity(fromQuantityProto(r.getEstimatedQuantity()));
        dto.setFoodItemsId(new java.util.ArrayList<>(r.getFoodItemsIdList()));
        dto.setStatus(fromProtoStatus(r.getStatus()));
        return dto;
    }

    private UserDetailDTO fromUserDetailProto(UserDetail ud) {
        if (ud == null) return null;
        return new UserDetailDTO(ud.getUserId(), ud.getFirstName(), ud.getLastName(), ud.getEmail(), ud.getPhone());
    }

    private QuantityDTO fromQuantityProto(Quantity q) {
        if (q == null) return null;
        return new QuantityDTO(q.getAmount(), fromProtoUnit(q.getUnit()));
    }

    private DonationStatus fromProtoStatus(com.greengrub.proto.donation.DonationStatus s) {
        return switch (s) {
            case CLAIMED -> DonationStatus.CLAIMED;
            case CANCELLED -> DonationStatus.CANCELLED;
            default -> DonationStatus.ACTIVE;
        };
    }

    private Unit fromProtoUnit(com.greengrub.proto.donation.Unit u) {
        return switch (u) {
            case SERVINGS -> Unit.SERVINGS;
            default -> Unit.KG;
        };
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDateTime.parse(value);
        } catch (Exception e) {
            return null;
        }
    }

    // ---------------- DTO → Proto ----------------

    private DonationResponse toProto(DonationDTO dto) {
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

    private UserDetail toUserDetailProto(UserDetailDTO dto) {
        return UserDetail.newBuilder()
                .setUserId(nullSafe(dto.getUserId()))
                .setFirstName(nullSafe(dto.getFirstName()))
                .setLastName(nullSafe(dto.getLastName()))
                .setEmail(nullSafe(dto.getEmail()))
                .setPhone(nullSafe(dto.getPhone()))
                .build();
    }

    private Quantity toQuantityProto(QuantityDTO dto) {
        return Quantity.newBuilder()
                .setAmount(dto.getAmount() != null ? dto.getAmount() : 0.0)
                .setUnit(toProtoUnit(dto.getUnit()))
                .build();
    }

    private com.greengrub.proto.donation.DonationStatus toProtoStatus(DonationStatus s) {
        if (s == null) return com.greengrub.proto.donation.DonationStatus.ACTIVE;
        return switch (s) {
            case CLAIMED -> com.greengrub.proto.donation.DonationStatus.CLAIMED;
            case CANCELLED -> com.greengrub.proto.donation.DonationStatus.CANCELLED;
            default -> com.greengrub.proto.donation.DonationStatus.ACTIVE;
        };
    }

    private com.greengrub.proto.donation.Unit toProtoUnit(Unit u) {
        if (u == null) return com.greengrub.proto.donation.Unit.UNIT_UNSPECIFIED;
        return switch (u) {
            case SERVINGS -> com.greengrub.proto.donation.Unit.SERVINGS;
            default -> com.greengrub.proto.donation.Unit.KG;
        };
    }

    private String nullSafe(String s) {
        return s != null ? s : "";
    }
}
