package com.innowise.payment_service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentCreateRequest {

    @NotNull(message = "Order ID is mandatory")
    private Long orderId;

    @NotNull(message = "User ID is mandatory")
    private Long userId;

    @NotNull(message = "Payment amount is mandatory")
    @Positive(message = "Payment amount must be greater than zero")
    private BigDecimal paymentAmount;
}