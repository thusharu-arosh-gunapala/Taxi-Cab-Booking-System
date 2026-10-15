package com.taxibooking.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Starts the app at http://localhost:8080/payment
@SpringBootApplication
public class CabPaymentApplication {

    public static void main(String[] args) {
        SpringApplication.run(CabPaymentApplication.class, args);
    }
}
