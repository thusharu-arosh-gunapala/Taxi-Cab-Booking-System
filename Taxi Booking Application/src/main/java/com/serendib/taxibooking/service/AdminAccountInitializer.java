package com.serendib.taxibooking.service;

import com.serendib.taxibooking.repository.AdministratorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class AdminAccountInitializer implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(AdminAccountInitializer.class);

    private final AdminBootstrapService adminBootstrapService;
    private final AdministratorRepository administratorRepository;

    public AdminAccountInitializer(AdminBootstrapService adminBootstrapService,
                                   AdministratorRepository administratorRepository) {
        this.adminBootstrapService = adminBootstrapService;
        this.administratorRepository = administratorRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (adminBootstrapService.provisionFirstAdministrator()) {
            logger.info("Created the first administrator account in the configured database.");
        } else if (administratorRepository.count() == 0) {
            logger.warn("No administrator account exists. Open /admin/setup locally to create the first administrator.");
        }
    }
}
