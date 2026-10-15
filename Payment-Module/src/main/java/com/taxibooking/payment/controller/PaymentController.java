package com.taxibooking.payment.controller;

import com.taxibooking.payment.model.Payment;
import com.taxibooking.payment.model.Promotion;
import com.taxibooking.payment.model.Receipt;
import com.taxibooking.payment.model.Refund;
import com.taxibooking.payment.service.PaymentService;
import com.taxibooking.payment.service.PromotionService;
import com.taxibooking.payment.service.ReceiptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.HashMap;

// Pages: payment, history, receipts and refunds
@Controller
public class PaymentController {

    private PaymentService paymentService;
    private PromotionService promotionService;
    private ReceiptService receiptService;

    @Autowired
    public PaymentController(PaymentService paymentService, PromotionService promotionService,
                             ReceiptService receiptService) {
        this.paymentService = paymentService;
        this.promotionService = promotionService;
        this.receiptService = receiptService;
    }

    @GetMapping("/")
    public String index() {
        return "redirect:/payment";
    }

    // ---------- Payment page ----------

    @GetMapping("/payment")
    public String showPaymentPage(
            @RequestParam(value = "bookingId", defaultValue = "BK-7788") String bookingId,
            @RequestParam(value = "passengerId", defaultValue = "PASS-405") String passengerId,
            @RequestParam(value = "driver", defaultValue = "David Miller") String driver,
            @RequestParam(value = "pickup", defaultValue = "Terminal 4, JFK Int'l Airport") String pickup,
            @RequestParam(value = "destination", defaultValue = "Times Square, New York, NY") String destination,
            @RequestParam(value = "distance", defaultValue = "26.4") double distance,
            @RequestParam(value = "baseFare", defaultValue = "55.00") double baseFare,
            @RequestParam(value = "promoCode", required = false) String promoCode,
            Model model) {

        // the demo booking is already paid, so use a new booking id
        if (bookingId.equals("BK-7788") && paymentService.hasPaidPaymentForBooking(bookingId)) {
            bookingId = paymentService.generateBookingId();
        }

        HashMap<String, Object> trip = new HashMap<String, Object>();
        trip.put("bookingId", bookingId);
        trip.put("passengerId", passengerId);
        trip.put("driver", driver);
        trip.put("pickup", pickup);
        trip.put("destination", destination);
        trip.put("distance", distance);
        trip.put("baseFare", baseFare);
        trip.put("promoCode", promoCode);

        double discount = 0.0;
        String promoMessage = null;
        boolean promoValid = false;
        if (promoCode != null && !promoCode.trim().isEmpty()) {
            try {
                Promotion promo = promotionService.validatePromotion(promoCode, baseFare);
                discount = promo.calculateDiscount(baseFare);
                promoValid = true;
                promoMessage = String.format("Promotion '%s' applied: %.0f%% off (-$%.2f)",
                        promo.getPromoCode(), promo.getDiscountPercentage(), discount);
            } catch (PaymentService.PaymentException e) {
                promoMessage = e.getMessage();
            }
        }

        model.addAttribute("trip", trip);
        model.addAttribute("discount", discount);
        model.addAttribute("finalAmount", baseFare - discount);
        model.addAttribute("promoMessage", promoMessage);
        model.addAttribute("promoValid", promoValid);
        model.addAttribute("activePromotions", promotionService.getAllPromotions());
        return "payment/payment";
    }

