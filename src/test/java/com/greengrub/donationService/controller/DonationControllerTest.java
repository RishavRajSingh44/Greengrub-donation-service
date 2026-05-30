package com.greengrub.donationService.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.greengrub.donationService.dto.*;
import com.greengrub.donationService.entity.DonationStatus;
import com.greengrub.donationService.entity.Unit;
import com.greengrub.donationService.exception.DonationNotFoundException;
import com.greengrub.donationService.exception.FoodServiceException;
import com.greengrub.donationService.exception.GlobalExceptionHandler;
import com.greengrub.donationService.service.DonationService;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class DonationControllerTest {

    @Mock DonationService donationService;

    MockMvc mockMvc;
    ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        mockMvc = MockMvcBuilders
                .standaloneSetup(new DonationController(donationService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    // ── GET /api/v1/donations ─────────────────────────────────────────────────

    @Test
    void getAllDonations_returns200WithList() throws Exception {
        when(donationService.getAllDonation()).thenReturn(List.of(buildDTO("id-1")));

        mockMvc.perform(get("/api/v1/donations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value("id-1"));
    }

    @Test
    void getAllDonations_empty_returns200WithEmptyList() throws Exception {
        when(donationService.getAllDonation()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/donations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // ── GET /api/v1/donations/{id} ────────────────────────────────────────────

    @Test
    void getDonationDetail_found_returns200() throws Exception {
        DonationDetailDTO detail = DonationDetailDTO.builder()
                .donation(buildDTO("id-1"))
                .foodItems(List.of())
                .totalFoodItems(0)
                .currentPage(0)
                .pageSize(10)
                .totalPages(0)
                .build();
        when(donationService.getDonationDetail("id-1", 0, 10)).thenReturn(detail);

        mockMvc.perform(get("/api/v1/donations/id-1").param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.donation.id").value("id-1"))
                .andExpect(jsonPath("$.totalFoodItems").value(0));
    }

    @Test
    void getDonationDetail_notFound_returns404() throws Exception {
        when(donationService.getDonationDetail(eq("bad"), anyInt(), anyInt()))
                .thenThrow(new DonationNotFoundException("bad"));

        mockMvc.perform(get("/api/v1/donations/bad"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void getDonationDetail_foodServiceUnavailable_returns503() throws Exception {
        when(donationService.getDonationDetail(eq("id-1"), anyInt(), anyInt()))
                .thenThrow(new FoodServiceException("food-service down", new RuntimeException()));

        mockMvc.perform(get("/api/v1/donations/id-1"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("Food Service Unavailable"));
    }

    // ── POST /api/v1/donations ────────────────────────────────────────────────

    @Test
    void createDonation_valid_returns200() throws Exception {
        DonationDTO req = buildDTO(null);
        DonationDTO created = buildDTO("new-id");
        when(donationService.createDonation(any())).thenReturn(created);

        mockMvc.perform(post("/api/v1/donations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("new-id"));
    }

    @Test
    void createDonation_missingDonationName_returns400() throws Exception {
        DonationDTO req = buildDTO(null);
        req.setDonationName("");

        mockMvc.perform(post("/api/v1/donations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void createDonation_missingBody_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/donations")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createDonation_invalidJson_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/donations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{bad json}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createDonation_invalidStatusEnum_returns400() throws Exception {
        String body = objectMapper.writeValueAsString(buildDTO(null))
                .replace("\"ACTIVE\"", "\"INVALID_STATUS\"");

        mockMvc.perform(post("/api/v1/donations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Malformed Request Body"));
    }

    // ── PUT /api/v1/donations/{id} ────────────────────────────────────────────

    @Test
    void updateDonation_valid_returns200() throws Exception {
        DonationDTO updated = buildDTO("id-1");
        updated.setStatus(DonationStatus.CLAIMED);
        when(donationService.updateDonation(eq("id-1"), any())).thenReturn(updated);

        mockMvc.perform(put("/api/v1/donations/id-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildDTO("id-1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLAIMED"));
    }

    @Test
    void updateDonation_notFound_returns404() throws Exception {
        when(donationService.updateDonation(eq("missing"), any()))
                .thenThrow(new DonationNotFoundException("missing"));

        mockMvc.perform(put("/api/v1/donations/missing")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildDTO("missing"))))
                .andExpect(status().isNotFound());
    }

    // ── DELETE /api/v1/donations/{id} ─────────────────────────────────────────

    @Test
    void deleteDonation_valid_returns200() throws Exception {
        doNothing().when(donationService).deleteDonation("id-1");

        mockMvc.perform(delete("/api/v1/donations/id-1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("deleted")));
    }

    @Test
    void deleteDonation_notFound_returns404() throws Exception {
        doThrow(new DonationNotFoundException("missing")).when(donationService).deleteDonation("missing");

        mockMvc.perform(delete("/api/v1/donations/missing"))
                .andExpect(status().isNotFound());
    }

    // ── Exception handler edge cases ──────────────────────────────────────────

    @Test
    void dataAccessException_returns503() throws Exception {
        when(donationService.getAllDonation())
                .thenThrow(new DataAccessResourceFailureException("DB down"));

        mockMvc.perform(get("/api/v1/donations"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("Database Unavailable"));
    }

    @Test
    void methodNotAllowed_returns405() throws Exception {
        mockMvc.perform(patch("/api/v1/donations"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void unhandledException_returns500() throws Exception {
        when(donationService.getAllDonation()).thenThrow(new RuntimeException("unexpected"));

        mockMvc.perform(get("/api/v1/donations"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Internal Server Error"));
    }

    @Test
    void constraintViolationException_returns400() throws Exception {
        // Build a real ConstraintViolation via Hibernate Validator to avoid Mockito interference
        // Use lenient mocking to avoid UnfinishedStubbing from ConstraintViolationException constructor
        ConstraintViolation<?> cv = mock(ConstraintViolation.class, org.mockito.Mockito.withSettings().lenient());
        when(cv.getMessage()).thenReturn("must not be blank");
        Path path = mock(Path.class, org.mockito.Mockito.withSettings().lenient());
        when(path.toString()).thenReturn("donationName");
        when(cv.getPropertyPath()).thenReturn(path);
        ConstraintViolationException cvEx = new ConstraintViolationException("Validation error", Set.of(cv));

        when(donationService.getAllDonation()).thenThrow(cvEx);

        mockMvc.perform(get("/api/v1/donations"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void circuitBreakerOpen_returns503() throws Exception {
        when(donationService.getAllDonation())
                .thenThrow(CallNotPermittedException.createCallNotPermittedException(
                        CircuitBreaker.ofDefaults("dbBreaker")));

        mockMvc.perform(get("/api/v1/donations"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("Service Unavailable"));
    }

    // ── helper ────────────────────────────────────────────────────────────────

    private DonationDTO buildDTO(String id) {
        DonationDTO dto = new DonationDTO();
        dto.setId(id);
        dto.setDonationName("Wedding Buffet");
        dto.setDonarDetails(new UserDetailDTO("u1", "Jane", "Doe", "jane@example.com", "9876543210"));
        dto.setPickUpAddress("123 Main St");
        dto.setPickUpTime(LocalDateTime.of(2026, 7, 1, 12, 0));
        dto.setEstimatedQuantity(new QuantityDTO(5.0, Unit.KG));
        dto.setStatus(DonationStatus.ACTIVE);
        return dto;
    }
}
