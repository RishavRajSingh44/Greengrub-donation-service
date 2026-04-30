package com.greengrub.donationService.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "donations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Donation {

    @Id
    private String id;

    private String donationName;

    @Embedded
    private UserDetail donarDetails;

    private String pickUpAddress;

    private LocalDateTime pickUpTime;

    @Embedded
    private Quantity estimatedQuantity;

    @ElementCollection
    @CollectionTable(name = "donation_food_items", joinColumns = @JoinColumn(name = "donation_id"))
    @Column(name = "food_item_id")
    @Builder.Default
    private List<String> foodItemsId = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private DonationStatus status;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime creationDate;

    @UpdateTimestamp
    private LocalDateTime updateDate;

    @PrePersist
    private void generateId() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
    }
}
