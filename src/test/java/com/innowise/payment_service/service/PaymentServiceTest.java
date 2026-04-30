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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PaymentMapper paymentMapper;
    @Mock
    private ExternalPaymentClient externalPaymentClient;
    @Mock
    private PaymentEventProducer paymentEventProducer;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void shouldCreatePaymentSuccessfully_WhenExternalApiReturnsTrue() {
        PaymentCreateRequest request = new PaymentCreateRequest();
        request.setOrderId(1L);
        request.setUserId(2L);
        request.setPaymentAmount(BigDecimal.TEN);

        Payment entity = new Payment();
        entity.setOrderId(1L);

        Payment savedEntity = new Payment();
        savedEntity.setOrderId(1L);
        savedEntity.setStatus(PaymentStatus.SUCCESS);

        PaymentResponse expectedResponse = new PaymentResponse();
        expectedResponse.setStatus(PaymentStatus.SUCCESS);

        when(paymentMapper.toEntity(request)).thenReturn(entity);
        when(externalPaymentClient.processPayment()).thenReturn(true);
        when(paymentRepository.save(any(Payment.class))).thenReturn(savedEntity);
        when(paymentMapper.toDto(savedEntity)).thenReturn(expectedResponse);

        PaymentResponse actualResponse = paymentService.createPayment(request);

        assertEquals(PaymentStatus.SUCCESS, actualResponse.getStatus());
        verify(paymentEventProducer, times(1)).sendPaymentEvent(any(PaymentEvent.class));
    }

    @Test
    void shouldCreatePaymentFailed_WhenExternalApiReturnsFalse() {
        PaymentCreateRequest request = new PaymentCreateRequest();
        request.setOrderId(1L);

        Payment entity = new Payment();
        Payment savedEntity = new Payment();
        savedEntity.setStatus(PaymentStatus.FAILED);
        PaymentResponse expectedResponse = new PaymentResponse();
        expectedResponse.setStatus(PaymentStatus.FAILED);

        when(paymentMapper.toEntity(request)).thenReturn(entity);
        when(externalPaymentClient.processPayment()).thenReturn(false);
        when(paymentRepository.save(any(Payment.class))).thenReturn(savedEntity);
        when(paymentMapper.toDto(savedEntity)).thenReturn(expectedResponse);

        PaymentResponse actualResponse = paymentService.createPayment(request);

        assertEquals(PaymentStatus.FAILED, actualResponse.getStatus());
        verify(paymentEventProducer, times(1)).sendPaymentEvent(any(PaymentEvent.class));
    }

    @Test
    void shouldGetUserTotalSum() {
        Long userId = 1L;
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now();
        when(paymentRepository.getTotalSumForUserInDateRange(userId, start, end)).thenReturn(BigDecimal.valueOf(150));

        BigDecimal sum = paymentService.getUserTotalSum(userId, start, end);

        assertEquals(BigDecimal.valueOf(150), sum);
    }
}