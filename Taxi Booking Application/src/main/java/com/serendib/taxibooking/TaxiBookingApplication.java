package com.serendib.taxibooking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TaxiBookingApplication {

    public static void main(String[] args) {
        SpringApplication.run(TaxiBookingApplication.class, args);
    }
}
