package com.serendib.taxibooking.service;

import com.serendib.taxibooking.entity.Passenger;
import com.serendib.taxibooking.entity.Administrator;
import com.serendib.taxibooking.repository.AdministratorRepository;
import com.serendib.taxibooking.repository.PassengerRepository;
import com.serendib.taxibooking.repository.DriverRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class PassengerDetailsService implements UserDetailsService {

    private final PassengerRepository passengerRepository;
    private final AdministratorRepository administratorRepository;
    private final DriverRepository driverRepository;

    public PassengerDetailsService(PassengerRepository passengerRepository,
                                   AdministratorRepository administratorRepository,
                                   DriverRepository driverRepository) {
        this.passengerRepository = passengerRepository;
        this.administratorRepository = administratorRepository;
        this.driverRepository = driverRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        var administrator = administratorRepository.findByEmailIgnoreCase(username);
        if (administrator.isPresent()) {
            return administrator.get();
        }
        var driver = driverRepository.findByEmailIgnoreCase(username);
        if (driver.isPresent()) {
            return driver.get();
        }
        return passengerRepository.findByEmailIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("No account found for that email."));
    }
}
