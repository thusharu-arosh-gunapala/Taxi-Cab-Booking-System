package com.taxibooking.payment.service;

import com.taxibooking.payment.model.Promotion;
import com.taxibooking.payment.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;

// Promo code validation, discount and save/toggle/delete
@Service
public class PromotionService {

    private PaymentRepository repository;

    @Autowired
    public PromotionService(PaymentRepository repository) {
        this.repository = repository;
    }

    public ArrayList<Promotion> getAllPromotions() {
        return repository.getAllPromotions();
    }

    // checks the code, active flag, expiry date and minimum fare
    public Promotion validatePromotion(String promoCode, double fare) {
        if (promoCode == null || promoCode.trim().isEmpty()) {
            throw new PaymentService.PaymentException("Promo code cannot be empty.");
        }
        String code = promoCode.trim().toUpperCase();
        Promotion promo = findPromotion(repository.getAllPromotions(), code);

        if (promo == null) {
            throw new PaymentService.PaymentException("Promo code '" + code + "' does not exist.");
        }
        if (!promo.isActive()) {
            throw new PaymentService.PaymentException("Promo code '" + code + "' is currently deactivated.");
        }

        String expiry = promo.getExpiryDate();
        if (expiry != null && !expiry.trim().isEmpty()) {
            LocalDate expiryDate;
            try {
                expiryDate = LocalDate.parse(expiry.trim());
            } catch (Exception e) {
                throw new PaymentService.PaymentException("Promo code '" + code + "' has an invalid expiry date.");
            }
            if (LocalDate.now().isAfter(expiryDate)) {
                throw new PaymentService.PaymentException("Promo code '" + code + "' expired on " + expiry + ".");
            }
        }

        if (fare < promo.getMinimumFare()) {
            throw new PaymentService.PaymentException(String.format(
                    "Promo code '%s' requires a minimum fare of $%.2f. Current fare is $%.2f.",
                    code, promo.getMinimumFare(), fare));
        }
        return promo;
    }

    // discount amount, or 0 if the code is empty or not valid
    public double getDiscount(String promoCode, double fare) {
        if (promoCode == null || promoCode.trim().isEmpty()) {
            return 0.0;
        }
        try {
            return validatePromotion(promoCode, fare).calculateDiscount(fare);
        } catch (PaymentService.PaymentException e) {
            return 0.0;
        }
    }

    // adds a new promo code, or replaces the one with the same code
    public void savePromotion(Promotion promotion) {
        String code = "";
        if (promotion.getPromoCode() != null) {
            code = promotion.getPromoCode().trim().toUpperCase();
        }
        if (code.isEmpty()) {
            throw new PaymentService.PaymentException("Promo code cannot be empty.");
        }
        if (promotion.getDiscountPercentage() <= 0 || promotion.getDiscountPercentage() > 100) {
            throw new PaymentService.PaymentException("Discount percentage must be between 1% and 100%.");
        }
        if (promotion.getMinimumFare() < 0) {
            throw new PaymentService.PaymentException("Minimum fare cannot be negative.");
        }
        promotion.setPromoCode(code);

        ArrayList<Promotion> all = repository.getAllPromotions();
        Promotion old = findPromotion(all, code);
        if (old != null) {
            all.set(all.indexOf(old), promotion);
        } else {
            all.add(promotion);
        }
        repository.saveAllPromotions(all);
    }

    // switch ACTIVE / INACTIVE
    public Promotion toggleStatus(String promoCode) {
        ArrayList<Promotion> all = repository.getAllPromotions();
        Promotion promo = findPromotion(all, promoCode);
        if (promo == null) {
            throw new PaymentService.PaymentException("Promotion '" + promoCode + "' not found.");
        }
        promo.setActive(!promo.isActive());
        repository.saveAllPromotions(all);
        return promo;
    }

    public void deletePromotion(String promoCode) {
        ArrayList<Promotion> all = repository.getAllPromotions();
        Promotion promo = findPromotion(all, promoCode);
        if (promo == null) {
            throw new PaymentService.PaymentException("Cannot delete: Promotion '" + promoCode + "' does not exist.");
        }
        all.remove(promo);
        repository.saveAllPromotions(all);
    }

    private Promotion findPromotion(ArrayList<Promotion> list, String promoCode) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getPromoCode().equalsIgnoreCase(promoCode.trim())) {
                return list.get(i);
            }
        }
        return null;
    }
}
