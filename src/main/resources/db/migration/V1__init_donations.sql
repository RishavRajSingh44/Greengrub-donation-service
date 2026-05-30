-- Owns the donation-service schema from inception.
-- Hibernate is configured with ddl-auto=validate so any drift between
-- this file and the JPA entities will fail at startup rather than silently
-- mutate the database.
--
-- Embedded field name in the entity is donerDetails (legacy typo preserved
-- intentionally), which Hibernate maps to the doner_details_* column prefix.

CREATE TABLE donations (
    id                          VARCHAR(36)     PRIMARY KEY,
    donation_name               VARCHAR(255)    NOT NULL,

    -- @Embedded UserDetail (field: donerDetails)
    doner_details_user_id       VARCHAR(36),
    doner_details_first_name    VARCHAR(100),
    doner_details_last_name     VARCHAR(100),
    doner_details_email         VARCHAR(255),
    doner_details_phone         VARCHAR(20),

    pick_up_address             VARCHAR(500),
    pick_up_time                TIMESTAMP,

    -- @Embedded Quantity (field: estimatedQuantity)
    estimated_quantity_amount   DOUBLE PRECISION,
    estimated_quantity_unit     VARCHAR(32),

    status                      VARCHAR(32)     NOT NULL,
    creation_date               TIMESTAMP       NOT NULL,
    update_date                 TIMESTAMP       NOT NULL
);

CREATE INDEX idx_donations_status          ON donations (status);
CREATE INDEX idx_donations_donor_user_id   ON donations (doner_details_user_id);
