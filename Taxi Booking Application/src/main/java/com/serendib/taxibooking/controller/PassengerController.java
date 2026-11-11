package com.serendib.taxibooking.controller;

import com.serendib.taxibooking.dto.FavouriteLocationForm;
import com.serendib.taxibooking.entity.FavouriteLocation;
import com.serendib.taxibooking.entity.Passenger;
import com.serendib.taxibooking.service.FavouriteLocationService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PassengerController {

    private final FavouriteLocationService favouriteLocationService;

    public PassengerController(FavouriteLocationService favouriteLocationService) {
        this.favouriteLocationService = favouriteLocationService;
    }

    @GetMapping("/passenger/favourites")
    public String favourites(@AuthenticationPrincipal Passenger passenger, Model model) {
        model.addAttribute("locations", favouriteLocationService.findForPassenger(passenger));
        model.addAttribute("locationTypes", FavouriteLocation.Type.values());
        model.addAttribute("locationForm", new FavouriteLocationForm());
        return "passenger/favourites";
    }

    @PostMapping("/passenger/favourites")
    public String addFavourite(@AuthenticationPrincipal Passenger passenger,
                               @Valid @ModelAttribute("locationForm") FavouriteLocationForm form,
                               BindingResult bindingResult,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("locations", favouriteLocationService.findForPassenger(passenger));
            model.addAttribute("locationTypes", FavouriteLocation.Type.values());
            return "passenger/favourites";
        }
        try {
            favouriteLocationService.add(passenger, form);
        } catch (IllegalArgumentException exception) {
            bindingResult.rejectValue("type", "location.duplicate", exception.getMessage());
            model.addAttribute("locations", favouriteLocationService.findForPassenger(passenger));
            model.addAttribute("locationTypes", FavouriteLocation.Type.values());
            return "passenger/favourites";
        }
        redirectAttributes.addFlashAttribute("notice", "Favourite location saved.");
        return "redirect:/passenger/favourites";
    }

    @PostMapping("/passenger/favourites/{id}/delete")
    public String deleteFavourite(@PathVariable Long id,
                                  @AuthenticationPrincipal Passenger passenger,
                                  RedirectAttributes redirectAttributes) {
        try {
            favouriteLocationService.delete(id, passenger);
            redirectAttributes.addFlashAttribute("notice", "Favourite location deleted.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/passenger/favourites";
    }
}
