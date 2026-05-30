package com.greengrub.donationService.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.greengrub.donationService.client.FoodServiceClient;
import com.greengrub.donationService.dto.*;
import com.greengrub.donationService.entity.DonationStatus;
import com.greengrub.donationService.entity.Unit;
import com.greengrub.donationService.kafka.DonationKafkaProducer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DonationApiTest {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate rest;

    @MockBean
    DonationKafkaProducer kafkaProducer;

    @MockBean
    FoodServiceClient foodServiceClient;

    ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // food-service returns empty list by default — best-effort enrichment
        when(foodServiceClient.getFoodsPage(any(), anyInt(), anyInt())).thenReturn(List.of());
    }

    // ── POST /api/v1/donations ────────────────────────────────────────────────

    @Test
    @Order(1)
    void createDonation_validRequest_returns200AndPersistedId() {
        DonationDTO req = buildRequest("Birthday Buffet");

        ResponseEntity<DonationDTO> resp = rest.postForEntity(url("/api/v1/donations"), req, DonationDTO.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().getId()).isNotBlank();
        assertThat(resp.getBody().getDonationName()).isEqualTo("Birthday Buffet");
        assertThat(resp.getBody().getStatus()).isEqualTo(DonationStatus.ACTIVE);
        verify(kafkaProducer, times(1)).publish(any());
    }

    @Test
    @Order(2)
    void createDonation_missingDonationName_returns400WithValidationError() {
        DonationDTO req = buildRequest("");

        ResponseEntity<String> resp = rest.postForEntity(url("/api/v1/donations"), req, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody()).contains("donationName");
    }

    @Test
    @Order(3)
    void createDonation_missingDonorDetails_returns400() {
        DonationDTO req = buildRequest("Test");
        req.setDonarDetails(null);

        ResponseEntity<String> resp = rest.postForEntity(url("/api/v1/donations"), req, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @Order(4)
    void createDonation_invalidDonorEmail_returns400() {
        DonationDTO req = buildRequest("Test");
        req.setDonarDetails(new UserDetailDTO("u1", "Jane", "Doe", "not-an-email", "9876543210"));

        ResponseEntity<String> resp = rest.postForEntity(url("/api/v1/donations"), req, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody()).contains("email");
    }

    @Test
    @Order(5)
    void createDonation_missingStatus_returns400() {
        DonationDTO req = buildRequest("Test");
        req.setStatus(null);

        ResponseEntity<String> resp = rest.postForEntity(url("/api/v1/donations"), req, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @Order(6)
    void createDonation_negativeQuantityAmount_returns400() {
        DonationDTO req = buildRequest("Test");
        req.setEstimatedQuantity(new QuantityDTO(-1.0, Unit.KG));

        ResponseEntity<String> resp = rest.postForEntity(url("/api/v1/donations"), req, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @Order(7)
    void createDonation_invalidJsonBody_returns400() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>("{not valid json}", headers);

        ResponseEntity<String> resp = rest.postForEntity(url("/api/v1/donations"), entity, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody()).contains("Malformed");
    }

    @Test
    @Order(8)
    void createDonation_invalidStatusEnum_returns400WithMessage() {
        String body = """
                {
                  "donationName": "Test",
                  "donarDetails": {"firstName":"A","lastName":"B","email":"a@b.com","phone":"111"},
                  "pickUpAddress": "123 St",
                  "pickUpTime": "2026-07-01T10:00:00",
                  "estimatedQuantity": {"amount": 2.0, "unit": "KG"},
                  "status": "UNKNOWN_STATUS"
                }
                """;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> resp = rest.postForEntity(
                url("/api/v1/donations"), new HttpEntity<>(body, headers), String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody()).contains("UNKNOWN_STATUS");
    }

    // ── GET /api/v1/donations ─────────────────────────────────────────────────

    @Test
    @Order(9)
    void getAllDonations_emptyDatabase_returns200WithEmptyList() {
        ResponseEntity<DonationDTO[]> resp = rest.getForEntity(url("/api/v1/donations"), DonationDTO[].class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull().isEmpty();
    }

    @Test
    @Order(10)
    void getAllDonations_afterCreate_returnsCreatedDonation() {
        rest.postForEntity(url("/api/v1/donations"), buildRequest("Soup Kitchen"), DonationDTO.class);

        ResponseEntity<DonationDTO[]> resp = rest.getForEntity(url("/api/v1/donations"), DonationDTO[].class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).hasSize(1);
        assertThat(resp.getBody()[0].getDonationName()).isEqualTo("Soup Kitchen");
    }

    @Test
    @Order(11)
    void getAllDonations_multipleRecords_returnsAll() {
        rest.postForEntity(url("/api/v1/donations"), buildRequest("Donation A"), DonationDTO.class);
        rest.postForEntity(url("/api/v1/donations"), buildRequest("Donation B"), DonationDTO.class);
        rest.postForEntity(url("/api/v1/donations"), buildRequest("Donation C"), DonationDTO.class);

        ResponseEntity<DonationDTO[]> resp = rest.getForEntity(url("/api/v1/donations"), DonationDTO[].class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).hasSize(3);
    }

    // ── GET /api/v1/donations/{id} ────────────────────────────────────────────

    @Test
    @Order(12)
    void getDonationById_existingId_returns200WithDetail() {
        DonationDTO created = rest.postForEntity(
                url("/api/v1/donations"), buildRequest("Pizza Night"), DonationDTO.class).getBody();
        assertThat(created).isNotNull();

        ResponseEntity<DonationDetailDTO> resp = rest.getForEntity(
                url("/api/v1/donations/" + created.getId()), DonationDetailDTO.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().getDonation().getId()).isEqualTo(created.getId());
        assertThat(resp.getBody().getDonation().getDonationName()).isEqualTo("Pizza Night");
        assertThat(resp.getBody().getTotalFoodItems()).isEqualTo(0);
    }

    @Test
    @Order(13)
    void getDonationById_unknownId_returns404() {
        ResponseEntity<String> resp = rest.getForEntity(
                url("/api/v1/donations/non-existent-id"), String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resp.getBody()).contains("404");
    }

    @Test
    @Order(14)
    void getDonationById_withPaginationParams_returns200() {
        DonationDTO created = rest.postForEntity(
                url("/api/v1/donations"), buildRequest("Pasta"), DonationDTO.class).getBody();
        assertThat(created).isNotNull();

        ResponseEntity<DonationDetailDTO> resp = rest.getForEntity(
                url("/api/v1/donations/" + created.getId() + "?page=0&size=5"), DonationDetailDTO.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().getPageSize()).isEqualTo(5);
        assertThat(resp.getBody().getCurrentPage()).isEqualTo(0);
    }

    @Test
    @Order(15)
    void getDonationById_foodServiceDegrades_stillReturns200() {
        when(foodServiceClient.getFoodsPage(any(), anyInt(), anyInt())).thenReturn(List.of());
        DonationDTO req = buildRequest("Rice Bowl");
        req.setFoodItemsId(List.of("food-1", "food-2"));
        DonationDTO created = rest.postForEntity(url("/api/v1/donations"), req, DonationDTO.class).getBody();
        assertThat(created).isNotNull();

        ResponseEntity<DonationDetailDTO> resp = rest.getForEntity(
                url("/api/v1/donations/" + created.getId()), DonationDetailDTO.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().getTotalFoodItems()).isEqualTo(2);
        assertThat(resp.getBody().getFoodItems()).isEmpty(); // degraded to empty
    }

    @Test
    @Order(16)
    void getDonationById_foodItemsHydrated_returnsInResponse() {
        FoodDetailDTO food = FoodDetailDTO.builder()
                .id("food-1").foodName("Apple").status("AVAILABLE")
                .quantityAmount(1.0).quantityUnit("KG").build();
        when(foodServiceClient.getFoodsPage(any(), anyInt(), anyInt())).thenReturn(List.of(food));

        DonationDTO req = buildRequest("Fruit Basket");
        req.setFoodItemsId(List.of("food-1"));
        DonationDTO created = rest.postForEntity(url("/api/v1/donations"), req, DonationDTO.class).getBody();
        assertThat(created).isNotNull();

        ResponseEntity<DonationDetailDTO> resp = rest.getForEntity(
                url("/api/v1/donations/" + created.getId()), DonationDetailDTO.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().getTotalFoodItems()).isEqualTo(1);
        assertThat(resp.getBody().getFoodItems()).hasSize(1);
        assertThat(resp.getBody().getFoodItems().get(0).getFoodName()).isEqualTo("Apple");
    }

    // ── PUT /api/v1/donations/{id} ────────────────────────────────────────────

    @Test
    @Order(17)
    void updateDonation_existingId_returns200WithUpdatedFields() {
        DonationDTO created = rest.postForEntity(
                url("/api/v1/donations"), buildRequest("Old Name"), DonationDTO.class).getBody();
        assertThat(created).isNotNull();

        DonationDTO updateReq = buildRequest("New Name");
        updateReq.setStatus(DonationStatus.CLAIMED);

        ResponseEntity<DonationDTO> resp = rest.exchange(
                url("/api/v1/donations/" + created.getId()),
                HttpMethod.PUT,
                new HttpEntity<>(updateReq),
                DonationDTO.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().getDonationName()).isEqualTo("New Name");
        assertThat(resp.getBody().getStatus()).isEqualTo(DonationStatus.CLAIMED);
        verify(kafkaProducer, times(2)).publish(any()); // create + update
    }

    @Test
    @Order(18)
    void updateDonation_nonExistentId_returns404() {
        ResponseEntity<String> resp = rest.exchange(
                url("/api/v1/donations/does-not-exist"),
                HttpMethod.PUT,
                new HttpEntity<>(buildRequest("Update")),
                String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @Order(19)
    void updateDonation_invalidBody_returns400() {
        DonationDTO created = rest.postForEntity(
                url("/api/v1/donations"), buildRequest("Original"), DonationDTO.class).getBody();
        assertThat(created).isNotNull();

        DonationDTO bad = buildRequest("");  // blank name — validation failure
        ResponseEntity<String> resp = rest.exchange(
                url("/api/v1/donations/" + created.getId()),
                HttpMethod.PUT,
                new HttpEntity<>(bad),
                String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @Order(20)
    void updateDonation_cancelledStatus_persistedCorrectly() {
        DonationDTO created = rest.postForEntity(
                url("/api/v1/donations"), buildRequest("Wedding"), DonationDTO.class).getBody();
        assertThat(created).isNotNull();

        DonationDTO cancelReq = buildRequest("Wedding");
        cancelReq.setStatus(DonationStatus.CANCELLED);

        rest.exchange(url("/api/v1/donations/" + created.getId()),
                HttpMethod.PUT, new HttpEntity<>(cancelReq), DonationDTO.class);

        // Verify via GET
        ResponseEntity<DonationDetailDTO> detail = rest.getForEntity(
                url("/api/v1/donations/" + created.getId()), DonationDetailDTO.class);
        assertThat(detail.getBody().getDonation().getStatus()).isEqualTo(DonationStatus.CANCELLED);
    }

    @Test
    @Order(21)
    void updateDonation_updatesPickUpAddress_persistedCorrectly() {
        DonationDTO created = rest.postForEntity(
                url("/api/v1/donations"), buildRequest("Party"), DonationDTO.class).getBody();
        assertThat(created).isNotNull();

        DonationDTO updateReq = buildRequest("Party");
        updateReq.setPickUpAddress("999 New St, Updated City");
        rest.exchange(url("/api/v1/donations/" + created.getId()),
                HttpMethod.PUT, new HttpEntity<>(updateReq), DonationDTO.class);

        ResponseEntity<DonationDetailDTO> detail = rest.getForEntity(
                url("/api/v1/donations/" + created.getId()), DonationDetailDTO.class);
        assertThat(detail.getBody().getDonation().getPickUpAddress()).isEqualTo("999 New St, Updated City");
    }

    // ── DELETE /api/v1/donations/{id} ─────────────────────────────────────────

    @Test
    @Order(22)
    void deleteDonation_existingId_returns200AndRemovesRecord() {
        DonationDTO created = rest.postForEntity(
                url("/api/v1/donations"), buildRequest("To Delete"), DonationDTO.class).getBody();
        assertThat(created).isNotNull();

        ResponseEntity<String> deleteResp = rest.exchange(
                url("/api/v1/donations/" + created.getId()),
                HttpMethod.DELETE, HttpEntity.EMPTY, String.class);

        assertThat(deleteResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(deleteResp.getBody()).contains("deleted");

        // Confirm gone
        ResponseEntity<String> getResp = rest.getForEntity(
                url("/api/v1/donations/" + created.getId()), String.class);
        assertThat(getResp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @Order(23)
    void deleteDonation_nonExistentId_returns404() {
        ResponseEntity<String> resp = rest.exchange(
                url("/api/v1/donations/ghost-id"),
                HttpMethod.DELETE, HttpEntity.EMPTY, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @Order(24)
    void deleteDonation_deletedDonationRemovedFromList() {
        DonationDTO d1 = rest.postForEntity(url("/api/v1/donations"), buildRequest("Keep Me"), DonationDTO.class).getBody();
        DonationDTO d2 = rest.postForEntity(url("/api/v1/donations"), buildRequest("Delete Me"), DonationDTO.class).getBody();
        assertThat(d1).isNotNull();
        assertThat(d2).isNotNull();

        rest.exchange(url("/api/v1/donations/" + d2.getId()), HttpMethod.DELETE, HttpEntity.EMPTY, String.class);

        ResponseEntity<DonationDTO[]> list = rest.getForEntity(url("/api/v1/donations"), DonationDTO[].class);
        assertThat(list.getBody()).hasSize(1);
        assertThat(list.getBody()[0].getDonationName()).isEqualTo("Keep Me");
    }

    // ── Method not allowed ────────────────────────────────────────────────────

    @Test
    @Order(25)
    void patchOnDonations_returns405() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> resp = rest.exchange(
                url("/api/v1/donations"), HttpMethod.PATCH,
                new HttpEntity<>("{}", headers), String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
    }

    // ── Full lifecycle: create → read → update → delete ───────────────────────

    @Test
    @Order(26)
    void fullLifecycle_createReadUpdateDelete() {
        // 1. Create
        DonationDTO req = buildRequest("Lifecycle Test");
        req.setFoodItemsId(List.of("f-001", "f-002"));
        DonationDTO created = rest.postForEntity(url("/api/v1/donations"), req, DonationDTO.class).getBody();
        assertThat(created).isNotNull();
        String id = created.getId();
        assertThat(id).isNotBlank();
        assertThat(created.getStatus()).isEqualTo(DonationStatus.ACTIVE);

        // 2. Read detail
        ResponseEntity<DonationDetailDTO> detail = rest.getForEntity(
                url("/api/v1/donations/" + id), DonationDetailDTO.class);
        assertThat(detail.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(detail.getBody().getTotalFoodItems()).isEqualTo(2);

        // 3. Update to CLAIMED
        DonationDTO updateReq = buildRequest("Lifecycle Test - Updated");
        updateReq.setStatus(DonationStatus.CLAIMED);
        ResponseEntity<DonationDTO> updated = rest.exchange(
                url("/api/v1/donations/" + id), HttpMethod.PUT,
                new HttpEntity<>(updateReq), DonationDTO.class);
        assertThat(updated.getBody().getStatus()).isEqualTo(DonationStatus.CLAIMED);

        // 4. Verify update persisted in list
        ResponseEntity<DonationDTO[]> list = rest.getForEntity(url("/api/v1/donations"), DonationDTO[].class);
        assertThat(list.getBody()[0].getStatus()).isEqualTo(DonationStatus.CLAIMED);

        // 5. Delete
        ResponseEntity<String> del = rest.exchange(
                url("/api/v1/donations/" + id), HttpMethod.DELETE, HttpEntity.EMPTY, String.class);
        assertThat(del.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 6. Confirm deletion
        ResponseEntity<String> gone = rest.getForEntity(url("/api/v1/donations/" + id), String.class);
        assertThat(gone.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        // Total kafka events: create + update + delete = 3
        verify(kafkaProducer, times(3)).publish(any());
    }

    // ── Donor details persistence ─────────────────────────────────────────────

    @Test
    @Order(27)
    void createDonation_donorDetailsPersistedCorrectly() {
        DonationDTO req = buildRequest("Donor Test");
        req.setDonarDetails(new UserDetailDTO("user-42", "Alice", "Smith", "alice@example.com", "5551234567"));

        DonationDTO created = rest.postForEntity(url("/api/v1/donations"), req, DonationDTO.class).getBody();
        assertThat(created).isNotNull();

        ResponseEntity<DonationDetailDTO> detail = rest.getForEntity(
                url("/api/v1/donations/" + created.getId()), DonationDetailDTO.class);
        UserDetailDTO donor = detail.getBody().getDonation().getDonarDetails();

        assertThat(donor.getFirstName()).isEqualTo("Alice");
        assertThat(donor.getLastName()).isEqualTo("Smith");
        assertThat(donor.getEmail()).isEqualTo("alice@example.com");
        assertThat(donor.getPhone()).isEqualTo("5551234567");
    }

    // ── Quantity persistence ──────────────────────────────────────────────────

    @Test
    @Order(28)
    void createDonation_quantityInServings_persistedCorrectly() {
        DonationDTO req = buildRequest("Servings Test");
        req.setEstimatedQuantity(new QuantityDTO(25.0, Unit.SERVINGS));

        DonationDTO created = rest.postForEntity(url("/api/v1/donations"), req, DonationDTO.class).getBody();
        assertThat(created).isNotNull();

        ResponseEntity<DonationDetailDTO> detail = rest.getForEntity(
                url("/api/v1/donations/" + created.getId()), DonationDetailDTO.class);
        QuantityDTO qty = detail.getBody().getDonation().getEstimatedQuantity();

        assertThat(qty.getAmount()).isEqualTo(25.0);
        assertThat(qty.getUnit()).isEqualTo(Unit.SERVINGS);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private DonationDTO buildRequest(String name) {
        DonationDTO dto = new DonationDTO();
        dto.setDonationName(name);
        dto.setDonarDetails(new UserDetailDTO("u1", "Jane", "Doe", "jane@example.com", "9876543210"));
        dto.setPickUpAddress("123 Main St, Springfield");
        dto.setPickUpTime(LocalDateTime.of(2026, 8, 1, 12, 0));
        dto.setEstimatedQuantity(new QuantityDTO(5.0, Unit.KG));
        dto.setStatus(DonationStatus.ACTIVE);
        return dto;
    }
}
