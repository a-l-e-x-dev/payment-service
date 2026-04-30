package com.innowise.payment_service.service;

import com.innowise.payment_service.client.ExternalPaymentClient;
import com.innowise.payment_service.dto.PaymentCreateRequest;
import com.innowise.payment_service.dto.PaymentResponse;
import com.innowise.payment_service.entity.Payment;
import com.innowise.payment_service.enums.PaymentStatus;
import com.innowise.payment_service.event.PaymentEvent;
import com.innowise.payment_service.mapper.PaymentMapper;
import com.innowise.payment_service.repository.PaymentRepository;
import kafka.PaymentEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final ExternalPaymentClient externalPaymentClient;
    private final PaymentEventProducer paymentEventProducer;



    @Transactional
    public PaymentResponse createPayment(PaymentCreateRequest request) {
        Payment payment = paymentMapper.toEntity(request);
        payment.setTimestamp(LocalDateTime.now());

        boolean isSuccess = externalPaymentClient.processPayment();
        payment.setStatus(isSuccess ? PaymentStatus.SUCCESS : PaymentStatus.FAILED);

        Payment savedPayment = paymentRepository.save(payment);

        PaymentEvent event = PaymentEvent.builder()
                .orderId(savedPayment.getOrderId())
                .paymentStatus(savedPayment.getStatus().name())
                .message("Payment processed with status: " + savedPayment.getStatus())
                .build();
        paymentEventProducer.sendPaymentEvent(event);

        return paymentMapper.toDto(savedPayment);
    }

    public List<PaymentResponse> getPayments(Long userId, Long orderId, PaymentStatus status) {
        return paymentRepository.findPaymentsByFilters(userId, orderId, status)
                .stream()
                .map(paymentMapper::toDto)
                .collect(Collectors.toList());
    }

    public BigDecimal getUserTotalSum(Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        return paymentRepository.getTotalSumForUserInDateRange(userId, startDate, endDate);
    }

    public BigDecimal getAllUsersTotalSum(LocalDateTime startDate, LocalDateTime endDate) {
        return paymentRepository.getTotalSumForAllUsersInDateRange(startDate, endDate);
    }
}