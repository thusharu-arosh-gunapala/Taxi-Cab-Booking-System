package com.taxibooking.payment.service;

import com.taxibooking.payment.model.Payment;
import com.taxibooking.payment.model.Refund;
import com.taxibooking.payment.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

// Payment rules and the refund flow (PAID -> REQUESTED -> REFUNDED)
@Service
public class PaymentService {

    // the only custom exception in this module
    public static class PaymentException extends RuntimeException {
        public PaymentException(String message) {
            super(message);
        }
    }

    private PaymentRepository repository;
    private ReceiptService receiptService;

    @Autowired
    public PaymentService(PaymentRepository repository, ReceiptService receiptService) {
        this.repository = repository;
        this.receiptService = receiptService;
    }

    // ---------- Payments ----------

    // process the payment (polymorphism) and save it, even if it FAILED
    public Payment executePayment(Payment payment) {
        if (hasPaidPaymentForBooking(payment.getBookingId())) {
            throw new PaymentException("A successful payment already exists for booking '" + payment.getBookingId() + "'.");
        }
        payment.processPayment();

        ArrayList<Payment> all = repository.getAllPayments();
        all.add(payment);
        repository.saveAllPayments(all);
        return payment;
    }

    public boolean hasPaidPaymentForBooking(String bookingId) {
        ArrayList<Payment> all = repository.getAllPayments();
        for (int i = 0; i < all.size(); i++) {
            Payment p = all.get(i);
            if (p.getBookingId().equalsIgnoreCase(bookingId) && p.getStatus().equals("PAID")) {
                return true;
            }
        }
        return false;
    }

    public ArrayList<Payment> getAllPayments() {
        return repository.getAllPayments();
    }

    // status or method can be "ALL"
    public ArrayList<Payment> filterPayments(String status, String method) {
        ArrayList<Payment> result = new ArrayList<Payment>();
        ArrayList<Payment> all = repository.getAllPayments();
        for (int i = 0; i < all.size(); i++) {
            Payment p = all.get(i);
            boolean statusOk = status.equalsIgnoreCase("ALL") || p.getStatus().equalsIgnoreCase(status);
            boolean methodOk = method.equalsIgnoreCase("ALL") || p.getPaymentMethod().equalsIgnoreCase(method);
            if (statusOk && methodOk) {
                result.add(p);
            }
        }
        return result;
    }

    public void updatePaymentStatus(String paymentId, String newStatus) {
        newStatus = newStatus.toUpperCase();
        ArrayList<Payment> all = repository.getAllPayments();
        Payment payment = findPayment(all, paymentId);
        if (payment == null) {
            throw new PaymentException("Payment '" + paymentId + "' not found.");
        }

        if (payment.getStatus().equals(newStatus)) {
            return;
        }
        if (payment.getStatus().equals("REFUNDED")) {
            throw new PaymentException("Payment '" + paymentId + "' is already REFUNDED and cannot be changed.");
        }

        // REFUNDED goes through the refund flow
        if (newStatus.equals("REFUNDED")) {
            Refund refund = requestRefund(paymentId, "Status changed to REFUNDED from payment history");
            processRefund(refund.getRefundId(), true);
            return;
        }

        payment.setStatus(newStatus);
        payment.setNotes(payment.getNotes() + " | Status updated to " + newStatus);
        repository.saveAllPayments(all);
    }

    public void deletePayment(String paymentId) {
        ArrayList<Payment> all = repository.getAllPayments();
        Payment payment = findPayment(all, paymentId);
        if (payment == null) {
            throw new PaymentException("Cannot delete: Payment '" + paymentId + "' does not exist.");
        }
        all.remove(payment);
        repository.saveAllPayments(all);
    }

    // ---------- Refunds ----------

