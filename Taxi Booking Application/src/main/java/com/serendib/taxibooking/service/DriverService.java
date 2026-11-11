package com.serendib.taxibooking.service;

import com.serendib.taxibooking.dto.DriverAccountRegistrationForm;
import com.serendib.taxibooking.dto.DriverApplicationForm;
import com.serendib.taxibooking.dto.DriverRegistrationForm;
import com.serendib.taxibooking.entity.Driver;
import com.serendib.taxibooking.entity.DriverApplicationStatus;
import com.serendib.taxibooking.entity.Vehicle;
import com.serendib.taxibooking.repository.AdministratorRepository;
import com.serendib.taxibooking.repository.DriverPasswordResetTokenRepository;
import com.serendib.taxibooking.repository.DriverRepository;
import com.serendib.taxibooking.repository.PassengerRepository;
import com.serendib.taxibooking.repository.VehicleRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.Locale;

@Service
public class DriverService {

    private final DriverRepository driverRepository;
    private final PassengerRepository passengerRepository;
    private final AdministratorRepository administratorRepository;
    private final VehicleRepository vehicleRepository;
    private final DriverDocumentStorageService documentStorageService;
    private final DriverPasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;

    public DriverService(DriverRepository driverRepository,
                         PassengerRepository passengerRepository,
                         AdministratorRepository administratorRepository,
                         VehicleRepository vehicleRepository,
                         DriverDocumentStorageService documentStorageService,
                         DriverPasswordResetTokenRepository passwordResetTokenRepository,
                         PasswordEncoder passwordEncoder) {
        this.driverRepository = driverRepository;
        this.passengerRepository = passengerRepository;
        this.administratorRepository = administratorRepository;
        this.vehicleRepository = vehicleRepository;
        this.documentStorageService = documentStorageService;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Driver register(DriverAccountRegistrationForm form) {
        String email = normalizeEmail(form.getEmail());
        if (!form.getPassword().equals(form.getConfirmPassword())) {
            throw new IllegalArgumentException("The passwords do not match.");
        }
        if (form.getPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Password must not exceed 72 UTF-8 bytes.");
        }
        validateUniqueEmail(email, null);
        return driverRepository.save(Driver.registered(
                form.getFullName().trim(),
                form.getPhone().trim(),
                email,
                passwordEncoder.encode(form.getPassword())));
    }

    @Transactional
    public Driver apply(DriverApplicationForm form) {
        String email = normalizeEmail(form.getEmail());
        String licenseNumber = normalizeLicense(form.getLicenseNumber());
        validateUnique(email, licenseNumber, null);
        return driverRepository.save(new Driver(
                form.getName().trim(),
                form.getPhone().trim(),
                email,
                licenseNumber));
    }

    @Transactional
    public Driver applyWithVehicle(Driver authenticatedDriver, DriverRegistrationForm form) {
        Driver driver = findByIdForUpdate(authenticatedDriver.getId());
        DriverApplicationStatus currentStatus = driver.getApplicationStatus();
        if (currentStatus != DriverApplicationStatus.NOT_SUBMITTED
                && currentStatus != DriverApplicationStatus.REJECTED
                && currentStatus != DriverApplicationStatus.RESUBMISSION_REQUIRED) {
            throw new IllegalStateException("You already have an active driver application.");
        }
        if (form.getDateOfBirth().isAfter(LocalDate.now().minusYears(18))) {
            throw new IllegalArgumentException("Applicants must be at least 18 years old.");
        }

        String licenseNumber = normalizeLicense(form.getLicenseNumber());
        String plateNumber = normalizePlate(form.getPlateNumber());
        if (driverRepository.existsByLicenseNumberIgnoreCaseAndIdNot(licenseNumber, driver.getId())) {
            throw new IllegalArgumentException("A driver application already uses this licence number.");
        }
        String nicNumber = normalizeNic(form.getNicNumber());
        if (driverRepository.existsByNicNumberIgnoreCaseAndIdNot(nicNumber, driver.getId())) {
            throw new IllegalArgumentException("A driver application already uses this NIC number.");
        }
        if (vehicleRepository.existsByPlateNumberIgnoreCase(plateNumber)) {
            throw new IllegalArgumentException("A vehicle already uses this registration plate.");
        }
        if (form.getVehicleYear() > Year.now().getValue()) {
            throw new IllegalArgumentException("Vehicle year cannot be in the future.");
        }

        String profilePhotoKey = null;
        String licenseImageKey = null;
        String vehiclePhotoKey = null;
        String registrationDocumentKey = null;
        try {
            profilePhotoKey = documentStorageService.storeRequiredImage(form.getProfilePhoto());
            licenseImageKey = documentStorageService.storeRequiredImage(form.getLicenseImage());
            vehiclePhotoKey = documentStorageService.storeRequiredImage(form.getVehiclePhoto());
            registrationDocumentKey = documentStorageService.storeRequiredImage(
                    form.getVehicleRegistrationDocument());

            driver.updateDetails(form.getName().trim(), form.getPhone().trim(),
                    driver.getEmail(), licenseNumber);
            driver.completeApplication(nicNumber, form.getAddress().trim(), form.getDateOfBirth(),
                    form.getLicenseExpiryDate(), profilePhotoKey, licenseImageKey);
            driverRepository.saveAndFlush(driver);

            Vehicle vehicle = new Vehicle(
                    form.getVehicleCategory(),
                    form.getVehicleBrand().trim(),
                    form.getVehicleModel().trim(),
                    form.getVehicleYear(),
                    plateNumber,
                    form.getVehicleColor().trim(),
                    vehiclePhotoKey,
                    driver);
            vehicle.setRegistrationDocumentKey(registrationDocumentKey);
            vehicleRepository.saveAndFlush(vehicle);
            return driver;
        } catch (RuntimeException exception) {
            documentStorageService.deleteQuietly(profilePhotoKey);
            documentStorageService.deleteQuietly(licenseImageKey);
            documentStorageService.deleteQuietly(vehiclePhotoKey);
            documentStorageService.deleteQuietly(registrationDocumentKey);
            throw exception;
        }
    }

    @Transactional
    public Driver update(Long id, DriverApplicationForm form) {
        Driver driver = findById(id);
        String email = normalizeEmail(form.getEmail());
        String licenseNumber = normalizeLicense(form.getLicenseNumber());
        validateUnique(email, licenseNumber, id);
        driver.updateDetails(form.getName().trim(), form.getPhone().trim(), email, licenseNumber);
        return driver;
    }

    @Transactional(readOnly = true)
    public List<Driver> findAll() {
        return driverRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<Driver> searchApplications(DriverApplicationStatus status, String search) {
        String normalizedSearch = search == null || search.isBlank() ? null : search.trim();
        return driverRepository.searchApplications(status, normalizedSearch);
    }

    @Transactional(readOnly = true)
    public List<Driver> findApproved() {
        return driverRepository.findAllByApprovedTrueOrderByNameAsc();
    }

    @Transactional
    public Driver findById(Long id) {
        return driverRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Driver was not found."));
    }

    @Transactional
    public Driver approve(Long id, Long administratorId) {
        Driver driver = findByIdForUpdate(id);
        driver.approve(administratorId);
        return driverRepository.saveAndFlush(driver);
    }

    @Transactional
    public Driver requestResubmission(Long id, String reason) {
        validateRejectionReason(reason);
        Driver driver = findByIdForUpdate(id);
        driver.requestResubmission(reason.trim());
        return driver;
    }

    @Transactional
    public void toggleOnline(Long id) {
        Driver driver = findById(id);
        if (driver.getLicenseExpiryDate() != null
                && driver.getLicenseExpiryDate().isBefore(LocalDate.now())) {
            throw new IllegalStateException("The driver's licence has expired and must be renewed before going online.");
        }
        driver.toggleOnline();
    }

    @Transactional
    public Driver reject(Long id, String reason) {
        validateRejectionReason(reason);
        Driver driver = findByIdForUpdate(id);
        driver.reject(reason.trim());
        return driver;
    }

    @Transactional
    public void changeTemporaryPassword(Driver authenticatedDriver, String password) {
        if (password == null || password.length() < 12) {
            throw new IllegalArgumentException("Choose a password with at least 12 characters.");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Password must not exceed 72 UTF-8 bytes.");
        }
        Driver driver = findByIdForUpdate(authenticatedDriver.getId());
        driver.changePassword(passwordEncoder.encode(password));
        driverRepository.saveAndFlush(driver);
        passwordResetTokenRepository.deleteByDriverId(driver.getId());
    }

    @Transactional
    public void updateInternalNotes(Long id, String notes) {
        if (notes != null && notes.length() > 4000) {
            throw new IllegalArgumentException("Internal notes must be 4000 characters or fewer.");
        }
        findByIdForUpdate(id).updateInternalNotes(notes == null || notes.isBlank() ? null : notes.trim());
    }

    @Transactional
    public void setSuspended(Long id, boolean suspended) {
        findByIdForUpdate(id).setSuspended(suspended);
    }

    @Transactional
    public void delete(Long id) {
        Driver driver = findById(id);
        if (vehicleRepository.existsByDriverId(id)) {
            throw new IllegalStateException("Reassign or remove this driver's vehicles before deleting the driver.");
        }
        driverRepository.delete(driver);
    }

    @Transactional(readOnly = true)
    public long countPending() {
        return driverRepository.countByApplicationStatus(DriverApplicationStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public long countApproved() {
        return driverRepository.countByApprovedTrue();
    }

    @Transactional(readOnly = true)
    public long countOnline() {
        return driverRepository.countByApprovedTrueAndOnlineTrue();
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private Driver findByIdForUpdate(Long id) {
        return driverRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new IllegalArgumentException("Driver was not found."));
    }

    private void validateRejectionReason(String reason) {
        if (reason == null || reason.isBlank() || reason.trim().length() > 1000) {
            throw new IllegalArgumentException("Enter a reason between 1 and 1000 characters.");
        }
    }

    private String normalizeLicense(String licenseNumber) {
        return licenseNumber.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizePlate(String plateNumber) {
        return plateNumber.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeNic(String nicNumber) {
        return nicNumber.trim().toUpperCase(Locale.ROOT);
    }

    private void validateUnique(String email, String licenseNumber, Long currentDriverId) {
        validateUniqueEmail(email, currentDriverId);
        boolean licenseUsed = currentDriverId == null
                ? driverRepository.existsByLicenseNumberIgnoreCase(licenseNumber)
                : driverRepository.existsByLicenseNumberIgnoreCaseAndIdNot(licenseNumber, currentDriverId);
        if (licenseUsed) {
            throw new IllegalArgumentException("A driver application already uses this licence number.");
        }
    }

    private void validateUniqueEmail(String email, Long currentDriverId) {
        boolean emailUsed = passengerRepository.existsByEmailIgnoreCase(email)
                || administratorRepository.existsByEmailIgnoreCase(email)
                || (currentDriverId == null
                ? driverRepository.existsByEmailIgnoreCase(email)
                : driverRepository.existsByEmailIgnoreCaseAndIdNot(email, currentDriverId));
        if (emailUsed) {
            throw new IllegalArgumentException("An account already exists for that email address.");
        }
    }
}
