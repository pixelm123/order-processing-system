package com.orderprocessing.payment.dto;

import com.orderprocessing.payment.domain.Payment;
import com.orderprocessing.payment.domain.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
    UUID id,
    UUID orderId,
    String customerId,
    BigDecimal amount,
    PaymentStatus status,
    String failureReason,
    String correlationId,
    Instant createdAt
) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
            payment.getId(),
            payment.getOrderId(),
            payment.getCustomerId(),
            payment.getAmount(),
            payment.getStatus(),
            payment.getFailureReason(),
            payment.getCorrelationId(),
            payment.getCreatedAt()
        );
    }
}
