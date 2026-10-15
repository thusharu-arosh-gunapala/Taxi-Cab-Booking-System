package com.taxicab.controller;

import com.taxicab.dto.fare.FareRequest;
import com.taxicab.dto.fare.FareResponse;
import com.taxicab.service.CustomerFareService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customer")
public class CustomerFareController {

    private final CustomerFareService customerFareService;

    public CustomerFareController(CustomerFareService customerFareService) {
        this.customerFareService = customerFareService;
    }

    @PostMapping("/fares/estimate")
    public ResponseEntity<FareResponse> estimateFare(@Valid @RequestBody FareRequest request) {
        return ResponseEntity.ok(customerFareService.calculateFare(request));
    }
}
