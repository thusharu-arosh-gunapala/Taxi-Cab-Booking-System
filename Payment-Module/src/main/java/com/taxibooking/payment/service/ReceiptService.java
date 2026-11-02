package com.taxibooking.payment.service;

import com.taxibooking.payment.model.Payment;
import com.taxibooking.payment.model.Receipt;
import com.taxibooking.payment.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

// Creates, finds and deletes receipts
@Service
public class ReceiptService {

    private PaymentRepository repository;

    @Autowired
    public ReceiptService(PaymentRepository repository) {
        this.repository = repository;
    }

    // make a receipt for a PAID payment
    public Receipt generateReceipt(Payment payment, String driver, String pickup, String destination,
                                   double distance, double originalFare, double discount) {
        if (driver == null || driver.trim().isEmpty()) {
            driver = "Assigned Driver";
        }
        if (pickup == null || pickup.trim().isEmpty()) {
            pickup = "Pickup Point";
        }
        if (destination == null || destination.trim().isEmpty()) {
            destination = "Destination Point";
        }
        if (originalFare <= 0) {
            originalFare = payment.getAmount() + discount;
        }

        Receipt receipt = new Receipt(generateReceiptId(), payment.getBookingId(), payment.getPassengerId(),
                driver, pickup, destination, distance, originalFare, discount,
                payment.getAmount(), payment.getPaymentMethod(), payment.getStatus());

        ArrayList<Receipt> all = repository.getAllReceipts();
        all.add(receipt);
        repository.saveAllReceipts(all);
        return receipt;
    }

    public ArrayList<Receipt> getAllReceipts() {
        return repository.getAllReceipts();
    }

    // search by receipt id, booking id or passenger id (empty search = all)
    public ArrayList<Receipt> searchReceipts(String search) {
        ArrayList<Receipt> all = repository.getAllReceipts();
        if (search == null || search.trim().isEmpty()) {
            return all;
        }
        String text = search.trim().toLowerCase();
        ArrayList<Receipt> result = new ArrayList<Receipt>();
        for (int i = 0; i < all.size(); i++) {
            Receipt r = all.get(i);
            if (r.getReceiptId().toLowerCase().contains(text)
                    || r.getBookingId().toLowerCase().contains(text)
                    || r.getPassengerId().toLowerCase().contains(text)) {
                result.add(r);
            }
        }
        return result;
    }

    public Receipt getReceiptById(String receiptId) {
        Receipt receipt = findReceipt(repository.getAllReceipts(), receiptId);
        if (receipt == null) {
            throw new PaymentService.PaymentException("Receipt '" + receiptId + "' not found.");
        }
        return receipt;
    }

    // used by the refund flow: the PAID receipt of this booking becomes REFUNDED
    public void updateReceiptStatusForBooking(String bookingId, String newStatus) {
        ArrayList<Receipt> all = repository.getAllReceipts();
        for (int i = 0; i < all.size(); i++) {
            Receipt r = all.get(i);
            if (r.getBookingId().equalsIgnoreCase(bookingId) && r.getStatus().equals("PAID")) {
                r.setStatus(newStatus);
                repository.saveAllReceipts(all);
                return;
            }
        }
    }

    public void deleteReceipt(String receiptId) {
        ArrayList<Receipt> all = repository.getAllReceipts();
        Receipt receipt = findReceipt(all, receiptId);
        if (receipt == null) {
            throw new PaymentService.PaymentException("Cannot delete: Receipt '" + receiptId + "' does not exist.");
        }
        all.remove(receipt);
        repository.saveAllReceipts(all);
    }

    // highest RCP number + 1
    private String generateReceiptId() {
        int max = 8000;
        ArrayList<Receipt> all = repository.getAllReceipts();
        for (int i = 0; i < all.size(); i++) {
            String id = all.get(i).getReceiptId();
            if (id.startsWith("RCP-")) {
                try {
                    int number = Integer.parseInt(id.substring(4));
                    if (number > max) {
                        max = number;
                    }
                } catch (NumberFormatException e) {
                    // not a number, skip it
                }
            }
        }
        return "RCP-" + (max + 1);
    }

    private Receipt findReceipt(ArrayList<Receipt> list, String receiptId) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getReceiptId().equalsIgnoreCase(receiptId)) {
                return list.get(i);
            }
        }
        return null;
    }
}
