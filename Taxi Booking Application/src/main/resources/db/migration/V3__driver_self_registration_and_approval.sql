ALTER TABLE drivers
    MODIFY license_number VARCHAR(64) NULL,
    MODIFY nic_number VARCHAR(32) NULL,
    MODIFY address VARCHAR(255) NULL,
    MODIFY license_expiry_date DATE NULL,
    MODIFY application_status VARCHAR(24) NOT NULL DEFAULT 'NOT_SUBMITTED',
    DROP COLUMN must_change_password,
    DROP COLUMN credential_email_status;

ALTER TABLE drivers
    ADD COLUMN date_of_birth DATE NULL,
    ADD COLUMN approved_by_admin_id BIGINT NULL,
    ADD COLUMN approved_at TIMESTAMP NULL,
    ADD COLUMN internal_notes TEXT NULL,
    ADD COLUMN suspended BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE drivers
    ADD CONSTRAINT fk_driver_approved_by_admin
        FOREIGN KEY (approved_by_admin_id) REFERENCES administrators (id);

ALTER TABLE vehicles
    ADD COLUMN registration_document_key VARCHAR(80) NULL;

CREATE INDEX idx_drivers_application_status_created
    ON drivers (application_status, created_at);

CREATE INDEX idx_drivers_available
    ON drivers (approved, account_active, suspended, online);

DROP TABLE driver_email_outbox;
