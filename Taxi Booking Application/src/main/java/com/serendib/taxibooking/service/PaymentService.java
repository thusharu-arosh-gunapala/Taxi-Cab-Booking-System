package com.serendib.taxibooking.service;

import com.serendib.taxibooking.entity.Booking;
import com.serendib.taxibooking.entity.BookingStatus;
import com.serendib.taxibooking.entity.Passenger;
import com.serendib.taxibooking.entity.Payment;
import com.serendib.taxibooking.entity.PaymentMethod;
import com.serendib.taxibooking.entity.PaymentStatus;
import com.serendib.taxibooking.entity.Promotion;
import com.serendib.taxibooking.entity.Receipt;
import com.serendib.taxibooking.entity.Refund;
import com.serendib.taxibooking.entity.RefundStatus;
import com.serendib.taxibooking.exception.PaymentNotFoundException;
import com.serendib.taxibooking.repository.BookingRepository;
import com.serendib.taxibooking.repository.PaymentRepository;
import com.serendib.taxibooking.repository.PromotionRepository;
import com.serendib.taxibooking.repository.ReceiptRepository;
import com.serendib.taxibooking.repository.RefundRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class PaymentService {

    private static final List<PaymentStatus> ACTIVE_PAYMENT_STATUSES =
            List.of(PaymentStatus.PENDING, PaymentStatus.SUCCEEDED, PaymentStatus.REFUND_REQUESTED,
                    PaymentStatus.REFUNDED);

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final PromotionRepository promotionRepository;
    private final ReceiptRepository receiptRepository;
    private final RefundRepository refundRepository;
    private final String defaultCurrency;

    public PaymentService(BookingRepository bookingRepository,
                          PaymentRepository paymentRepository,
                          PromotionRepository promotionRepository,
                          ReceiptRepository receiptRepository,
                          RefundRepository refundRepository,
                          @Value("${app.fare.currency:LKR}") String defaultCurrency) {
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.promotionRepository = promotionRepository;
        this.receiptRepository = receiptRepository;
        this.refundRepository = refundRepository;
        this.defaultCurrency = defaultCurrency;
    }

    @Transactional
    public Payment startPayment(Passenger passenger, Long bookingId, PaymentMethod method, String promotionCode) {
        if (method == null) {
            throw new IllegalArgumentException("Select a payment method.");
        }
        Booking booking = bookingRepository.findByIdAndPassengerId(bookingId, passenger.getId())
                .orElseThrow(() -> new IllegalArgumentException("Booking was not found."));
        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new IllegalStateException("Payment is available after the trip is completed.");
        }

        BigDecimal originalAmount = booking.getEstimatedFare().setScale(2, RoundingMode.HALF_UP);
        if (originalAmount.signum() <= 0) {
            throw new IllegalStateException("This booking has no payable fare.");
        }
        Promotion promotion = resolvePromotion(promotionCode, originalAmount);
        BigDecimal discount = promotion == null
                ? BigDecimal.ZERO.setScale(2)
                : promotion.calculateDiscount(originalAmount).setScale(2, RoundingMode.HALF_UP);
        BigDecimal amount = originalAmount.subtract(discount).setScale(2, RoundingMode.HALF_UP);
        if (amount.signum() <= 0) {
            throw new IllegalStateException("Promotion discounts cannot reduce the payable fare to zero.");
        }
        String currency = booking.getFareCurrency() == null || booking.getFareCurrency().isBlank()
                ? defaultCurrency.toUpperCase(Locale.ROOT)
                : booking.getFareCurrency().toUpperCase(Locale.ROOT);

        Payment payment = paymentRepository.findByBookingIdAndPassengerId(bookingId, passenger.getId())
                .orElse(null);
        if (payment != null && ACTIVE_PAYMENT_STATUSES.contains(payment.getStatus())) {
            throw new IllegalStateException("A payment has already been started for this booking.");
        }
        if (promotion != null) {
            promotion.reserveUsage();
        }
        if (payment == null) {
            payment = new Payment(UUID.randomUUID().toString(), booking, passenger, promotion,
                    method, originalAmount, discount, amount, currency);
        } else {
            payment.retry(promotion, method, discount, amount);
        }
        return paymentRepository.save(payment);
    }

    @Transactional
    public void attachCheckoutSession(String paymentReference, String sessionId) {
        Payment payment = findPaymentByReference(paymentReference);
        if (payment.getMethod() != PaymentMethod.STRIPE_CHECKOUT) {
            throw new IllegalStateException("The selected payment method does not use Stripe Checkout.");
        }
        payment.attachProviderSession(sessionId);
    }

    @Transactional
    public void markCheckoutFailed(String paymentReference) {
        Payment payment = findPaymentByReference(paymentReference);
        if (payment.getStatus() == PaymentStatus.PENDING) {
            lockPromotionReservation(payment);
            payment.markFailed();
        }
    }

    @Transactional
    public void confirmStripePayment(PaymentGateway.CheckoutSession session) {
        confirmStripePayment(session, null);
    }

    @Transactional
    public void confirmStripePayment(PaymentGateway.CheckoutSession session, Passenger passenger) {
        Payment payment = findPaymentByReference(session.paymentReference());
        if (passenger != null && !payment.getPassenger().getId().equals(passenger.getId())) {
            throw new IllegalArgumentException("Payment was not found.");
        }
        if (payment.getMethod() != PaymentMethod.STRIPE_CHECKOUT) {
            throw new IllegalStateException("The checkout session does not match an online payment.");
        }
        if (payment.getProviderSessionId() != null
                && !payment.getProviderSessionId().equals(session.sessionId())) {
            throw new IllegalStateException("The checkout session does not match this payment.");
        }
        if (session.amountMinor() == null
                || session.amountMinor() != payment.getAmount().movePointRight(2).longValueExact()
                || !payment.getCurrency().equalsIgnoreCase(session.currency())) {
            throw new IllegalStateException("The verified checkout amount does not match the booking.");
        }
        if (payment.getProviderSessionId() == null) {
            payment.attachProviderSession(session.sessionId());
        }
        if (!"paid".equalsIgnoreCase(session.paymentStatus())) {
            return;
        }
        lockPromotionReservation(payment);
        payment.markSucceeded(session.transactionReference());
        issueReceipt(payment);
    }

    @Transactional
    public void markCashCollected(String paymentReference, String collectionReference) {
        if (collectionReference == null || collectionReference.isBlank()) {
            throw new IllegalArgumentException("A cash collection reference is required.");
        }
        Payment payment = findPaymentByReference(paymentReference);
        lockPromotionReservation(payment);
        payment.markCashCollected(collectionReference.trim());
        issueReceipt(payment);
    }

    @Transactional
    public void failCheckoutSession(String sessionId) {
        paymentRepository.findByProviderSessionId(sessionId).ifPresent(payment -> {
            if (payment.getStatus() == PaymentStatus.PENDING) {
                lockPromotionReservation(payment);
                payment.markFailed();
            }
        });
    }

    @Transactional(readOnly = true)
    public List<Payment> findPaymentsForPassenger(Passenger passenger) {
        return paymentRepository.findAllByPassengerIdOrderByCreatedAtDesc(passenger.getId());
    }

    @Transactional(readOnly = true)
    public Payment findOwnedPayment(String paymentReference, Passenger passenger) {
        return paymentRepository.findByPaymentReferenceAndPassengerId(paymentReference, passenger.getId())
                .orElseThrow(PaymentNotFoundException::new);
    }

    @Transactional(readOnly = true)
    public Payment findForBooking(Long bookingId, Passenger passenger) {
        return paymentRepository.findByBookingIdAndPassengerId(bookingId, passenger.getId()).orElse(null);
    }

    @Transactional(readOnly = true)
    public Receipt findOwnedReceipt(String receiptReference, Passenger passenger) {
        return receiptRepository.findByReceiptReferenceAndPaymentPassengerId(receiptReference, passenger.getId())
                .orElseThrow(PaymentNotFoundException::new);
    }

    @Transactional
    public Refund requestRefund(Passenger passenger, String paymentReference, String reason) {
        if (reason == null || reason.isBlank() || reason.trim().length() > 500) {
            throw new IllegalArgumentException("Provide a refund reason of up to 500 characters.");
        }
        Payment payment = paymentRepository.findByPaymentReferenceAndPassengerId(
                        paymentReference, passenger.getId())
                .orElseThrow(PaymentNotFoundException::new);
        if (refundRepository.existsByPaymentIdAndStatus(payment.getId(), RefundStatus.REQUESTED)) {
            throw new IllegalStateException("A refund request is already open for this payment.");
        }
        payment.requestRefund();
        Refund refund = new Refund(UUID.randomUUID().toString(), payment, passenger, reason.trim());
        return refundRepository.save(refund);
    }

    @Transactional(readOnly = true)
    public List<Refund> findRefundsForPassenger(Passenger passenger) {
        return refundRepository.findAllByPassengerIdOrderByRequestedAtDesc(passenger.getId());
    }

    @Transactional(readOnly = true)
    public PromotionQuote quotePromotion(Passenger passenger, Long bookingId, String code) {
        Booking booking = bookingRepository.findByIdAndPassengerId(bookingId, passenger.getId())
                .orElseThrow(() -> new IllegalArgumentException("Booking was not found."));
        BigDecimal fare = booking.getEstimatedFare().setScale(2, RoundingMode.HALF_UP);
        Promotion promotion = resolvePromotion(code, fare);
        if (promotion == null) {
            throw new IllegalArgumentException("Enter a promotion code.");
        }
        BigDecimal discount = promotion.calculateDiscount(fare).setScale(2, RoundingMode.HALF_UP);
        return new PromotionQuote(promotion.getCode(), discount, fare.subtract(discount), booking.getFareCurrency());
    }

    private Promotion resolvePromotion(String code, BigDecimal fare) {
        if (code == null || code.isBlank()) {
            return null;
        }
        Promotion promotion = promotionRepository.findByCodeForUpdate(code.trim().toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new IllegalArgumentException("This promotion code is not valid."));
        if (!promotion.isAvailable()) {
            throw new IllegalArgumentException("This promotion is expired or unavailable.");
        }
        if (promotion.getExpiresOn() != null && promotion.getExpiresOn().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("This promotion has expired.");
        }
        if (fare.compareTo(promotion.getMinimumFare()) < 0) {
            throw new IllegalArgumentException("The booking fare does not meet this promotion's minimum.");
        }
        return promotion;
    }

    private Payment findPaymentByReference(String paymentReference) {
        return paymentRepository.findByPaymentReference(paymentReference)
                .orElseThrow(() -> new IllegalArgumentException("Payment was not found."));
    }

    private void lockPromotionReservation(Payment payment) {
        Promotion promotion = payment.getPromotion();
        if (promotion != null) {
            promotionRepository.findByCodeForUpdate(promotion.getCode())
                    .orElseThrow(() -> new IllegalStateException("The payment promotion no longer exists."));
        }
    }

    private void issueReceipt(Payment payment) {
        if (receiptRepository.findByPaymentId(payment.getId()).isEmpty()) {
            receiptRepository.save(new Receipt(UUID.randomUUID().toString(), payment));
        }
    }

    public record PromotionQuote(String code, BigDecimal discount, BigDecimal total, String currency) {
    }
}
