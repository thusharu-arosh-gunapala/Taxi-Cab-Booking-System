package com.serendib.taxibooking.service;

import com.serendib.taxibooking.entity.Administrator;
import com.serendib.taxibooking.repository.AdministratorRepository;
import com.serendib.taxibooking.repository.PassengerRepository;
import com.serendib.taxibooking.repository.DriverRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class AdminBootstrapService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final AdministratorRepository administratorRepository;
    private final PassengerRepository passengerRepository;
    private final DriverRepository driverRepository;
    private final PasswordEncoder passwordEncoder;
    private final String administratorName;
    private final String administratorEmail;
    private final String administratorPassword;

    public AdminBootstrapService(AdministratorRepository administratorRepository,
                                 PassengerRepository passengerRepository,
                                 DriverRepository driverRepository,
                                 PasswordEncoder passwordEncoder,
                                 @Value("${app.admin.name:Site Administrator}") String administratorName,
                                 @Value("${app.admin.email:}") String administratorEmail,
                                 @Value("${app.admin.password:}") String administratorPassword) {
        this.administratorRepository = administratorRepository;
        this.passengerRepository = passengerRepository;
        this.driverRepository = driverRepository;
        this.passwordEncoder = passwordEncoder;
        this.administratorName = administratorName.trim();
        this.administratorEmail = administratorEmail.trim().toLowerCase(Locale.ROOT);
        this.administratorPassword = administratorPassword;
    }

    @Transactional
    public boolean provisionFirstAdministrator() {
        if (administratorRepository.count() > 0) {
            return false;
        }
        if (administratorEmail.isBlank() && administratorPassword.isBlank()) {
            return false;
        }
        if (administratorEmail.isBlank() || administratorPassword.isBlank()) {
            throw new IllegalStateException("Set both ADMIN_EMAIL and ADMIN_PASSWORD to create the first administrator.");
        }
        if (administratorName.isBlank() || administratorName.length() > 120) {
            throw new IllegalStateException("ADMIN_NAME must contain between 1 and 120 characters.");
        }
        try {
            validateCredentials(administratorEmail, administratorPassword);
            saveAdministrator(administratorName, administratorEmail, administratorPassword);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(exception.getMessage(), exception);
        }
        return true;
    }

    @Transactional
    public synchronized void createFirstAdministrator(String name, String email, String password) {
        if (administratorRepository.count() > 0) {
            throw new IllegalStateException("An administrator already exists. Use the existing sign-in page.");
        }
        String normalizedName = name == null ? "" : name.trim();
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (normalizedName.isBlank() || normalizedName.length() > 120) {
            throw new IllegalArgumentException("Enter a name between 1 and 120 characters.");
        }
        validateCredentials(normalizedEmail, password);
        saveAdministrator(normalizedName, normalizedEmail, password);
    }

    private void saveAdministrator(String name, String email, String password) {
        if (passengerRepository.existsByEmailIgnoreCase(email)
                || driverRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("That email already belongs to another account. Use a different email.");
        }
        administratorRepository.saveAndFlush(new Administrator(
                name,
                email,
                passwordEncoder.encode(password)));
    }

    private void validateCredentials(String email, String password) {
        if (email == null || email.length() > 254 || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("Enter a valid email address no longer than 254 characters.");
        }
        int passwordByteLength = password == null ? 0 : password.getBytes(StandardCharsets.UTF_8).length;
        if (passwordByteLength < 8 || passwordByteLength > 72) {
            throw new IllegalArgumentException("Password must be between 8 and 72 UTF-8 bytes.");
        }
    }
}
