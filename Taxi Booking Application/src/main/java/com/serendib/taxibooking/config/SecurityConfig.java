package com.serendib.taxibooking.config;

import com.serendib.taxibooking.entity.Driver;
import com.serendib.taxibooking.service.PassengerDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain applicationSecurity(HttpSecurity http,
                                           PassengerDetailsService passengerDetailsService,
                                           AuthenticationSuccessHandler authenticationSuccessHandler) throws Exception {
        return http
                .userDetailsService(passengerDetailsService)
                .csrf(csrf -> csrf.ignoringRequestMatchers(
                        "/api/fares/**", "/api/maps/**", "/api/integrations/**",
                        "/api/payments/stripe/webhook"))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/maps/search", "/api/maps/drivers", "/api/maps/tiles/**")
                        .hasRole("PASSENGER")
                        .requestMatchers("/api/email/test").hasRole("ADMIN")
                        .requestMatchers("/", "/login", "/admin/login", "/admin/setup",
                                "/register", "/css/**", "/js/**",
                                "/driver/register", "/driver/login", "/driver/forgot-password",
                                "/driver/reset-password",
                                "/api/fares/**", "/api/maps/**", "/api/integrations/**",
                                "/api/payments/stripe/webhook", "/error")
                        .permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/driver/application", "/driver/application/**", "/driver/status")
                        .hasRole("DRIVER")
                        .requestMatchers("/driver/**").access((authentication, context) -> {
                            var currentAuthentication = authentication.get();
                            if (!(currentAuthentication.getPrincipal() instanceof Driver driver)) {
                                return new AuthorizationDecision(false);
                            }
                            return new AuthorizationDecision(driver.isApproved()
                                    && driver.isAccountActive()
                                    && !driver.isSuspended()
                                    && driver.getApplicationStatus()
                                    == com.serendib.taxibooking.entity.DriverApplicationStatus.APPROVED);
                        })
                        .requestMatchers("/bookings/**", "/passenger/**", "/trips/**",
                                "/payments/**", "/refunds/**").hasRole("PASSENGER")
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .successHandler(authenticationSuccessHandler)
                        .failureHandler((request, response, exception) -> {
                            response.sendRedirect("/login?error");
                        })
                        .permitAll())
                .logout(logout -> logout
                        .logoutSuccessUrl("/?logout")
                        .permitAll())
                .build();
    }

    @Bean
    AuthenticationSuccessHandler authenticationSuccessHandler() {
        return (request, response, authentication) -> {
            boolean admin = authentication.getAuthorities().stream()
                    .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
            boolean driver = authentication.getAuthorities().stream()
                    .anyMatch(authority -> "ROLE_DRIVER".equals(authority.getAuthority()));
            boolean mustChangePassword = authentication.getPrincipal() instanceof Driver driverDetails
                    && driverDetails.isMustChangePassword();
            if (admin) {
                response.sendRedirect("/admin");
            } else if (driver && authentication.getPrincipal() instanceof Driver driverDetails) {
                String destination = switch (driverDetails.getApplicationStatus()) {
                    case NOT_SUBMITTED, RESUBMISSION_REQUIRED, REJECTED -> "/driver/application";
                    case PENDING -> "/driver/status";
                    case APPROVED -> driverDetails.isSuspended()
                            ? "/driver/status?suspended" : "/driver/dashboard";
                };
                response.sendRedirect(destination);
            } else {
                response.sendRedirect("/bookings");
            }
        };
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}
