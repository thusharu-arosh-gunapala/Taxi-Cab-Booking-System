package com.taxicab.controller;

import com.taxicab.dto.location.LocationResponse;
import com.taxicab.dto.location.LocationSuggestionResponse;
import com.taxicab.service.CustomerLocationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/customer")
public class CustomerLocationController {

    private final CustomerLocationService customerLocationService;

    public CustomerLocationController(CustomerLocationService customerLocationService) {
        this.customerLocationService = customerLocationService;
    }

    @GetMapping("/locations/search")
    public ResponseEntity<List<LocationSuggestionResponse>> search(@RequestParam("q") String query) {
        return ResponseEntity.ok(customerLocationService.searchLocations(query));
    }

    @GetMapping("/locations/reverse")
    public ResponseEntity<LocationResponse> reverseGeocode(@RequestParam double lat, @RequestParam double lon) {
        return ResponseEntity.ok(customerLocationService.reverseGeocode(new com.taxicab.dto.location.LocationRequest(lat, lon, null)));
    }
}
