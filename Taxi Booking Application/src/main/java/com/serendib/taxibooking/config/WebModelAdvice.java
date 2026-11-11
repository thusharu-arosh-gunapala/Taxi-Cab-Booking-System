package com.serendib.taxibooking.config;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(annotations = Controller.class)
public class WebModelAdvice {

    @ModelAttribute
    public void addSignedInState(Authentication authentication, Model model) {
        boolean signedIn = authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
        model.addAttribute("signedIn", signedIn);
        model.addAttribute("admin", signedIn
                && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority())));
        model.addAttribute("driver", signedIn
                && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_DRIVER".equals(authority.getAuthority())));
        if (signedIn) {
            model.addAttribute("currentUser", authentication.getPrincipal() instanceof com.serendib.taxibooking.entity.Administrator administrator
                    ? administrator.getFullName()
                    : authentication.getPrincipal() instanceof com.serendib.taxibooking.entity.Driver driver
                    ? driver.getName()
                    : authentication.getName());
        }
    }
}
