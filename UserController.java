package com.taxibooking.controller; //UserController

import com.taxibooking.model.FavouriteLocation;
import com.taxibooking.model.Passenger;
import com.taxibooking.model.User;
import com.taxibooking.service.AuthenticationService;
import com.taxibooking.service.UserService;
import com.taxibooking.service.ValidationException;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;

/** Everything a logged-in passenger can do with their own account. */
@Controller
@RequestMapping("/passenger")
public class UserController {

    private static final String LOGIN = "redirect:/login";

    private final UserService userService;
    private final AuthenticationService authService;

    public UserController(UserService userService, AuthenticationService authService) {
        this.userService = userService;
        this.authService = authService;
    }

    /** Returns the logged-in ACTIVE passenger, or null (caller redirects to /login). */
    private Passenger current(HttpSession session) {
        Object id = session.getAttribute("userId");
        if (id == null) return null;
        return userService.findPassenger((String) id).filter(User::isActive).orElse(null);
    }

    // ---------- Dashboard ----------
    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        Passenger p = current(session);
        if (p == null) return LOGIN;
        model.addAttribute("user", p);
        model.addAttribute("favouriteCount", userService.getFavourites(p.getUserId()).size());
        return "passenger/passenger-dashboard";
    }

    // ---------- View profile ----------
    @GetMapping("/profile")
    public String profile(HttpSession session, Model model) {
        Passenger p = current(session);
        if (p == null) return LOGIN;
        model.addAttribute("user", p);
        return "passenger/profile";
    }

    // ---------- Update profile ----------
    @GetMapping("/profile/edit")
    public String editProfileForm(HttpSession session, Model model) {
        Passenger p = current(session);
        if (p == null) return LOGIN;
        Map<String, String> form = new HashMap<>();
        form.put("fullName", p.getFullName());
        form.put("phone", p.getPhone());
        form.put("address", p.getAddress());
        model.addAttribute("user", p);
        model.addAttribute("form", form);
        return "passenger/edit-profile";
    }

    @PostMapping("/profile/edit")
    public String editProfile(@RequestParam Map<String, String> params, HttpSession session,
                              Model model, RedirectAttributes ra) {
        Passenger p = current(session);
        if (p == null) return LOGIN;
        try {
            userService.updateProfile(p.getUserId(), params);
            ra.addFlashAttribute("success", "Profile updated.");
            return "redirect:/passenger/profile";
        } catch (ValidationException e) {
            model.addAttribute("user", p);
            model.addAttribute("form", params);
            model.addAttribute("errors", e.getErrors());
            return "passenger/edit-profile";
        }
    }

    // ---------- Change password ----------
    @GetMapping("/change-password")
    public String changePasswordForm(HttpSession session, Model model) {
        Passenger p = current(session);
        if (p == null) return LOGIN;
        model.addAttribute("user", p);
        return "passenger/change-password";
    }

    @PostMapping("/change-password")
    public String changePassword(@RequestParam String currentPassword, @RequestParam String newPassword,
                                 @RequestParam String confirmPassword, HttpSession session,
                                 Model model, RedirectAttributes ra) {
        Passenger p = current(session);
        if (p == null) return LOGIN;
        try {
            authService.changePassword(p.getUserId(), currentPassword, newPassword, confirmPassword);
            ra.addFlashAttribute("success", "Password changed successfully.");
            return "redirect:/passenger/profile";
        } catch (ValidationException e) {
            model.addAttribute("user", p);
            model.addAttribute("errors", e.getErrors());
            return "passenger/change-password";
        }
    }

    // ---------- Deactivate account ----------
    @PostMapping("/deactivate")
    public String deactivate(@RequestParam String password, HttpSession session, RedirectAttributes ra) {
        Passenger p = current(session);
        if (p == null) return LOGIN;
        try {
            userService.deactivate(p.getUserId(), password);
            session.invalidate();
            ra.addFlashAttribute("success", "Your account has been deactivated.");
            return LOGIN;
        } catch (ValidationException e) {
            ra.addFlashAttribute("errors", e.getErrors());
            return "redirect:/passenger/profile";
        }
    }

    // ---------- Favourite locations (list / add / edit / delete) ----------
    @GetMapping("/favourites")
    public String favourites(@RequestParam(required = false) String edit, HttpSession session, Model model) {
        Passenger p = current(session);
        if (p == null) return LOGIN;
        model.addAttribute("user", p);
        model.addAttribute("locations", userService.getFavourites(p.getUserId()));
        model.addAttribute("types", FavouriteLocation.Type.values());

        FavouriteLocation editing = edit == null ? null : userService.getFavourite(p.getUserId(), edit).orElse(null);
        model.addAttribute("editing", editing);
        return "passenger/favourite-locations";
    }

    @PostMapping("/favourites")
    public String addFavourite(@RequestParam Map<String, String> params, HttpSession session, RedirectAttributes ra) {
        Passenger p = current(session);
        if (p == null) return LOGIN;
        try {
            userService.addFavourite(p.getUserId(), params);
            ra.addFlashAttribute("success", "Location saved.");
        } catch (ValidationException e) {
            ra.addFlashAttribute("errors", e.getErrors());
        }
        return "redirect:/passenger/favourites";
    }

    @PostMapping("/favourites/{id}/update")
    public String updateFavourite(@PathVariable String id, @RequestParam Map<String, String> params,
                                  HttpSession session, RedirectAttributes ra) {
        Passenger p = current(session);
        if (p == null) return LOGIN;
        try {
            userService.updateFavourite(p.getUserId(), id, params);
            ra.addFlashAttribute("success", "Location updated.");
            return "redirect:/passenger/favourites";
        } catch (ValidationException e) {
            ra.addFlashAttribute("errors", e.getErrors());
            return "redirect:/passenger/favourites?edit=" + id;
        }
    }

    @PostMapping("/favourites/{id}/delete")
    public String deleteFavourite(@PathVariable String id, HttpSession session, RedirectAttributes ra) {
        Passenger p = current(session);
        if (p == null) return LOGIN;
        try {
            userService.deleteFavourite(p.getUserId(), id);
            ra.addFlashAttribute("success", "Location deleted.");
        } catch (ValidationException e) {
            ra.addFlashAttribute("errors", e.getErrors());
        }
        return "redirect:/passenger/favourites";
    }
}

