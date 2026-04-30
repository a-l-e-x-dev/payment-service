package com.innowise.payment_service.repository;

import com.innowise.payment_service.entity.Payment;
import com.innowise.payment_service.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface PaymentRepositoryCustom {

    List<Payment> findPaymentsByFilters(Long userId, Long orderId, PaymentStatus status);

    BigDecimal getTotalSumForUserInDateRange(Long userId, LocalDateTime startDate, LocalDateTime endDate);

    BigDecimal getTotalSumForAllUsersInDateRange(LocalDateTime startDate, LocalDateTime endDate);
}