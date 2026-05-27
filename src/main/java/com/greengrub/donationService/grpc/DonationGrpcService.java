package com.greengrub.donationService.grpc;

import com.greengrub.donationService.client.FoodServiceClient;
import com.greengrub.donationService.dto.DonationDTO;
import com.greengrub.donationService.dto.DonationDetailDTO;
import com.greengrub.donationService.dto.FoodDetailDTO;
import com.greengrub.donationService.mapper.DonationProtoMapper;
import com.greengrub.donationService.service.DonationService;
import com.greengrub.proto.donation.*;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.List;

@Slf4j
@GrpcService
public class DonationGrpcService extends DonationServiceGrpc.DonationServiceImplBase {

    private final DonationService donationService;
    private final DonationProtoMapper mapper;
    private final FoodServiceClient foodServiceClient;

    public DonationGrpcService(
            DonationService donationService,
            DonationProtoMapper mapper,
            FoodServiceClient foodServiceClient) {
        this.donationService = donationService;
        this.mapper = mapper;
        this.foodServiceClient = foodServiceClient;
    }

    @Override
    public void createDonation(CreateDonationRequest request, StreamObserver<DonationResponse> responseObserver) {
        DonationDTO created = donationService.createDonation(mapper.fromProto(request));
        responseObserver.onNext(mapper.toProto(created));
        responseObserver.onCompleted();
    }

    @Override
    public void getAllDonations(DonationListRequest request, StreamObserver<DonationListResponse> responseObserver) {
        List<DonationDTO> all = donationService.getAllDonation();
        DonationListResponse response = DonationListResponse.newBuilder()
                .addAllDonations(all.stream().map(mapper::toProto).toList())
                .setTotalCount(all.size())
                .setPage(request.getPage())
                .setPageSize(request.getPageSize())
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getDonationById(DonationByIdRequest request, StreamObserver<DonationResponse> responseObserver) {
        // gRPC callers get base donation without food hydration (use getFoodItemsByDonationId for enrichment)
        DonationDetailDTO detail = donationService.getDonationDetail(request.getId(), 0, 10);
        responseObserver.onNext(mapper.toProto(detail.getDonation()));
        responseObserver.onCompleted();
    }

    @Override
    public void getDonationsByUserId(DonationByUserIdRequest request, StreamObserver<DonationListResponse> responseObserver) {
        List<DonationDTO> all = donationService.getAllDonation().stream()
                .filter(d -> d.getDonarDetails() != null
                        && request.getUserId().equals(d.getDonarDetails().getUserId()))
                .toList();
        DonationListResponse response = DonationListResponse.newBuilder()
                .addAllDonations(all.stream().map(mapper::toProto).toList())
                .setTotalCount(all.size())
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void updateDonation(UpdateDonationRequest request, StreamObserver<DonationResponse> responseObserver) {
        DonationDTO updated = donationService.updateDonation(request.getId(), mapper.fromProto(request));
        responseObserver.onNext(mapper.toProto(updated));
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
        List<String> foodIds = request.getFoodItemIdList();
        int page = request.getPage();
        int pageSize = request.getPageSize() > 0 ? request.getPageSize() : 10;

        List<FoodDetailDTO> foods = foodServiceClient.getFoodsPage(foodIds, page, pageSize);
        int totalCount = foodIds.size();

        FoodListResponse.Builder builder = FoodListResponse.newBuilder()
                .setTotalCount(totalCount)
                .setPage(page)
                .setPageSize(pageSize);

        for (FoodDetailDTO food : foods) {
            builder.addFoods(Food.newBuilder()
                    .setFoodId(food.getId())
                    .setFoodName(food.getFoodName())
                    .setStatus(food.getStatus() != null ? food.getStatus() : "")
                    .build());
        }

        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void getUsersByDonationId(DonationByIdRequest request, StreamObserver<ListUsersResponse> responseObserver) {
        DonationDetailDTO detail = donationService.getDonationDetail(request.getId(), 0, 1);
        DonationDTO dto = detail.getDonation();
        ListUsersResponse.Builder builder = ListUsersResponse.newBuilder();
        if (dto.getDonarDetails() != null) {
            builder.addUsers(mapper.toUserDetailProto(dto.getDonarDetails()));
            builder.setTotalCount(1);
        }
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }
}
