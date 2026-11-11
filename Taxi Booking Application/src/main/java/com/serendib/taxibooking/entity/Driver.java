package com.serendib.taxibooking.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "drivers")
public class Driver implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 32)
    private String phone;

    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Column(name = "license_number", unique = true, length = 64)
    private String licenseNumber;

    @Column(name = "nic_number", unique = true, length = 32)
    private String nicNumber;

    @Column(length = 255)
    private String address;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "license_expiry_date")
    private LocalDate licenseExpiryDate;

    @Column(name = "profile_photo_key", length = 80)
    private String profilePhotoKey;

    @Column(name = "license_image_key", length = 80)
    private String licenseImageKey;

    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "application_status", nullable = false, length = 24)
    private DriverApplicationStatus applicationStatus = DriverApplicationStatus.NOT_SUBMITTED;

    @Column(name = "driver_reference", nullable = false, unique = true, length = 32)
    private String driverReference;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash = "!";

    @Column(name = "account_active", nullable = false)
    private boolean accountActive;

    @Column(nullable = false)
    private boolean approved;

    @Column(nullable = false)
    private boolean online;

    @Column(nullable = false)
    private boolean suspended;

    @Column(name = "approved_by_admin_id")
    private Long approvedByAdminId;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "internal_notes", columnDefinition = "TEXT")
    private String internalNotes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Driver() {
    }

    public Driver(String name, String phone, String email, String licenseNumber) {
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.licenseNumber = licenseNumber;
        this.nicNumber = "LEGACY-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        this.address = "Not provided";
        this.licenseExpiryDate = LocalDate.of(2099, 12, 31);
        this.driverReference = "DRV-" + UUID.randomUUID().toString().replace("-", "")
                .substring(0, 20).toUpperCase();
        this.approved = false;
        this.online = false;
        this.accountActive = false;
        this.applicationStatus = DriverApplicationStatus.PENDING;
        this.createdAt = Instant.now();
    }

    public static Driver registered(String name, String phone, String email, String passwordHash) {
        Driver driver = new Driver();
        driver.name = name;
        driver.phone = phone;
        driver.email = email;
        driver.passwordHash = passwordHash;
        driver.driverReference = "DRV-" + UUID.randomUUID().toString().replace("-", "")
                .substring(0, 20).toUpperCase();
        driver.applicationStatus = DriverApplicationStatus.NOT_SUBMITTED;
        driver.accountActive = true;
        driver.createdAt = Instant.now();
        return driver;
    }

    public void completeApplication(String nicNumber, String address, LocalDate dateOfBirth,
                                    LocalDate licenseExpiryDate, String profilePhotoKey,
                                    String licenseImageKey) {
        this.nicNumber = nicNumber;
        this.address = address;
        this.dateOfBirth = dateOfBirth;
        this.licenseExpiryDate = licenseExpiryDate;
        this.profilePhotoKey = profilePhotoKey;
        this.licenseImageKey = licenseImageKey;
        this.applicationStatus = DriverApplicationStatus.PENDING;
        this.approved = false;
        this.online = false;
        this.rejectionReason = null;
    }

    public void updateDetails(String name, String phone, String email, String licenseNumber) {
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.licenseNumber = licenseNumber;
    }

    public void approve(Long administratorId) {
        if (applicationStatus != DriverApplicationStatus.PENDING) {
            throw new IllegalStateException("Only a pending driver application can be approved.");
        }
        approved = true;
        applicationStatus = DriverApplicationStatus.APPROVED;
        rejectionReason = null;
        accountActive = true;
        approvedByAdminId = administratorId;
        approvedAt = Instant.now();
        suspended = false;
        online = false;
    }

    public void changePassword(String passwordHash) {
        if (!approved || !accountActive || suspended) {
            throw new IllegalStateException("Only an approved active driver can change their password.");
        }
        this.passwordHash = passwordHash;
    }

    public void reject(String reason) {
        if (applicationStatus != DriverApplicationStatus.PENDING) {
            throw new IllegalStateException("Only a pending driver application can be rejected.");
        }
        applicationStatus = DriverApplicationStatus.REJECTED;
        rejectionReason = reason;
        approved = false;
        online = false;
    }

    public void requestResubmission(String reason) {
        if (applicationStatus != DriverApplicationStatus.PENDING
                && applicationStatus != DriverApplicationStatus.REJECTED) {
            throw new IllegalStateException("Only an application under review can be returned for correction.");
        }
        applicationStatus = DriverApplicationStatus.RESUBMISSION_REQUIRED;
        rejectionReason = reason;
        approved = false;
        online = false;
    }

    public void updateInternalNotes(String notes) {
        internalNotes = notes;
    }

    public void setSuspended(boolean suspended) {
        if (applicationStatus != DriverApplicationStatus.APPROVED) {
            throw new IllegalStateException("Only an approved driver account can be suspended or reactivated.");
        }
        this.suspended = suspended;
        this.accountActive = !suspended;
        if (suspended) {
            online = false;
        }
    }

    public void toggleOnline() {
        if (!approved || !accountActive || suspended
                || applicationStatus != DriverApplicationStatus.APPROVED) {
            throw new IllegalStateException("Only an approved active driver can change availability.");
        }
        online = !online;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public String getNicNumber() {
        return nicNumber;
    }

    public String getAddress() {
        return address;
    }

    public LocalDate getLicenseExpiryDate() {
        return licenseExpiryDate;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public String getProfilePhotoKey() {
        return profilePhotoKey;
    }

    public String getLicenseImageKey() {
        return licenseImageKey;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public DriverApplicationStatus getApplicationStatus() {
        return applicationStatus;
    }

    public Long getApprovedByAdminId() {
        return approvedByAdminId;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }

    public String getInternalNotes() {
        return internalNotes;
    }

    public boolean isSuspended() {
        return suspended;
    }

    public boolean isApproved() {
        return approved;
    }

    public boolean isOnline() {
        return online;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getDriverReference() {
        return driverReference;
    }

    public boolean isAccountActive() {
        return accountActive;
    }

    public boolean isMustChangePassword() {
        return false;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_DRIVER"));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return accountActive;
    }
}
