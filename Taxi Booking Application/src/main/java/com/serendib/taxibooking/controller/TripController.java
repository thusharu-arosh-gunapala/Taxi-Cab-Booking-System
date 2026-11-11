package com.serendib.taxibooking.controller;

import com.serendib.taxibooking.entity.Passenger;
import com.serendib.taxibooking.service.TripService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class TripController {

    private final TripService tripService;

    public TripController(TripService tripService) {
        this.tripService = tripService;
    }

    @GetMapping("/trips")
    public String trips(@AuthenticationPrincipal Passenger passenger, Model model) {
        model.addAttribute("trips", tripService.findForPassenger(passenger));
        return "trip/list";
    }
}