    public Refund requestRefund(String paymentId, String reason) {
        Payment payment = findPayment(repository.getAllPayments(), paymentId);
        if (payment == null) {
            throw new PaymentException("Cannot request refund: Payment '" + paymentId + "' not found.");
        }
        if (!payment.getStatus().equals("PAID")) {
            throw new PaymentException("Invalid Refund Request: Only settled 'PAID' payments can be refunded. Current status is " + payment.getStatus());
        }

        ArrayList<Refund> all = repository.getAllRefunds();
        for (int i = 0; i < all.size(); i++) {
            Refund r = all.get(i);
            if (r.getPaymentId().equalsIgnoreCase(paymentId) && r.getRefundStatus().equals("REQUESTED")) {
                throw new PaymentException("A refund request is already pending review for payment '" + paymentId + "'.");
            }
        }

        if (reason == null || reason.trim().isEmpty()) {
            reason = "Customer requested cancellation";
        }
        Refund refund = new Refund(generateRefundId(), payment.getPaymentId(), payment.getBookingId(),
                payment.getAmount(), reason.trim());
        all.add(refund);
        repository.saveAllRefunds(all);
        return refund;
    }

    // approve = true -> REFUNDED (payment and receipt too), false -> REJECTED
    public void processRefund(String refundId, boolean approve) {
        ArrayList<Refund> all = repository.getAllRefunds();
        Refund refund = findRefund(all, refundId);
        if (refund == null) {
            throw new PaymentException("Refund '" + refundId + "' not found.");
        }
        if (!refund.getRefundStatus().equals("REQUESTED")) {
            throw new PaymentException("Refund " + refundId + " is already processed. Current status is " + refund.getRefundStatus());
        }

        String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        refund.setProcessedDate(now);

        if (approve) {
            refund.setRefundStatus("REFUNDED");
            ArrayList<Payment> payments = repository.getAllPayments();
            Payment payment = findPayment(payments, refund.getPaymentId());
            if (payment != null) {
                payment.setStatus("REFUNDED");
                payment.setNotes(payment.getNotes() + " | Refund approved on " + now);
                repository.saveAllPayments(payments);
            }
            receiptService.updateReceiptStatusForBooking(refund.getBookingId(), "REFUNDED");
        } else {
            refund.setRefundStatus("REJECTED");
        }
        repository.saveAllRefunds(all);
    }

    public ArrayList<Refund> getAllRefunds() {
        return repository.getAllRefunds();
    }

    public void deleteRefund(String refundId) {
        ArrayList<Refund> all = repository.getAllRefunds();
        Refund refund = findRefund(all, refundId);
        if (refund == null) {
            throw new PaymentException("Cannot delete: Refund '" + refundId + "' does not exist.");
        }
        all.remove(refund);
        repository.saveAllRefunds(all);
    }

    // ---------- New ids (highest number + 1) ----------

    public String generatePaymentId() {
        int max = 1000;
        ArrayList<Payment> all = repository.getAllPayments();
        for (int i = 0; i < all.size(); i++) {
            int number = getNumber(all.get(i).getPaymentId(), "PAY-");
            if (number > max) {
                max = number;
            }
        }
        return "PAY-" + (max + 1);
    }

    public String generateBookingId() {
        int max = 9000;
        ArrayList<Payment> all = repository.getAllPayments();
        for (int i = 0; i < all.size(); i++) {
            int number = getNumber(all.get(i).getBookingId(), "BK-");
            if (number > max) {
                max = number;
            }
        }
        return "BK-" + (max + 1);
    }

    public String generateRefundId() {
        int max = 9000;
        ArrayList<Refund> all = repository.getAllRefunds();
        for (int i = 0; i < all.size(); i++) {
            int number = getNumber(all.get(i).getRefundId(), "REF-");
            if (number > max) {
                max = number;
            }
        }
        return "REF-" + (max + 1);
    }

    // ---------- Small helpers ----------

    private Payment findPayment(ArrayList<Payment> list, String paymentId) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getPaymentId().equalsIgnoreCase(paymentId)) {
                return list.get(i);
            }
        }
        return null;
    }

    private Refund findRefund(ArrayList<Refund> list, String refundId) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getRefundId().equalsIgnoreCase(refundId)) {
                return list.get(i);
            }
        }
        return null;
    }

    // "PAY-1005" -> 1005, anything else -> 0
    private int getNumber(String id, String prefix) {
        if (!id.startsWith(prefix)) {
            return 0;
        }
        try {
            return Integer.parseInt(id.substring(prefix.length()));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
