package com.greengrub.donationService.grpc;

import com.greengrub.donationService.dto.*;
import com.greengrub.donationService.entity.DonationStatus;
import com.greengrub.donationService.entity.Unit;
import com.greengrub.donationService.mapper.DonationProtoMapper;
import com.greengrub.donationService.service.DonationService;
import com.greengrub.proto.donation.*;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DonationGrpcServiceTest {

    @Mock DonationService donationService;
    @Mock DonationProtoMapper mapper;
    @Mock com.greengrub.donationService.client.FoodServiceClient foodServiceClient;

    DonationGrpcService grpcService;

    private DonationDTO sampleDTO;
    private DonationDetailDTO sampleDetail;
    private DonationResponse sampleResponse;

    @BeforeEach
    void setUp() {
        grpcService = new DonationGrpcService(donationService, mapper, foodServiceClient);

        sampleDTO = buildDonationDTO("id-1");
        sampleDetail = DonationDetailDTO.builder()
                .donation(sampleDTO)
                .foodItems(List.of())
                .totalFoodItems(0)
                .currentPage(0).pageSize(10).totalPages(0)
                .build();
        sampleResponse = DonationResponse.newBuilder().setId("id-1").build();
    }

    // ── createDonation ────────────────────────────────────────────────────────

    @Test
    void createDonation_callsServiceAndResponds() {
        CreateDonationRequest req = CreateDonationRequest.newBuilder().build();
        when(mapper.fromProto(req)).thenReturn(sampleDTO);
        when(donationService.createDonation(sampleDTO)).thenReturn(sampleDTO);
        when(mapper.toProto(sampleDTO)).thenReturn(sampleResponse);

        StreamObserver<DonationResponse> obs = captureObserver();
        grpcService.createDonation(req, obs);

        verify(obs).onNext(sampleResponse);
        verify(obs).onCompleted();
    }

    // ── getAllDonations ───────────────────────────────────────────────────────

    @Test
    void getAllDonations_returnsListResponse() {
        DonationListRequest req = DonationListRequest.newBuilder().setPage(0).setPageSize(10).build();
        when(donationService.getAllDonation()).thenReturn(List.of(sampleDTO));
        when(mapper.toProto(sampleDTO)).thenReturn(sampleResponse);

        StreamObserver<DonationListResponse> obs = captureObserver();
        grpcService.getAllDonations(req, obs);

        ArgumentCaptor<DonationListResponse> cap = ArgumentCaptor.forClass(DonationListResponse.class);
        verify(obs).onNext(cap.capture());
        verify(obs).onCompleted();
        assertThat(cap.getValue().getTotalCount()).isEqualTo(1);
        assertThat(cap.getValue().getPage()).isEqualTo(0);
        assertThat(cap.getValue().getPageSize()).isEqualTo(10);
    }

    // ── getDonationById ───────────────────────────────────────────────────────

    @Test
    void getDonationById_callsDetailAndResponds() {
        DonationByIdRequest req = DonationByIdRequest.newBuilder().setId("id-1").build();
        when(donationService.getDonationDetail("id-1", 0, 10)).thenReturn(sampleDetail);
        when(mapper.toProto(sampleDTO)).thenReturn(sampleResponse);

        StreamObserver<DonationResponse> obs = captureObserver();
        grpcService.getDonationById(req, obs);

        verify(obs).onNext(sampleResponse);
        verify(obs).onCompleted();
    }

    // ── getDonationsByUserId ──────────────────────────────────────────────────

    @Test
    void getDonationsByUserId_filtersAndReturns() {
        DonationByUserIdRequest req = DonationByUserIdRequest.newBuilder().setUserId("u1").build();
        when(donationService.getAllDonation()).thenReturn(List.of(sampleDTO));
        when(mapper.toProto(sampleDTO)).thenReturn(sampleResponse);

        StreamObserver<DonationListResponse> obs = captureObserver();
        grpcService.getDonationsByUserId(req, obs);

        ArgumentCaptor<DonationListResponse> cap = ArgumentCaptor.forClass(DonationListResponse.class);
        verify(obs).onNext(cap.capture());
        assertThat(cap.getValue().getTotalCount()).isEqualTo(1);
    }

    @Test
    void getDonationsByUserId_noMatchingUser_returnsEmpty() {
        DonationByUserIdRequest req = DonationByUserIdRequest.newBuilder().setUserId("other").build();
        when(donationService.getAllDonation()).thenReturn(List.of(sampleDTO));

        StreamObserver<DonationListResponse> obs = captureObserver();
        grpcService.getDonationsByUserId(req, obs);

        ArgumentCaptor<DonationListResponse> cap = ArgumentCaptor.forClass(DonationListResponse.class);
        verify(obs).onNext(cap.capture());
        assertThat(cap.getValue().getTotalCount()).isEqualTo(0);
    }

    @Test
    void getDonationsByUserId_donationWithNullDonorSkipped() {
        sampleDTO.setDonarDetails(null);
        DonationByUserIdRequest req = DonationByUserIdRequest.newBuilder().setUserId("u1").build();
        when(donationService.getAllDonation()).thenReturn(List.of(sampleDTO));

        StreamObserver<DonationListResponse> obs = captureObserver();
        grpcService.getDonationsByUserId(req, obs);

        ArgumentCaptor<DonationListResponse> cap = ArgumentCaptor.forClass(DonationListResponse.class);
        verify(obs).onNext(cap.capture());
        assertThat(cap.getValue().getTotalCount()).isEqualTo(0);
    }

    // ── updateDonation ────────────────────────────────────────────────────────

    @Test
    void updateDonation_callsServiceAndResponds() {
        UpdateDonationRequest req = UpdateDonationRequest.newBuilder().setId("id-1").build();
        when(mapper.fromProto(req)).thenReturn(sampleDTO);
        when(donationService.updateDonation("id-1", sampleDTO)).thenReturn(sampleDTO);
        when(mapper.toProto(sampleDTO)).thenReturn(sampleResponse);

        StreamObserver<DonationResponse> obs = captureObserver();
        grpcService.updateDonation(req, obs);

        verify(obs).onNext(sampleResponse);
        verify(obs).onCompleted();
    }

    // ── deleteDonation ────────────────────────────────────────────────────────

    @Test
    void deleteDonation_callsDeleteAndResponds() {
        DonationByIdRequest req = DonationByIdRequest.newBuilder().setId("id-1").build();
        doNothing().when(donationService).deleteDonation("id-1");

        StreamObserver<DeleteDonationResponse> obs = captureObserver();
        grpcService.deleteDonation(req, obs);

        ArgumentCaptor<DeleteDonationResponse> cap = ArgumentCaptor.forClass(DeleteDonationResponse.class);
        verify(obs).onNext(cap.capture());
        assertThat(cap.getValue().getMessage()).contains("id-1");
        verify(obs).onCompleted();
    }

    // ── getFoodItemsByDonationId ──────────────────────────────────────────────

    @Test
    void getFoodItemsByDonationId_returnsFoodListFromRequest() {
        FoodListRequest req = FoodListRequest.newBuilder()
                .addFoodItemId("f1").addFoodItemId("f2")
                .setPage(0).setPageSize(10).build();
        com.greengrub.donationService.dto.FoodDetailDTO food =
                com.greengrub.donationService.dto.FoodDetailDTO.builder()
                        .id("f1").foodName("Apple").status("AVAILABLE").build();
        when(foodServiceClient.getFoodsPage(anyList(), eq(0), eq(10)))
                .thenReturn(List.of(food));

        StreamObserver<FoodListResponse> obs = captureObserver();
        grpcService.getFoodItemsByDonationId(req, obs);

        ArgumentCaptor<FoodListResponse> cap = ArgumentCaptor.forClass(FoodListResponse.class);
        verify(obs).onNext(cap.capture());
        assertThat(cap.getValue().getTotalCount()).isEqualTo(2);
        assertThat(cap.getValue().getFoods(0).getFoodId()).isEqualTo("f1");
    }

    @Test
    void getFoodItemsByDonationId_zeroPageSize_defaultsToTen() {
        FoodListRequest req = FoodListRequest.newBuilder()
                .setPage(0).setPageSize(0).build();

        StreamObserver<FoodListResponse> obs = captureObserver();
        grpcService.getFoodItemsByDonationId(req, obs);

        ArgumentCaptor<FoodListResponse> cap = ArgumentCaptor.forClass(FoodListResponse.class);
        verify(obs).onNext(cap.capture());
        assertThat(cap.getValue().getPageSize()).isEqualTo(10);
        verify(obs).onCompleted();
    }

    @Test
    void getFoodItemsByDonationId_emptyFoodList_returnsZeroCount() {
        FoodListRequest req = FoodListRequest.newBuilder()
                .setPage(0).setPageSize(10).build();

        StreamObserver<FoodListResponse> obs = captureObserver();
        grpcService.getFoodItemsByDonationId(req, obs);

        ArgumentCaptor<FoodListResponse> cap = ArgumentCaptor.forClass(FoodListResponse.class);
        verify(obs).onNext(cap.capture());
        assertThat(cap.getValue().getTotalCount()).isEqualTo(0);
    }

    // ── getUsersByDonationId ──────────────────────────────────────────────────

    @Test
    void getUsersByDonationId_withDonor_returnsUserList() {
        UserDetail protoUser = UserDetail.newBuilder().setUserId("u1").build();
        when(donationService.getDonationDetail("id-1", 0, 1)).thenReturn(sampleDetail);
        when(mapper.toUserDetailProto(sampleDTO.getDonarDetails())).thenReturn(protoUser);

        StreamObserver<ListUsersResponse> obs = captureObserver();
        grpcService.getUsersByDonationId(
                DonationByIdRequest.newBuilder().setId("id-1").build(), obs);

        ArgumentCaptor<ListUsersResponse> cap = ArgumentCaptor.forClass(ListUsersResponse.class);
        verify(obs).onNext(cap.capture());
        assertThat(cap.getValue().getTotalCount()).isEqualTo(1);
    }

    @Test
    void getUsersByDonationId_noDonor_returnsEmptyList() {
        sampleDTO.setDonarDetails(null);
        when(donationService.getDonationDetail("id-1", 0, 1)).thenReturn(sampleDetail);

        StreamObserver<ListUsersResponse> obs = captureObserver();
        grpcService.getUsersByDonationId(
                DonationByIdRequest.newBuilder().setId("id-1").build(), obs);

        ArgumentCaptor<ListUsersResponse> cap = ArgumentCaptor.forClass(ListUsersResponse.class);
        verify(obs).onNext(cap.capture());
        assertThat(cap.getValue().getTotalCount()).isEqualTo(0);
    }

    // ── helper ────────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private <T> StreamObserver<T> captureObserver() {
        return mock(StreamObserver.class);
    }

    private DonationDTO buildDonationDTO(String id) {
        DonationDTO dto = new DonationDTO();
        dto.setId(id);
        dto.setDonationName("Pizza Night");
        dto.setDonarDetails(new UserDetailDTO("u1", "Jane", "Doe", "jane@example.com", "9876543210"));
        dto.setPickUpAddress("99 Park Ave");
        dto.setPickUpTime(LocalDateTime.of(2026, 7, 1, 12, 0));
        dto.setEstimatedQuantity(new QuantityDTO(5.0, Unit.KG));
        dto.setStatus(DonationStatus.ACTIVE);
        return dto;
    }
}
