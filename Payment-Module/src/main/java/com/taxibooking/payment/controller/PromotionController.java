package com.taxibooking.payment.controller;

import com.taxibooking.payment.model.Promotion;
import com.taxibooking.payment.service.PaymentService;
import com.taxibooking.payment.service.PromotionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;

// Promotions page and the live promo check used by payment.js
@Controller
public class PromotionController {

    private PromotionService promotionService;

    @Autowired
    public PromotionController(PromotionService promotionService) {
        this.promotionService = promotionService;
    }

    @GetMapping("/promotions")
    public String listPromotions(Model model) {
        model.addAttribute("promotions", promotionService.getAllPromotions());
        model.addAttribute("newPromotion", new Promotion());
        return "payment/promotions";
    }

    @PostMapping("/promotions/save")
    public String savePromotion(@ModelAttribute("newPromotion") Promotion promotion,
                                RedirectAttributes redirectAttributes) {
        try {
            promotionService.savePromotion(promotion);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Promo code '" + promotion.getPromoCode() + "' saved successfully!");
        } catch (PaymentService.PaymentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/promotions";
    }

    @GetMapping("/promotions/toggle/{promoCode}")
    public String togglePromotion(@PathVariable("promoCode") String promoCode,
                                  RedirectAttributes redirectAttributes) {
        try {
            Promotion promo = promotionService.toggleStatus(promoCode);
            String state = "INACTIVE";
            if (promo.isActive()) {
                state = "ACTIVE";
            }
            redirectAttributes.addFlashAttribute("successMessage", "Promo code '" + promoCode + "' is now " + state);
        } catch (PaymentService.PaymentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/promotions";
    }

    @PostMapping("/promotions/delete/{promoCode}")
    public String deletePromotion(@PathVariable("promoCode") String promoCode,
                                  RedirectAttributes redirectAttributes) {
        try {
            promotionService.deletePromotion(promoCode);
            redirectAttributes.addFlashAttribute("successMessage", "Promo code '" + promoCode + "' deleted.");
        } catch (PaymentService.PaymentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/promotions";
    }

    // returns JSON for payment.js
    @GetMapping("/api/promotions/validate")
    @ResponseBody
    public HashMap<String, Object> validatePromoCode(@RequestParam("code") String code,
                                                     @RequestParam("fare") double fare) {
        HashMap<String, Object> response = new HashMap<String, Object>();
        try {
            Promotion promo = promotionService.validatePromotion(code, fare);
            double discount = promo.calculateDiscount(fare);
            response.put("valid", true);
            response.put("code", promo.getPromoCode());
            response.put("discountPercentage", promo.getDiscountPercentage());
            response.put("discountAmount", discount);
            response.put("finalAmount", fare - discount);
            response.put("message", String.format("Promo applied! %.0f%% off (Save $%.2f)",
                    promo.getDiscountPercentage(), discount));
        } catch (PaymentService.PaymentException e) {
            response.put("valid", false);
            response.put("message", e.getMessage());
        }
        return response;
    }
}