    @PostMapping("/payment/process")
    public String processPayment(
            @RequestParam("bookingId") String bookingId,
            @RequestParam("passengerId") String passengerId,
            @RequestParam("driver") String driver,
            @RequestParam("pickup") String pickup,
            @RequestParam("destination") String destination,
            @RequestParam("distance") double distance,
            @RequestParam("baseFare") double baseFare,
            @RequestParam(value = "promoCode", required = false) String promoCode,
            @RequestParam("paymentMethod") String paymentMethod,
            @RequestParam(value = "amountTendered", defaultValue = "0.0") double amountTendered,
            @RequestParam(value = "cardNumber", required = false) String cardNumber,
            @RequestParam(value = "cardHolderName", required = false) String cardHolderName,
            @RequestParam(value = "expiryDate", required = false) String expiryDate,
            @RequestParam(value = "cvv", required = false) String cvv,
            @RequestParam(value = "provider", required = false) String provider,
            @RequestParam(value = "transactionReference", required = false) String transactionReference,
            RedirectAttributes redirectAttributes) {

        // the discount is calculated again here, not taken from the page
        double discount = promotionService.getDiscount(promoCode, baseFare);
        double finalAmount = baseFare - discount;
        String paymentId = paymentService.generatePaymentId();

        // CashPayment, CardPayment or OnlinePayment (all are Payment)
        Payment payment;
        if (paymentMethod.equalsIgnoreCase("CASH")) {
            payment = new Payment.CashPayment(paymentId, bookingId, passengerId, finalAmount, amountTendered);
        } else if (paymentMethod.equalsIgnoreCase("CARD")) {
            payment = new Payment.CardPayment(paymentId, bookingId, passengerId, finalAmount,
                    cardNumber, cardHolderName, expiryDate, cvv);
        } else if (paymentMethod.equalsIgnoreCase("ONLINE")) {
            payment = new Payment.OnlinePayment(paymentId, bookingId, passengerId, finalAmount,
                    provider, transactionReference);
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Unknown payment method: " + paymentMethod);
            return "redirect:/payment";
        }

        try {
            Payment saved = paymentService.executePayment(payment);
            if (saved.getStatus().equals("PAID")) {
                Receipt receipt = receiptService.generateReceipt(saved, driver, pickup, destination,
                        distance, baseFare, discount);
                redirectAttributes.addFlashAttribute("successMessage",
                        "Payment of $" + String.format("%.2f", finalAmount) + " completed! Receipt generated.");
                return "redirect:/receipt/" + receipt.getReceiptId();
            }
            redirectAttributes.addFlashAttribute("errorMessage", saved.getNotes());
        } catch (PaymentService.PaymentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/payment?bookingId=" + bookingId + "&baseFare=" + baseFare;
    }

    // ---------- Payment history ----------

    @GetMapping("/payment/history")
    public String showPaymentHistory(
            @RequestParam(value = "status", defaultValue = "ALL") String status,
            @RequestParam(value = "method", defaultValue = "ALL") String method,
            Model model) {
        model.addAttribute("payments", paymentService.filterPayments(status, method));
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedMethod", method);
        return "payment/payment-history";
    }

    @PostMapping("/payment/status")
    public String updateStatus(@RequestParam("paymentId") String paymentId,
                               @RequestParam("status") String newStatus,
                               RedirectAttributes redirectAttributes) {
        try {
            paymentService.updatePaymentStatus(paymentId, newStatus);
            redirectAttributes.addFlashAttribute("successMessage", "Payment " + paymentId + " status changed to " + newStatus);
        } catch (PaymentService.PaymentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/payment/history";
    }

    @PostMapping("/payment/delete/{paymentId}")
    public String deletePayment(@PathVariable("paymentId") String paymentId,
                                RedirectAttributes redirectAttributes) {
        try {
            paymentService.deletePayment(paymentId);
            redirectAttributes.addFlashAttribute("successMessage", "Payment " + paymentId + " deleted successfully.");
        } catch (PaymentService.PaymentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/payment/history";
    }

    // ---------- Receipts ----------

    @GetMapping("/receipt/{receiptId}")
    public String viewReceipt(@PathVariable("receiptId") String receiptId, Model model,
                              RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("receipt", receiptService.getReceiptById(receiptId));
            model.addAttribute("allReceipts", receiptService.getAllReceipts());
            return "payment/receipt";
        } catch (PaymentService.PaymentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/receipts";
        }
    }

    @GetMapping("/receipts")
    public String listReceipts(@RequestParam(value = "search", required = false) String search, Model model) {
        ArrayList<Receipt> results = receiptService.searchReceipts(search);
        model.addAttribute("allReceipts", results);
        if (!results.isEmpty()) {
            model.addAttribute("receipt", results.get(0));
        }
        return "payment/receipt";
    }

    @PostMapping("/receipts/delete/{receiptId}")
    public String deleteReceipt(@PathVariable("receiptId") String receiptId,
                                RedirectAttributes redirectAttributes) {
        try {
            receiptService.deleteReceipt(receiptId);
            redirectAttributes.addFlashAttribute("successMessage", "Receipt " + receiptId + " deleted successfully.");
        } catch (PaymentService.PaymentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/receipts";
    }

    // ---------- Refunds ----------

    @GetMapping("/refunds")
    public String showRefundsPage(@RequestParam(value = "paymentId", required = false) String preselectedPaymentId,
                                  Model model) {
        model.addAttribute("refunds", paymentService.getAllRefunds());
        model.addAttribute("paidPayments", paymentService.filterPayments("PAID", "ALL"));
        model.addAttribute("preselectedPaymentId", preselectedPaymentId);
        return "payment/refund";
    }

    @PostMapping("/refunds/request")
    public String requestRefund(@RequestParam("paymentId") String paymentId,
                                @RequestParam(value = "reason", required = false) String reason,
                                RedirectAttributes redirectAttributes) {
        try {
            Refund refund = paymentService.requestRefund(paymentId, reason);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Refund request '" + refund.getRefundId() + "' submitted successfully for Payment " + paymentId);
        } catch (PaymentService.PaymentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/refunds";
    }

    @PostMapping("/refunds/process")
    public String processRefund(@RequestParam("refundId") String refundId,
                                @RequestParam("approve") boolean approve,
                                RedirectAttributes redirectAttributes) {
        try {
            paymentService.processRefund(refundId, approve);
            if (approve) {
                redirectAttributes.addFlashAttribute("successMessage",
                        "Refund " + refundId + " APPROVED! Payment and receipt updated to REFUNDED.");
            } else {
                redirectAttributes.addFlashAttribute("successMessage", "Refund " + refundId + " was REJECTED.");
            }
        } catch (PaymentService.PaymentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/refunds";
    }

    @PostMapping("/refunds/delete/{refundId}")
    public String deleteRefund(@PathVariable("refundId") String refundId,
                               RedirectAttributes redirectAttributes) {
        try {
            paymentService.deleteRefund(refundId);
            redirectAttributes.addFlashAttribute("successMessage", "Refund " + refundId + " deleted successfully.");
        } catch (PaymentService.PaymentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/refunds";
    }
}
