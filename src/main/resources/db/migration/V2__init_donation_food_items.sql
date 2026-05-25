-- @ElementCollection mapping for List<String> foodItemsId in Donation entity.
-- @CollectionTable: name=donation_food_items, joinColumns=donation_id
-- @Column: food_item_id
-- ON DELETE CASCADE: removing a donation row drops its food-item ID rows automatically.

CREATE TABLE donation_food_items (
    donation_id  VARCHAR(36) NOT NULL,
    food_item_id VARCHAR(36) NOT NULL,
    PRIMARY KEY (donation_id, food_item_id),
    CONSTRAINT fk_donation_food_items FOREIGN KEY (donation_id)
        REFERENCES donations (id) ON DELETE CASCADE
);
