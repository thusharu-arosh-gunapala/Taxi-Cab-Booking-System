package com.taxibooking.payment.repository;

import com.taxibooking.payment.model.Payment;
import com.taxibooking.payment.model.Promotion;
import com.taxibooking.payment.model.Receipt;
import com.taxibooking.payment.model.Refund;
import org.springframework.stereotype.Repository;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

// Reads and writes the 4 text files. Every method reads or saves a whole file.
@Repository
public class PaymentRepository {

    private String paymentsFile = "data/payments.txt";
    private String promotionsFile = "data/promotions.txt";
    private String receiptsFile = "data/receipts.txt";
    private String refundsFile = "data/refunds.txt";

    // make the data folder and the files if they are missing
    public PaymentRepository() {
        new File("data").mkdirs();
        String[] files = {paymentsFile, promotionsFile, receiptsFile, refundsFile};
        for (int i = 0; i < files.length; i++) {
            try {
                new File(files[i]).createNewFile();
            } catch (IOException e) {
                throw new RuntimeException("Could not create " + files[i]);
            }
        }
    }

    // ---------- Payments ----------

    public ArrayList<Payment> getAllPayments() {
        ArrayList<Payment> list = new ArrayList<Payment>();
        ArrayList<String> lines = readLines(paymentsFile);
        for (int i = 0; i < lines.size(); i++) {
            try {
                String[] p = split(lines.get(i));
                double amount = Double.parseDouble(p[3]);
                Payment payment;
                if (p[4].equals("CARD")) {
                    payment = new Payment.CardPayment(p[0], p[1], p[2], amount, part(p, 8), part(p, 9), part(p, 10), "");
                } else if (p[4].equals("ONLINE")) {
                    payment = new Payment.OnlinePayment(p[0], p[1], p[2], amount, part(p, 8), part(p, 9));
                } else {
                    Payment.CashPayment cash = new Payment.CashPayment(p[0], p[1], p[2], amount, toDouble(part(p, 8)));
                    cash.setChangeAmount(toDouble(part(p, 9)));
                    payment = cash;
                }
                payment.setStatus(p[5]);
                payment.setPaymentDate(p[6]);
                payment.setNotes(p[7]);
                list.add(payment);
            } catch (Exception e) {
                // skip a broken line
            }
        }
        return list;
    }

    public void saveAllPayments(ArrayList<Payment> list) {
        ArrayList<String> lines = new ArrayList<String>();
        for (int i = 0; i < list.size(); i++) {
            Payment p = list.get(i);
            String line = clean(p.getPaymentId()) + "|" + clean(p.getBookingId()) + "|" + clean(p.getPassengerId())
                    + "|" + p.getAmount() + "|" + p.getPaymentMethod() + "|" + clean(p.getStatus())
                    + "|" + clean(p.getPaymentDate()) + "|" + clean(p.getNotes());
            if (p instanceof Payment.CashPayment) {
                Payment.CashPayment cash = (Payment.CashPayment) p;
                line = line + "|" + cash.getAmountTendered() + "|" + cash.getChangeAmount();
            } else if (p instanceof Payment.CardPayment) {
                Payment.CardPayment card = (Payment.CardPayment) p;
                line = line + "|" + clean(card.getCardNumber()) + "|" + clean(card.getCardHolderName())
                        + "|" + clean(card.getExpiryDate());
            } else if (p instanceof Payment.OnlinePayment) {
                Payment.OnlinePayment online = (Payment.OnlinePayment) p;
                line = line + "|" + clean(online.getProvider()) + "|" + clean(online.getTransactionReference());
            }
            lines.add(line);
        }
        writeLines(paymentsFile, lines);
    }

    // ---------- Promotions ----------

    public ArrayList<Promotion> getAllPromotions() {
        ArrayList<Promotion> list = new ArrayList<Promotion>();
        ArrayList<String> lines = readLines(promotionsFile);
        for (int i = 0; i < lines.size(); i++) {
            try {
                String[] p = split(lines.get(i));
                list.add(new Promotion(p[0].toUpperCase(), Double.parseDouble(p[1]), Double.parseDouble(p[2]),
                        p[3], Boolean.parseBoolean(p[4])));
            } catch (Exception e) {
                // skip a broken line
            }
        }
        return list;
    }

