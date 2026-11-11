CREATE TABLE driver_email_outbox (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    driver_id BIGINT NOT NULL UNIQUE,
    recipient VARCHAR(254) NOT NULL,
    email_type VARCHAR(16) NOT NULL,
    encrypted_payload VARCHAR(2048) NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    sent_at TIMESTAMP NULL,
    CONSTRAINT fk_driver_email_outbox_driver
        FOREIGN KEY (driver_id) REFERENCES drivers (id) ON DELETE CASCADE
);

CREATE INDEX idx_driver_email_outbox_pending
    ON driver_email_outbox (sent_at, created_at);
