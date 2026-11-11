package com.serendib.taxibooking.controller;

import com.serendib.taxibooking.dto.PaymentForm;
import com.serendib.taxibooking.dto.RefundForm;
import com.serendib.taxibooking.entity.Passenger;
import com.serendib.taxibooking.entity.PaymentMethod;
import com.serendib.taxibooking.entity.PaymentStatus;
import com.serendib.taxibooking.service.PaymentGateway;
import com.serendib.taxibooking.service.PaymentProviderException;
import com.serendib.taxibooking.service.PaymentService;
import com.serendib.taxibooking.service.BookingService;
import com.serendib.taxibooking.service.StripeWebhookVerifier;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PaymentController {

    private final PaymentService paymentService;
    private final BookingService bookingService;
    private final PaymentGateway paymentGateway;
    private final StripeWebhookVerifier webhookVerifier;
    private final String appBaseUrl;

    public PaymentController(PaymentService paymentService,
                             BookingService bookingService,
                             PaymentGateway paymentGateway,
                             StripeWebhookVerifier webhookVerifier,
                             @Value("${app.payment.base-url:http://localhost:8081}") String appBaseUrl) {
        this.paymentService = paymentService;
        this.bookingService = bookingService;
        this.paymentGateway = paymentGateway;
        this.webhookVerifier = webhookVerifier;
        this.appBaseUrl = appBaseUrl.replaceAll("/+$", "");
    }

    @GetMapping("/payments")
    public String payments(@AuthenticationPrincipal Passenger passenger, Model model) {
        model.addAttribute("payments", paymentService.findPaymentsForPassenger(passenger));
        return "payment/list";
    }

    @GetMapping("/bookings/{bookingId}/payment")
    public String checkout(@PathVariable Long bookingId,
                           @AuthenticationPrincipal Passenger passenger,
                           Model model) {
        model.addAttribute("booking", bookingService.findOwnedBooking(bookingId, passenger));
        model.addAttribute("existingPayment", paymentService.findForBooking(bookingId, passenger));
        if (!model.containsAttribute("paymentForm")) {
            model.addAttribute("paymentForm", new PaymentForm());
        }
        model.addAttribute("stripeConfigured", stripeConfigured());
        model.addAttribute("paymentMethods", PaymentMethod.values());
        return "payment/checkout";
    }

    @PostMapping("/bookings/{bookingId}/payment")
    public String startPayment(@PathVariable Long bookingId,
                               @AuthenticationPrincipal Passenger passenger,
                               @Valid @ModelAttribute("paymentForm") PaymentForm form,
                               BindingResult bindingResult,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("booking", bookingService.findOwnedBooking(bookingId, passenger));
            model.addAttribute("existingPayment", paymentService.findForBooking(bookingId, passenger));
            model.addAttribute("paymentMethods", PaymentMethod.values());
            model.addAttribute("stripeConfigured", stripeConfigured());
            return "payment/checkout";
        }

        try {
            if (form.getMethod() == PaymentMethod.STRIPE_CHECKOUT && !stripeConfigured()) {
                throw new PaymentProviderException(
                        "Online payment is unavailable until Stripe checkout and webhook verification are configured.");
            }
            var payment = paymentService.startPayment(passenger, bookingId, form.getMethod(),
                    form.getPromotionCode());
            if (payment.getMethod() == PaymentMethod.CASH) {
                redirectAttributes.addFlashAttribute("notice",
                        "Cash payment is pending driver confirmation. It is not marked paid yet.");
                return "redirect:/payments/" + payment.getPaymentReference();
            }

            try {
                var session = paymentGateway.createCheckoutSession(
                        payment,
                        appBaseUrl + "/payments/checkout/success?session_id={CHECKOUT_SESSION_ID}",
                        appBaseUrl + "/payments/checkout/cancel?paymentReference=" + payment.getPaymentReference());
                paymentService.attachCheckoutSession(payment.getPaymentReference(), session.sessionId());
                return "redirect:" + session.url();
            } catch (PaymentProviderException | IllegalStateException exception) {
                paymentService.markCheckoutFailed(payment.getPaymentReference());
                throw exception;
            }
        } catch (IllegalArgumentException | IllegalStateException | PaymentProviderException exception) {
            model.addAttribute("error", exception.getMessage());
            model.addAttribute("booking", bookingService.findOwnedBooking(bookingId, passenger));
            model.addAttribute("paymentMethods", PaymentMethod.values());
            model.addAttribute("stripeConfigured", stripeConfigured());
            return "payment/checkout";
        }
    }

    @GetMapping("/payments/checkout/success")
    public String checkoutSuccess(@RequestParam("session_id") String sessionId,
                                  @AuthenticationPrincipal Passenger passenger,
                                  RedirectAttributes redirectAttributes) {
        try {
            var session = paymentGateway.retrieveCheckoutSession(sessionId);
            paymentService.confirmStripePayment(session, passenger);
            if (!"paid".equalsIgnoreCase(session.paymentStatus())) {
                redirectAttributes.addFlashAttribute("notice",
                        "Stripe has not confirmed payment yet. Your payment status will update after verification.");
                return "redirect:/payments";
            }
            return "redirect:/payments/" + session.paymentReference();
        } catch (IllegalArgumentException | IllegalStateException | PaymentProviderException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
            return "redirect:/payments";
        }
    }

    @GetMapping("/payments/checkout/cancel")
    public String checkoutCancelled(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("notice",
                "Checkout was cancelled. No payment is considered successful.");
        return "redirect:/payments";
    }

    @GetMapping("/payments/{paymentReference}")
    public String paymentDetails(@PathVariable String paymentReference,
                                 @AuthenticationPrincipal Passenger passenger,
                                 Model model) {
        model.addAttribute("payment", paymentService.findOwnedPayment(paymentReference, passenger));
        model.addAttribute("refundForm", new RefundForm());
        return "payment/detail";
    }

    @GetMapping("/payments/{paymentReference}/continue")
    public String continueCheckout(@PathVariable String paymentReference,
                                   @AuthenticationPrincipal Passenger passenger,
                                   RedirectAttributes redirectAttributes) {
        try {
            var payment = paymentService.findOwnedPayment(paymentReference, passenger);
            if (payment.getMethod() != PaymentMethod.STRIPE_CHECKOUT
                    || payment.getStatus() != PaymentStatus.PENDING
                    || payment.getProviderSessionId() == null) {
                throw new IllegalStateException("This payment has no active Stripe checkout session.");
            }
            var session = paymentGateway.retrieveCheckoutSession(payment.getProviderSessionId());
            if ("paid".equalsIgnoreCase(session.paymentStatus())) {
                paymentService.confirmStripePayment(session, passenger);
                return "redirect:/payments/" + paymentReference;
            }
            if (session.url() == null || session.url().isBlank()) {
                throw new IllegalStateException("This Stripe checkout session is no longer available.");
            }
            return "redirect:" + session.url();
        } catch (IllegalArgumentException | IllegalStateException | PaymentProviderException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
            return "redirect:/payments/" + paymentReference;
        }
    }

    @PostMapping("/payments/{paymentReference}/refund")
    public String requestRefund(@PathVariable String paymentReference,
                                @AuthenticationPrincipal Passenger passenger,
                                @Valid @ModelAttribute("refundForm") RefundForm form,
                                BindingResult bindingResult,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Provide a refund reason of up to 500 characters.");
            return "redirect:/payments/" + paymentReference;
        }
        try {
            paymentService.requestRefund(passenger, paymentReference, form.getReason());
            redirectAttributes.addFlashAttribute("notice",
                    "Your refund request was submitted for review. No refund has been issued yet.");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/payments/" + paymentReference;
    }

    @GetMapping("/refunds")
    public String refunds(@AuthenticationPrincipal Passenger passenger, Model model) {
        model.addAttribute("refunds", paymentService.findRefundsForPassenger(passenger));
        return "payment/refunds";
    }

    @GetMapping("/receipts/{receiptReference}")
    public String receipt(@PathVariable String receiptReference,
                          @AuthenticationPrincipal Passenger passenger,
                          Model model) {
        model.addAttribute("receipt", paymentService.findOwnedReceipt(receiptReference, passenger));
        return "payment/receipt";
    }

    private boolean stripeConfigured() {
        return paymentGateway.isConfigured() && webhookVerifier.isConfigured();
    }
}
