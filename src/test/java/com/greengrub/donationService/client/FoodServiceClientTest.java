package com.greengrub.donationService.client;

import com.greengrub.donationService.dto.FoodDetailDTO;
import com.greengrub.donationService.exception.FoodServiceException;
import com.greengrub.proto.foods.Food;
import com.greengrub.proto.foods.FoodServiceGrpc;
import com.greengrub.proto.foods.GetFoodsByIdsRequest;
import com.greengrub.proto.foods.GetFoodsByIdsResponse;
import com.greengrub.proto.foods.Quantity;
import com.greengrub.proto.foods.Unit;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FoodServiceClientTest {

    @Mock FoodServiceGrpc.FoodServiceBlockingStub blockingStub;

    FoodServiceClient client;

    @BeforeEach
    void setUp() {
        client = new FoodServiceClient();
        ReflectionTestUtils.setField(client, "blockingStub", blockingStub);
    }

    // ── getFoodsPage ──────────────────────────────────────────────────────────

    @Test
    void getFoodsPage_nullFoodIds_returnsEmpty() {
        assertThat(client.getFoodsPage(null, 0, 10)).isEmpty();
        verifyNoInteractions(blockingStub);
    }

    @Test
    void getFoodsPage_emptyFoodIds_returnsEmpty() {
        assertThat(client.getFoodsPage(List.of(), 0, 10)).isEmpty();
        verifyNoInteractions(blockingStub);
    }

    @Test
    void getFoodsPage_pageOutOfRange_returnsEmpty() {
        assertThat(client.getFoodsPage(List.of("f1"), 5, 10)).isEmpty();
        verifyNoInteractions(blockingStub);
    }

    @Test
    void getFoodsPage_success_returnsMappedDtos() {
        Food food = Food.newBuilder()
                .setId("f1").setFoodName("Apple")
                .setQuantity(Quantity.newBuilder().setAmount(2.0).setUnit(Unit.KG).build())
                .setStatus("AVAILABLE")
                .setRequestedBy("user-1").setRequestedDate("2026-05-01")
                .build();
        GetFoodsByIdsResponse response = GetFoodsByIdsResponse.newBuilder()
                .addFoods(food).build();
        when(blockingStub.getFoodsByIds(any())).thenReturn(response);

        List<FoodDetailDTO> result = client.getFoodsPage(List.of("f1"), 0, 10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("f1");
        assertThat(result.get(0).getFoodName()).isEqualTo("Apple");
        assertThat(result.get(0).getStatus()).isEqualTo("AVAILABLE");
        assertThat(result.get(0).getQuantityUnit()).isEqualTo("KG");
        assertThat(result.get(0).getQuantityAmount()).isEqualTo(2.0);
    }

    @Test
    void getFoodsPage_foodWithUsedByDate_mapsUsedByDate() {
        Food food = Food.newBuilder()
                .setId("f2").setFoodName("Bread")
                .setQuantity(Quantity.newBuilder().setAmount(1.0).setUnit(Unit.KG).build())
                .setUsedByDate("2026-06-30")
                .build();
        GetFoodsByIdsResponse response = GetFoodsByIdsResponse.newBuilder().addFoods(food).build();
        when(blockingStub.getFoodsByIds(any())).thenReturn(response);

        List<FoodDetailDTO> result = client.getFoodsPage(List.of("f2"), 0, 10);

        assertThat(result.get(0).getUsedByDate()).isEqualTo("2026-06-30");
    }

    @Test
    void getFoodsPage_foodWithoutQuantity_nullQuantityFields() {
        Food food = Food.newBuilder().setId("f3").setFoodName("Rice").build();
        GetFoodsByIdsResponse response = GetFoodsByIdsResponse.newBuilder().addFoods(food).build();
        when(blockingStub.getFoodsByIds(any())).thenReturn(response);

        List<FoodDetailDTO> result = client.getFoodsPage(List.of("f3"), 0, 10);

        assertThat(result.get(0).getQuantityAmount()).isNull();
        assertThat(result.get(0).getQuantityUnit()).isNull();
    }

    @Test
    void getFoodsPage_grpcStatusRuntimeException_throwsFoodServiceException() {
        when(blockingStub.getFoodsByIds(any()))
                .thenThrow(new StatusRuntimeException(Status.UNAVAILABLE));

        assertThatThrownBy(() -> client.getFoodsPage(List.of("f1"), 0, 10))
                .isInstanceOf(FoodServiceException.class);
    }

    @Test
    void getFoodsPage_emptyResponse_returnsEmptyList() {
        when(blockingStub.getFoodsByIds(any()))
                .thenReturn(GetFoodsByIdsResponse.newBuilder().build());

        assertThat(client.getFoodsPage(List.of("f1"), 0, 10)).isEmpty();
    }

    // ── getFoodsByIdsFallback ─────────────────────────────────────────────────

    @Test
    void getFoodsByIdsFallback_returnsEmptyList() {
        List<FoodDetailDTO> result = client.getFoodsByIdsFallback(
                List.of("f1"), 0, 10, new RuntimeException("circuit open"));
        assertThat(result).isEmpty();
    }
}
