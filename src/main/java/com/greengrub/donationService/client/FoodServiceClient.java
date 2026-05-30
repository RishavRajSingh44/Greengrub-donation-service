package com.greengrub.donationService.client;

import com.greengrub.donationService.dto.FoodDetailDTO;
import com.greengrub.donationService.exception.FoodServiceException;
import com.greengrub.proto.foods.Food;
import com.greengrub.proto.foods.FoodServiceGrpc;
import com.greengrub.proto.foods.GetFoodsByIdsRequest;
import com.greengrub.proto.foods.GetFoodsByIdsResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import com.greengrub.proto.foods.Unit;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * gRPC client for food-service.
 *
 * Food hydration is best-effort enrichment on the donation detail page.
 * If food-service is unavailable the donation is still returned — food items
 * degrade to an empty list rather than failing the whole request.
 *
 * Pagination is handled here: the full foodItemsId list is stored on the
 * donation, so we slice the requested page out of that list before calling
 * GetFoodsByIds with only the IDs on the current page.
 */
@Slf4j
@Component
public class FoodServiceClient {

    @GrpcClient("food-service")
    private FoodServiceGrpc.FoodServiceBlockingStub blockingStub;

    /**
     * Fetch one page of food details for the given id list.
     *
     * @param allFoodIds full list of food IDs stored on the donation entity
     * @param page       0-based page index
     * @param size       page size (default 10)
     * @return paginated food detail list; empty list on any failure
     */
    @Retry(name = "foodServiceRetry")
    @CircuitBreaker(name = "foodServiceBreaker", fallbackMethod = "getFoodsByIdsFallback")
    public List<FoodDetailDTO> getFoodsPage(List<String> allFoodIds, int page, int size) {
        if (allFoodIds == null || allFoodIds.isEmpty()) {
            return Collections.emptyList();
        }

        int fromIndex = page * size;
        if (fromIndex >= allFoodIds.size()) {
            return Collections.emptyList();
        }
        int toIndex = Math.min(fromIndex + size, allFoodIds.size());
        List<String> pageIds = allFoodIds.subList(fromIndex, toIndex);

        try {
            GetFoodsByIdsRequest request = GetFoodsByIdsRequest.newBuilder()
                    .addAllFoodIds(pageIds)
                    .build();

            GetFoodsByIdsResponse response = blockingStub.getFoodsByIds(request);
            return response.getFoodsList().stream()
                    .map(FoodServiceClient::toDto)
                    .toList();
        } catch (StatusRuntimeException e) {
            log.warn("food-service getFoodsByIds failed for {} ids, status={}: {}",
                    pageIds.size(), e.getStatus().getCode(), e.getStatus().getDescription());
            throw new FoodServiceException(
                    "food-service call failed: " + e.getStatus().getDescription(), e);
        }
    }

    // Fallback: degrade silently — donation detail still returned, food items empty
    List<FoodDetailDTO> getFoodsByIdsFallback(List<String> allFoodIds, int page, int size, Throwable t) {
        log.warn("food-service unavailable (circuit open or retries exhausted) — " +
                 "returning empty food list for page {}: {}", page, t.getMessage());
        return Collections.emptyList();
    }

    private static FoodDetailDTO toDto(Food food) {
        com.greengrub.proto.foods.Quantity q = food.getQuantity();
        boolean hasQuantity = q.getAmount() != 0.0 || q.getUnit() != Unit.UNIT_UNSPECIFIED;
        return FoodDetailDTO.builder()
                .id(food.getId())
                .foodName(food.getFoodName())
                .quantityAmount(hasQuantity ? q.getAmount() : null)
                .quantityUnit(hasQuantity ? q.getUnit().name() : null)
                .status(food.getStatus())
                .requestedBy(food.getRequestedBy())
                .requestedDate(food.getRequestedDate())
                .usedByDate(food.getUsedByDate().isEmpty() ? null : food.getUsedByDate())
                .build();
    }
}
