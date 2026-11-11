package com.serendib.taxibooking.service;

import com.serendib.taxibooking.dto.AdministratorForm;
import com.serendib.taxibooking.entity.Administrator;
import com.serendib.taxibooking.repository.AdministratorRepository;
import com.serendib.taxibooking.repository.PassengerRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminOperationsService {

    private final AdministratorRepository administratorRepository;
    private final PassengerRepository passengerRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminOperationsService(AdministratorRepository administratorRepository,
                                  PassengerRepository passengerRepository,
                                  PasswordEncoder passwordEncoder) {
        this.administratorRepository = administratorRepository;
        this.passengerRepository = passengerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Administrator createAdministrator(AdministratorForm form) {
        String email = form.getEmail().trim().toLowerCase(java.util.Locale.ROOT);
        if (passengerRepository.existsByEmailIgnoreCase(email)
                || administratorRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("That email already belongs to an account.");
        }
        return administratorRepository.save(new Administrator(
                form.getFullName().trim(), email, passwordEncoder.encode(form.getPassword())));
    }

    @Transactional
    public void removeAdministrator(Long administratorId, String currentAdminEmail) {
        Administrator administrator = administratorRepository.findById(administratorId)
                .orElseThrow(() -> new IllegalArgumentException("Administrator account was not found."));
        if (administrator.getEmail().equalsIgnoreCase(currentAdminEmail)) {
            throw new IllegalStateException("You cannot remove your own administrator account.");
        }
        if (administratorRepository.count() <= 1) {
            throw new IllegalStateException("The site must keep at least one administrator.");
        }
        administratorRepository.delete(administrator);
    }
}
