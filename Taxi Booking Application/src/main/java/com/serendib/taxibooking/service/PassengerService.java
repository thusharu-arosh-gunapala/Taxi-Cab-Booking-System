package com.serendib.taxibooking.service;

import com.serendib.taxibooking.dto.PassengerRegistrationForm;
import com.serendib.taxibooking.entity.Passenger;
import com.serendib.taxibooking.repository.PassengerRepository;
import com.serendib.taxibooking.repository.AdministratorRepository;
import com.serendib.taxibooking.repository.DriverRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class PassengerService {

    private final PassengerRepository passengerRepository;
    private final AdministratorRepository administratorRepository;
    private final DriverRepository driverRepository;
    private final PasswordEncoder passwordEncoder;

    public PassengerService(PassengerRepository passengerRepository,
                            AdministratorRepository administratorRepository,
                            DriverRepository driverRepository,
                            PasswordEncoder passwordEncoder) {
        this.passengerRepository = passengerRepository;
        this.administratorRepository = administratorRepository;
        this.driverRepository = driverRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Passenger register(PassengerRegistrationForm form) {
        if (!form.getPassword().equals(form.getConfirmPassword())) {
            throw new IllegalArgumentException("The passwords do not match.");
        }

        String email = form.getEmail().trim().toLowerCase(Locale.ROOT);
        if (passengerRepository.existsByEmailIgnoreCase(email)
                || administratorRepository.existsByEmailIgnoreCase(email)
                || driverRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("An account already exists for that email address.");
        }

        Passenger passenger = new Passenger(
                form.getFullName().trim(),
                email,
                form.getPhone().trim(),
                form.getAddress().trim(),
                passwordEncoder.encode(form.getPassword()));
        return passengerRepository.save(passenger);
    }

    @Transactional(readOnly = true)
    public Passenger findByEmail(String email) {
        return passengerRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("Passenger account was not found."));
    }
}
