package com.serendib.taxibooking.controller;

import com.serendib.taxibooking.dto.FareEstimateRequest;
import com.serendib.taxibooking.dto.FareEstimateResponse;
import com.serendib.taxibooking.service.FareEstimateService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fares")
public class FareController {

    private final FareEstimateService fareEstimateService;

    public FareController(FareEstimateService fareEstimateService) {
        this.fareEstimateService = fareEstimateService;
    }

    @PostMapping("/estimate")
    public FareEstimateResponse estimate(@Valid @RequestBody FareEstimateRequest request) {
        return fareEstimateService.estimate(request);
    }
}
