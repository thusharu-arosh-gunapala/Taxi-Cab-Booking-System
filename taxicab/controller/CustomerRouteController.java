package com.taxicab.controller;

import com.taxicab.dto.route.RouteRequest;
import com.taxicab.dto.route.RouteResponse;
import com.taxicab.service.CustomerRouteService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customer")
public class CustomerRouteController {

    private final CustomerRouteService customerRouteService;

    public CustomerRouteController(CustomerRouteService customerRouteService) {
        this.customerRouteService = customerRouteService;
    }

    @PostMapping("/routes")
    public ResponseEntity<RouteResponse> calculateRoute(@Valid @RequestBody RouteRequest request) {
        return ResponseEntity.ok(customerRouteService.calculate(request));
    }
}
