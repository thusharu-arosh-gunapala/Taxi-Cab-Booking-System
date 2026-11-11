CREATE TABLE passengers (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(120) NOT NULL,
    email VARCHAR(254) NOT NULL UNIQUE,
    phone VARCHAR(32) NOT NULL,
    address VARCHAR(255) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role VARCHAR(16) NOT NULL DEFAULT 'PASSENGER',
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE favourite_locations (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    passenger_id BIGINT NOT NULL,
    location_type VARCHAR(16) NOT NULL,
    address VARCHAR(255) NOT NULL,
    latitude DOUBLE NOT NULL,
    longitude DOUBLE NOT NULL,
    CONSTRAINT fk_favourite_passenger FOREIGN KEY (passenger_id) REFERENCES passengers (id)
);

CREATE TABLE bookings (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    passenger_id BIGINT NOT NULL,
    pickup_address VARCHAR(255) NOT NULL,
    destination_address VARCHAR(255) NOT NULL,
    pickup_latitude DOUBLE NOT NULL,
    pickup_longitude DOUBLE NOT NULL,
    destination_latitude DOUBLE NOT NULL,
    destination_longitude DOUBLE NOT NULL,
    estimated_distance_km DECIMAL(10, 3) NOT NULL,
    estimated_fare DECIMAL(12, 2) NOT NULL,
    fare_currency VARCHAR(3) NOT NULL,
    vehicle_category VARCHAR(16) NOT NULL,
    peak_hour BOOLEAN NOT NULL,
    status VARCHAR(24) NOT NULL,
    booking_time TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT NOT NULL,
    CONSTRAINT fk_booking_passenger FOREIGN KEY (passenger_id) REFERENCES passengers (id)
);

CREATE INDEX idx_bookings_passenger_created ON bookings (passenger_id, created_at);

CREATE TABLE trips (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    booking_id BIGINT NOT NULL UNIQUE,
    started_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP NULL,
    CONSTRAINT fk_trip_booking FOREIGN KEY (booking_id) REFERENCES bookings (id)
);

ALTER TABLE bookings ADD COLUMN booking_reference VARCHAR(45) NULL;
ALTER TABLE bookings ADD COLUMN estimated_duration_minutes BIGINT NOT NULL DEFAULT 1;
ALTER TABLE bookings ADD COLUMN assigned_driver_reference VARCHAR(120) NULL;
ALTER TABLE bookings ADD COLUMN cancellation_reason VARCHAR(500) NULL;

UPDATE bookings
SET booking_reference = CONCAT('RIDE-LEGACY-', id)
WHERE booking_reference IS NULL;

ALTER TABLE bookings MODIFY booking_reference VARCHAR(45) NOT NULL;
CREATE UNIQUE INDEX idx_bookings_reference ON bookings (booking_reference);

UPDATE bookings
SET status = CASE status
    WHEN 'REQUESTED' THEN 'PENDING'
    ELSE status
END;

CREATE TABLE promotions (
    promo_code VARCHAR(32) NOT NULL PRIMARY KEY,
    discount_type VARCHAR(20) NOT NULL,
    discount_value DECIMAL(10, 2) NOT NULL,
    minimum_fare DECIMAL(12, 2) NOT NULL DEFAULT 0,
    maximum_discount DECIMAL(12, 2) NULL,
    expires_on DATE NULL,
    usage_limit BIGINT NULL,
    usage_count BIGINT NOT NULL DEFAULT 0,
    reserved_count BIGINT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE payments (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    payment_reference VARCHAR(45) NOT NULL UNIQUE,
    booking_id BIGINT NOT NULL UNIQUE,
    passenger_id BIGINT NOT NULL,
    promo_code VARCHAR(32) NULL,
    payment_method VARCHAR(24) NOT NULL,
    status VARCHAR(24) NOT NULL,
    original_amount DECIMAL(12, 2) NOT NULL,
    discount_amount DECIMAL(12, 2) NOT NULL,
    amount DECIMAL(12, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    provider_session_id VARCHAR(255) NULL UNIQUE,
    provider_transaction_reference VARCHAR(255) NULL,
    promotion_usage_reserved BOOLEAN NOT NULL DEFAULT FALSE,
    checkout_attempt INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_payment_booking FOREIGN KEY (booking_id) REFERENCES bookings (id),
    CONSTRAINT fk_payment_passenger FOREIGN KEY (passenger_id) REFERENCES passengers (id),
    CONSTRAINT fk_payment_promotion FOREIGN KEY (promo_code) REFERENCES promotions (promo_code)
);

CREATE INDEX idx_payments_passenger_created ON payments (passenger_id, created_at);

CREATE TABLE receipts (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    receipt_reference VARCHAR(45) NOT NULL UNIQUE,
    payment_id BIGINT NOT NULL UNIQUE,
    pickup_address VARCHAR(255) NOT NULL,
    destination_address VARCHAR(255) NOT NULL,
    distance_km DECIMAL(10, 3) NOT NULL,
    issued_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_receipt_payment FOREIGN KEY (payment_id) REFERENCES payments (id)
);

CREATE TABLE refunds (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    refund_reference VARCHAR(45) NOT NULL UNIQUE,
    payment_id BIGINT NOT NULL UNIQUE,
    passenger_id BIGINT NOT NULL,
    reason VARCHAR(500) NOT NULL,
    refund_amount DECIMAL(12, 2) NOT NULL,
    status VARCHAR(24) NOT NULL,
    requested_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_refund_payment FOREIGN KEY (payment_id) REFERENCES payments (id),
    CONSTRAINT fk_refund_passenger FOREIGN KEY (passenger_id) REFERENCES passengers (id)
);

CREATE INDEX idx_refunds_passenger_requested ON refunds (passenger_id, requested_at);

CREATE TABLE drivers (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    phone VARCHAR(32) NOT NULL,
    email VARCHAR(254) NOT NULL UNIQUE,
    license_number VARCHAR(64) NOT NULL UNIQUE,
    nic_number VARCHAR(32) NOT NULL UNIQUE,
    address VARCHAR(255) NOT NULL,
    license_expiry_date DATE NOT NULL,
    profile_photo_key VARCHAR(80) NULL,
    license_image_key VARCHAR(80) NULL,
    application_status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    rejection_reason VARCHAR(1000) NULL,
    driver_reference VARCHAR(32) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL DEFAULT '!',
    account_active BOOLEAN NOT NULL DEFAULT FALSE,
    must_change_password BOOLEAN NOT NULL DEFAULT FALSE,
    credential_email_status VARCHAR(16) NOT NULL DEFAULT 'NOT_SENT',
    approved BOOLEAN NOT NULL DEFAULT FALSE,
    online BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE vehicles (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    vehicle_category VARCHAR(16) NOT NULL,
    brand VARCHAR(80) NOT NULL DEFAULT 'Unknown',
    model VARCHAR(120) NOT NULL,
    manufacturing_year INT NOT NULL DEFAULT 2000,
    plate_number VARCHAR(20) NOT NULL UNIQUE,
    color VARCHAR(40) NOT NULL,
    photo_key VARCHAR(80) NULL,
    available BOOLEAN NOT NULL DEFAULT TRUE,
    driver_id BIGINT NULL,
    CONSTRAINT fk_vehicle_driver FOREIGN KEY (driver_id) REFERENCES drivers (id)
);

CREATE INDEX idx_vehicles_driver ON vehicles (driver_id);
CREATE INDEX idx_vehicles_matching ON vehicles (available, vehicle_category);

CREATE TABLE driver_password_reset_tokens (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    driver_id BIGINT NOT NULL UNIQUE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_driver_password_reset_driver FOREIGN KEY (driver_id) REFERENCES drivers (id) ON DELETE CASCADE
);

CREATE TABLE administrators (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(120) NOT NULL,
    email VARCHAR(254) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

INSERT INTO administrators (full_name, email, password_hash, created_at)
SELECT full_name, email, password_hash, created_at
FROM passengers
WHERE role = 'ADMIN';

UPDATE passengers
SET role = 'PASSENGER'
WHERE role = 'ADMIN';
