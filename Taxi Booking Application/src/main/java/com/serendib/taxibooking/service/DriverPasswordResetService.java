package com.serendib.taxibooking.service;

import com.serendib.taxibooking.entity.Driver;
import com.serendib.taxibooking.entity.DriverPasswordResetToken;
import com.serendib.taxibooking.repository.DriverPasswordResetTokenRepository;
import com.serendib.taxibooking.repository.DriverRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

@Service
public class DriverPasswordResetService {

    private static final Logger logger = LoggerFactory.getLogger(DriverPasswordResetService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Duration TOKEN_LIFETIME = Duration.ofMinutes(30);

    private final DriverRepository driverRepository;
    private final DriverPasswordResetTokenRepository tokenRepository;
    private final DriverEmailService emailService;
    private final PasswordEncoder passwordEncoder;

    public DriverPasswordResetService(DriverRepository driverRepository,
                                      DriverPasswordResetTokenRepository tokenRepository,
                                      DriverEmailService emailService,
                                      PasswordEncoder passwordEncoder) {
        this.driverRepository = driverRepository;
        this.tokenRepository = tokenRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void requestReset(String email) {
        if (email == null || email.isBlank()) {
            return;
        }
        Driver driver = driverRepository.findByEmailIgnoreCase(email.trim().toLowerCase(Locale.ROOT))
                .filter(account -> account.isApproved() && account.isAccountActive())
                .orElse(null);
        if (driver == null) {
            return;
        }
        Driver lockedDriver = driverRepository.findByIdForUpdate(driver.getId())
                .orElseThrow(() -> new IllegalArgumentException("Driver was not found."));
        if (!lockedDriver.isApproved() || !lockedDriver.isAccountActive()) {
            return;
        }

        String rawToken = createToken();
        tokenRepository.deleteByDriverId(lockedDriver.getId());
        tokenRepository.saveAndFlush(new DriverPasswordResetToken(
                lockedDriver, hashToken(rawToken), Instant.now().plus(TOKEN_LIFETIME)));
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("Driver password reset email requires a transaction.");
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    DriverEmailService.DeliveryResult result = emailService.sendPasswordReset(lockedDriver, rawToken);
                    if (result != DriverEmailService.DeliveryResult.SENT) {
                        logger.warn("Driver password reset email was not sent for driver reference {}: {}",
                                lockedDriver.getDriverReference(), result);
                    }
                } catch (RuntimeException ignored) {
                    logger.warn("Driver password reset email failed for driver reference {}",
                            lockedDriver.getDriverReference());
                }
            }
        });
    }

    @Transactional(readOnly = true)
    public boolean isTokenValid(String rawToken) {
        if (rawToken == null || rawToken.isBlank() || rawToken.length() > 128) {
            return false;
        }
        return tokenRepository.findValidToken(hashToken(rawToken), Instant.now()).isPresent();
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        if (rawToken == null || rawToken.isBlank() || rawToken.length() > 128) {
            throw new IllegalArgumentException("The password reset link is invalid or expired.");
        }
        if (newPassword == null || newPassword.length() < 12
                || newPassword.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Choose a password with 12 to 72 UTF-8 bytes.");
        }
        String tokenHash = hashToken(rawToken);
        DriverPasswordResetToken candidate = tokenRepository.findValidToken(tokenHash, Instant.now())
                .orElseThrow(() -> new IllegalArgumentException("The password reset link is invalid or expired."));
        Long driverId = candidate.getDriver().getId();
        Driver driver = driverRepository.findByIdForUpdate(driverId)
                .orElseThrow(() -> new IllegalArgumentException("The password reset link is invalid or expired."));
        tokenRepository.findValidTokenForUpdate(tokenHash, Instant.now())
                .filter(token -> token.getDriver().getId().equals(driverId))
                .orElseThrow(() -> new IllegalArgumentException("The password reset link is invalid or expired."));
        driver.changePassword(passwordEncoder.encode(newPassword));
        tokenRepository.deleteByDriverId(driver.getId());
        driverRepository.saveAndFlush(driver);
    }

    private String createToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String rawToken) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }
}
