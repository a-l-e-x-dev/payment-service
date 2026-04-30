package com.innowise.payment_service.mapper;

import com.innowise.payment_service.dto.PaymentCreateRequest;
import com.innowise.payment_service.dto.PaymentResponse;
import com.innowise.payment_service.entity.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    PaymentResponse toDto(Payment payment);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "timestamp", ignore = true)
    Payment toEntity(PaymentCreateRequest request);
}