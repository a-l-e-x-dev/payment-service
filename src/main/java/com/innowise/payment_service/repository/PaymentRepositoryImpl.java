package com.innowise.payment_service.repository;

import com.innowise.payment_service.entity.Payment;
import com.innowise.payment_service.enums.PaymentStatus;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class PaymentRepositoryImpl implements PaymentRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    @Override
    public List<Payment> findPaymentsByFilters(Long userId, Long orderId, PaymentStatus status) {
        Query query = new Query();
        List<Criteria> criteriaList = new ArrayList<>();

        if (userId != null) {
            criteriaList.add(Criteria.where("user_id").is(userId));
        }
        if (orderId != null) {
            criteriaList.add(Criteria.where("order_id").is(orderId));
        }
        if (status != null) {
            criteriaList.add(Criteria.where("status").is(status));
        }

        if (!criteriaList.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        }

        return mongoTemplate.find(query, Payment.class);
    }

    @Override
    public BigDecimal getTotalSumForUserInDateRange(Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        MatchOperation matchStage = Aggregation.match(
                Criteria.where("user_id").is(userId)
                        .and("timestamp").gte(startDate).lte(endDate)
                        .and("status").is(PaymentStatus.SUCCESS)
        );

        return executeSumAggregation(matchStage);
    }

    @Override
    public BigDecimal getTotalSumForAllUsersInDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        MatchOperation matchStage = Aggregation.match(
                Criteria.where("timestamp").gte(startDate).lte(endDate)
                        .and("status").is(PaymentStatus.SUCCESS)
        );

        return executeSumAggregation(matchStage);
    }

    private BigDecimal executeSumAggregation(MatchOperation matchStage) {
        GroupOperation groupStage = Aggregation.group().sum("payment_amount").as("totalSum");

        Aggregation aggregation = Aggregation.newAggregation(matchStage, groupStage);

        AggregationResults<SumResult> results = mongoTemplate.aggregate(aggregation, "payments", SumResult.class);
        SumResult mappedResult = results.getUniqueMappedResult();

        return (mappedResult != null && mappedResult.getTotalSum() != null)
                ? mappedResult.getTotalSum()
                : BigDecimal.ZERO;
    }

    @Data
    private static class SumResult {
        private BigDecimal totalSum;
    }
}