    public void saveAllPromotions(ArrayList<Promotion> list) {
        ArrayList<String> lines = new ArrayList<String>();
        for (int i = 0; i < list.size(); i++) {
            Promotion p = list.get(i);
            lines.add(clean(p.getPromoCode()) + "|" + p.getDiscountPercentage() + "|" + p.getMinimumFare()
                    + "|" + clean(p.getExpiryDate()) + "|" + p.isActive());
        }
        writeLines(promotionsFile, lines);
    }

    // ---------- Receipts ----------

    public ArrayList<Receipt> getAllReceipts() {
        ArrayList<Receipt> list = new ArrayList<Receipt>();
        ArrayList<String> lines = readLines(receiptsFile);
        for (int i = 0; i < lines.size(); i++) {
            try {
                String[] p = split(lines.get(i));
                Receipt r = new Receipt(p[0], p[1], p[2], p[3], p[4], p[5], Double.parseDouble(p[6]),
                        Double.parseDouble(p[7]), Double.parseDouble(p[8]), Double.parseDouble(p[9]), p[10], p[11]);
                r.setIssueDate(part(p, 12));
                list.add(r);
            } catch (Exception e) {
                // skip a broken line
            }
        }
        return list;
    }

    public void saveAllReceipts(ArrayList<Receipt> list) {
        ArrayList<String> lines = new ArrayList<String>();
        for (int i = 0; i < list.size(); i++) {
            Receipt r = list.get(i);
            lines.add(clean(r.getReceiptId()) + "|" + clean(r.getBookingId()) + "|" + clean(r.getPassengerId())
                    + "|" + clean(r.getDriver()) + "|" + clean(r.getPickup()) + "|" + clean(r.getDestination())
                    + "|" + r.getDistance() + "|" + r.getOriginalFare() + "|" + r.getDiscount() + "|" + r.getTotal()
                    + "|" + r.getPaymentMethod() + "|" + clean(r.getStatus()) + "|" + clean(r.getIssueDate()));
        }
        writeLines(receiptsFile, lines);
    }

    // ---------- Refunds ----------

    public ArrayList<Refund> getAllRefunds() {
        ArrayList<Refund> list = new ArrayList<Refund>();
        ArrayList<String> lines = readLines(refundsFile);
        for (int i = 0; i < lines.size(); i++) {
            try {
                String[] p = split(lines.get(i));
                Refund refund = new Refund(p[0], p[1], p[2], Double.parseDouble(p[3]), p[4]);
                refund.setRefundStatus(p[5]);
                refund.setRequestDate(part(p, 6));
                refund.setProcessedDate(part(p, 7));
                list.add(refund);
            } catch (Exception e) {
                // skip a broken line
            }
        }
        return list;
    }

    public void saveAllRefunds(ArrayList<Refund> list) {
        ArrayList<String> lines = new ArrayList<String>();
        for (int i = 0; i < list.size(); i++) {
            Refund r = list.get(i);
            lines.add(clean(r.getRefundId()) + "|" + clean(r.getPaymentId()) + "|" + clean(r.getBookingId())
                    + "|" + r.getRefundAmount() + "|" + clean(r.getReason()) + "|" + clean(r.getRefundStatus())
                    + "|" + clean(r.getRequestDate()) + "|" + clean(r.getProcessedDate()));
        }
        writeLines(refundsFile, lines);
    }

    // ---------- Small helpers ----------

    // read all lines of a file (empty lines and # comments are skipped)
    private ArrayList<String> readLines(String fileName) {
        ArrayList<String> lines = new ArrayList<String>();
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    lines.add(line);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Error reading " + fileName);
        }
        return lines;
    }

    // replace the whole file with the given lines
    private void writeLines(String fileName, ArrayList<String> lines) {
        try (FileWriter writer = new FileWriter(fileName)) {
            for (int i = 0; i < lines.size(); i++) {
                writer.write(lines.get(i) + "\n");
            }
        } catch (IOException e) {
            throw new RuntimeException("Error writing " + fileName);
        }
    }

    // split a line at | and trim every part
    private String[] split(String line) {
        String[] parts = line.split("\\|", -1);
        for (int i = 0; i < parts.length; i++) {
            parts[i] = parts[i].trim();
        }
        return parts;
    }

    // get one part, or "" if the line is shorter
    private String part(String[] parts, int index) {
        if (index < parts.length) {
            return parts[index];
        }
        return "";
    }

    private double toDouble(String text) {
        if (text.isEmpty()) {
            return 0.0;
        }
        return Double.parseDouble(text);
    }

    // remove | and null so the line format is not broken
    private String clean(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("|", " ").trim();
    }
}
