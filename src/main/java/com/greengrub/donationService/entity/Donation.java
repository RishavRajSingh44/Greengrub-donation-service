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
    @AttributeOverrides({
            @AttributeOverride(name = "userId",    column = @Column(name = "doner_details_user_id")),
            @AttributeOverride(name = "firstName", column = @Column(name = "doner_details_first_name")),
            @AttributeOverride(name = "lastName",  column = @Column(name = "doner_details_last_name")),
            @AttributeOverride(name = "email",     column = @Column(name = "doner_details_email")),
            @AttributeOverride(name = "phone",     column = @Column(name = "doner_details_phone"))
    })
    private UserDetail donerDetails;

    private String pickUpAddress;

    private LocalDateTime pickUpTime;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "estimated_quantity_amount")),
            @AttributeOverride(name = "unit",   column = @Column(name = "estimated_quantity_unit"))
    })
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
