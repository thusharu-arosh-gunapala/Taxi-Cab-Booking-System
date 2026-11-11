package com.serendib.taxibooking.service;

import com.serendib.taxibooking.entity.Administrator;
import com.serendib.taxibooking.repository.AdministratorRepository;
import com.serendib.taxibooking.repository.PassengerRepository;
import com.serendib.taxibooking.repository.DriverRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminBootstrapServiceTest {

    @Test
    void provisionsConfiguredFirstAdminWithEncodedPassword() {
        AdministratorRepository administratorRepository = mock(AdministratorRepository.class);
        PassengerRepository passengerRepository = mock(PassengerRepository.class);
        DriverRepository driverRepository = mock(DriverRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        when(administratorRepository.count()).thenReturn(0L);
        when(passengerRepository.existsByEmailIgnoreCase("owner@example.test")).thenReturn(false);
        when(passwordEncoder.encode("strong-password")).thenReturn("bcrypt-hash");
        when(administratorRepository.saveAndFlush(any(Administrator.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        AdminBootstrapService service = service(
                administratorRepository, passengerRepository, driverRepository, passwordEncoder,
                "owner@example.test", "strong-password");

        assertThat(service.provisionFirstAdministrator()).isTrue();

        var administrator = org.mockito.ArgumentCaptor.forClass(Administrator.class);
        verify(administratorRepository).saveAndFlush(administrator.capture());
        assertThat(administrator.getValue().getFullName()).isEqualTo("Site Administrator");
        assertThat(administrator.getValue().getEmail()).isEqualTo("owner@example.test");
        assertThat(administrator.getValue().getPassword()).isEqualTo("bcrypt-hash");
        assertThat(administrator.getValue().getAuthorities()).extracting(Object::toString)
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    void doesNotProvisionWhenCredentialsAreNotConfiguredOrAnAdminAlreadyExists() {
        AdministratorRepository administratorRepository = mock(AdministratorRepository.class);
        PassengerRepository passengerRepository = mock(PassengerRepository.class);
        DriverRepository driverRepository = mock(DriverRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        AdminBootstrapService service = service(
                administratorRepository, passengerRepository, driverRepository, passwordEncoder, "", "");
        when(administratorRepository.count()).thenReturn(0L);

        assertThat(service.provisionFirstAdministrator()).isFalse();

        AdminBootstrapService configuredService = service(
                administratorRepository, passengerRepository, driverRepository, passwordEncoder,
                "owner@example.test", "strong-password");
        when(administratorRepository.count()).thenReturn(1L);
        assertThat(configuredService.provisionFirstAdministrator()).isFalse();
    }

    @Test
    void rejectsIncompleteOrInvalidCredentialsAndPassengerEmail() {
        AdministratorRepository administratorRepository = mock(AdministratorRepository.class);
        PassengerRepository passengerRepository = mock(PassengerRepository.class);
        DriverRepository driverRepository = mock(DriverRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        when(administratorRepository.count()).thenReturn(0L);

        assertThatThrownBy(() -> service(administratorRepository, passengerRepository, driverRepository, passwordEncoder,
                "owner@example.test", "").provisionFirstAdministrator())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Set both ADMIN_EMAIL and ADMIN_PASSWORD");
        assertThatThrownBy(() -> service(administratorRepository, passengerRepository, driverRepository, passwordEncoder,
                "not-an-email", "strong-password").provisionFirstAdministrator())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("valid email address");
        assertThatThrownBy(() -> service(administratorRepository, passengerRepository, driverRepository, passwordEncoder,
                "owner@example.test", "short").provisionFirstAdministrator())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Password must be between");

        when(passengerRepository.existsByEmailIgnoreCase("owner@example.test")).thenReturn(true);
        assertThatThrownBy(() -> service(administratorRepository, passengerRepository, driverRepository, passwordEncoder,
                "owner@example.test", "strong-password").provisionFirstAdministrator())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already belongs to another account");
    }

    private AdminBootstrapService service(AdministratorRepository administratorRepository,
                                          PassengerRepository passengerRepository,
                                          DriverRepository driverRepository,
                                          PasswordEncoder passwordEncoder,
                                          String email,
                                          String password) {
        return new AdminBootstrapService(
                administratorRepository, passengerRepository, driverRepository, passwordEncoder,
                "Site Administrator", email, password);
    }
}
