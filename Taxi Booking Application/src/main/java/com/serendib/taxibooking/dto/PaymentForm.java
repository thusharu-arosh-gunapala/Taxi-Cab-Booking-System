package com.serendib.taxibooking.dto;

import com.serendib.taxibooking.entity.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class PaymentForm {

    @NotNull(message = "Select a payment method.")
    private PaymentMethod method;

    @Size(max = 32, message = "Promotion codes can be up to 32 characters.")
    private String promotionCode;

    public PaymentMethod getMethod() {
        return method;
    }

    public void setMethod(PaymentMethod method) {
        this.method = method;
    }

    public String getPromotionCode() {
        return promotionCode;
    }

    public void setPromotionCode(String promotionCode) {
        this.promotionCode = promotionCode;
    }
}
