package com.innowise.payment_service.repository;

import com.innowise.payment_service.entity.Payment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends MongoRepository<Payment, String>, PaymentRepositoryCustom {
}